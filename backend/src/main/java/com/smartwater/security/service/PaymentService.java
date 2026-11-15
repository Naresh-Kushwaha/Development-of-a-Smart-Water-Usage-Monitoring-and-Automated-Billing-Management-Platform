package com.smartwater.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartwater.domain.Enums.InvoiceStatus;
import com.smartwater.domain.Invoice;
import com.smartwater.domain.Payment;
import com.smartwater.domain.User;
import com.smartwater.events.DomainEvents;
import com.smartwater.payment.RazorpayGateway;
import com.smartwater.payment.RazorpaySignature;
import com.smartwater.repo.InvoiceRepository;
import com.smartwater.repo.PaymentRepository;
import com.smartwater.repo.UserRepository;
import com.smartwater.web.ApiException;
import com.smartwater.web.PaymentDtos.OrderRes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Online payment of an invoice with Razorpay.
 *   1. createOrder : server decides the amount from the invoice and creates the Razorpay order
 *   2. (browser)   : customer pays in Razorpay Checkout
 *   3. verify      : browser sends order/payment ids + signature; we check the signature and mark the invoice PAID
 *   4. webhook     : Razorpay calls us as well, so a payment is recorded even if the browser was closed
 * Steps 3 and 4 are idempotent: whichever arrives first marks it paid, the other changes nothing.
 */
@Service
@Slf4j
public class PaymentService {
    private final PaymentRepository payments;
    private final InvoiceRepository invoices;
    private final UserRepository users;
    private final RazorpayGateway gateway;
    private final DomainEvents events;
    private final ObjectMapper mapper;
    private final String keySecret;
    private final String webhookSecret;

    public PaymentService(PaymentRepository payments, InvoiceRepository invoices, UserRepository users,
                          RazorpayGateway gateway, DomainEvents events, ObjectMapper mapper,
                          @Value("${app.razorpay.key-secret:}") String keySecret,
                          @Value("${app.razorpay.webhook-secret:}") String webhookSecret) {
        this.payments = payments;
        this.invoices = invoices;
        this.users = users;
        this.gateway = gateway;
        this.events = events;
        this.mapper = mapper;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
    }

    // ------------------------------------------------------------------ 1. create the order
    @Transactional
    public OrderRes createOrder(User user, Long invoiceId) {
        if (!gateway.configured()) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Online payments are not configured");
        Invoice inv = ownInvoice(user, invoiceId);
        if (inv.getStatus() == InvoiceStatus.PAID) throw ApiException.conflict("This invoice is already paid");

        long paise = toPaise(inv.getTotal());               // the amount comes from the database, never from the browser
        if (paise < 100) throw ApiException.bad("Amounts below INR 1.00 cannot be paid online");
        String unit = inv.getHousehold().getUnitNumber();

        // reuse the still-open order for the same amount (customer closed the popup and clicked Pay again)
        Optional<Payment> open = payments.findFirstByInvoiceIdAndStatusOrderByCreatedAtDesc(inv.getId(), Payment.Status.CREATED)
                .filter(p -> toPaise(p.getAmount()) == paise);
        String orderId;
        if (open.isPresent()) {
            orderId = open.get().getRazorpayOrderId();
        } else {
            RazorpayGateway.Order order = gateway.createOrder(paise, "INR", "inv_" + inv.getId(),
                    Map.of("invoiceId", String.valueOf(inv.getId()), "unit", unit));
            Payment p = new Payment();
            p.setInvoice(inv);
            p.setRazorpayOrderId(order.id());
            p.setAmount(inv.getTotal());
            payments.save(p);
            orderId = order.id();
        }
        return new OrderRes(orderId, paise, "INR", gateway.keyId(),
                "Water bill " + inv.getBillingCycle().getLabel() + " - unit " + unit);
    }

    // ------------------------------------------------------------------ 3. browser confirms the payment
    @Transactional
    @CacheEvict(cacheNames = {"dashboard", "compare"}, allEntries = true)
    public Invoice verify(User user, String orderId, String paymentId, String signature) {
        Payment p = payments.findByRazorpayOrderId(orderId).orElseThrow(() -> ApiException.notFound("Payment"));
        ownInvoice(user, p.getInvoice().getId());
        if (!RazorpaySignature.verifyPayment(orderId, paymentId, signature, keySecret))
            throw ApiException.bad("Payment signature could not be verified");
        return markPaid(p, paymentId);
    }

    // ------------------------------------------------------------------ 4. Razorpay webhook
    @Transactional
    @CacheEvict(cacheNames = {"dashboard", "compare"}, allEntries = true)
    public void handleWebhook(String rawBody, String signature) {
        if (!RazorpaySignature.verifyWebhook(rawBody, signature, webhookSecret))
            throw ApiException.bad("Invalid webhook signature");

        JsonNode root;
        try { root = mapper.readTree(rawBody); }
        catch (JsonProcessingException e) { throw ApiException.bad("Malformed webhook payload"); }

        String event = root.path("event").asText("");
        JsonNode entity = root.path("payload").path("payment").path("entity");
        String orderId = entity.path("order_id").asText("");
        String paymentId = entity.path("id").asText("");
        if (orderId.isEmpty()) return;                                  // an event we do not use

        Optional<Payment> found = payments.findByRazorpayOrderId(orderId);
        if (found.isEmpty()) return;                                    // not one of our orders
        Payment p = found.get();

        switch (event) {
            case "payment.captured", "order.paid" -> {
                long paidPaise = entity.path("amount").asLong(-1);
                if (paidPaise != toPaise(p.getAmount())) {
                    log.warn("Webhook amount {} does not match order {} ({} expected) - ignored", paidPaise, orderId, toPaise(p.getAmount()));
                    return;
                }
                markPaid(p, paymentId);
            }
            case "payment.failed" -> {
                if (p.getStatus() == Payment.Status.CREATED) {          // the customer may still retry on the same order
                    p.setStatus(Payment.Status.FAILED);
                    payments.save(p);
                }
            }
            default -> { /* other events are ignored */ }
        }
    }

    // ------------------------------------------------------------------ helpers
    private Invoice markPaid(Payment p, String paymentId) {
        Invoice inv = p.getInvoice();
        if (p.getStatus() != Payment.Status.PAID) {
            p.setStatus(Payment.Status.PAID);
            p.setRazorpayPaymentId(paymentId);
            p.setPaidAt(Instant.now());
            payments.save(p);
        }
        if (inv.getStatus() != InvoiceStatus.PAID) {
            inv.setStatus(InvoiceStatus.PAID);
            invoices.save(inv);
            sendReceipt(inv, paymentId);
        }
        return inv;
    }

    private void sendReceipt(Invoice inv, String paymentId) {
        String body = String.format(Locale.US,
                "We received your payment of INR %,.2f for the water bill %s (unit %s).%nPayment reference: %s%n%nThank you!",
                inv.getTotal(), inv.getBillingCycle().getLabel(), inv.getHousehold().getUnitNumber(), paymentId);
        for (User u : users.findByHouseholdId(inv.getHousehold().getId()))
            events.email(u.getEmail(), "Payment received - " + inv.getBillingCycle().getLabel(), body);
    }

    private Invoice ownInvoice(User u, Long invoiceId) {
        Invoice inv = invoices.findById(invoiceId).orElseThrow(() -> ApiException.notFound("Invoice"));
        if (u.getHousehold() == null || !u.getHousehold().getId().equals(inv.getHousehold().getId()))
            throw ApiException.notFound("Invoice");            // 404, not 403: do not reveal other people's invoices
        return inv;
    }

    /** Razorpay amounts are integers in the smallest unit: INR 320.00 -> 32000 paise. */
    static long toPaise(BigDecimal rupees) {
        return rupees.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
