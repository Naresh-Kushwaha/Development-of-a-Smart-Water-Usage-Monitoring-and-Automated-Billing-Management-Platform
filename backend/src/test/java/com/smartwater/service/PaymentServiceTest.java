package com.smartwater.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartwater.domain.*;
import com.smartwater.domain.Enums.CycleStatus;
import com.smartwater.domain.Enums.InvoiceStatus;
import com.smartwater.domain.Enums.Role;
import com.smartwater.events.DomainEvents;
import com.smartwater.payment.RazorpayGateway;
import com.smartwater.payment.RazorpaySignature;
import com.smartwater.repo.InvoiceRepository;
import com.smartwater.repo.PaymentRepository;
import com.smartwater.repo.UserRepository;
import com.smartwater.web.ApiException;
import com.smartwater.web.PaymentDtos.OrderRes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.smartwater.TestData.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    private static final String KEY_SECRET = "keysecret";
    private static final String WEBHOOK_SECRET = "whsecret";

    @Mock PaymentRepository payments;
    @Mock InvoiceRepository invoices;
    @Mock UserRepository users;
    @Mock RazorpayGateway gateway;
    @Mock DomainEvents events;

    private PaymentService service;

    private final Apartment apt = apartment(1L);
    private final Household home = household(1L, apt, "A-1", "900", 3, "M1");
    private final Household neighbour = household(2L, apt, "A-2", "800", 2, "M2");
    private final BillingCycle cycle = cycle(1L, apt, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), "0", CycleStatus.FINALIZED);
    private final User resident = user(10L, "res@x.com", Role.RESIDENT, apt, home);
    private final Invoice inv = invoice(7L, cycle, home, "270.00", "50.00");        // total 320.00 = 32000 paise

    @BeforeEach
    void setUp() {
        service = new PaymentService(payments, invoices, users, gateway, events, new ObjectMapper(), KEY_SECRET, WEBHOOK_SECRET);
    }

    private Payment openPayment(String orderId) {
        Payment p = new Payment();
        p.setInvoice(inv);
        p.setRazorpayOrderId(orderId);
        p.setAmount(inv.getTotal());
        return p;
    }

    private static String capturedBody(String orderId, String paymentId, long paise) {
        return "{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"" + paymentId
                + "\",\"order_id\":\"" + orderId + "\",\"amount\":" + paise + ",\"status\":\"captured\"}}}}";
    }

    private static String sign(String body) { return RazorpaySignature.hmacSha256Hex(body, WEBHOOK_SECRET); }

    // ================= create order =================
    @Test
    void createOrder_failsWhenRazorpayIsNotConfigured() {
        when(gateway.configured()).thenReturn(false);
        var ex = assertThrows(ApiException.class, () -> service.createOrder(resident, 7L));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
    }

    @Test
    void createOrder_neighboursInvoiceLooksLikeItDoesNotExist() {
        when(gateway.configured()).thenReturn(true);
        when(invoices.findById(8L)).thenReturn(Optional.of(invoice(8L, cycle, neighbour, "100.00", "0.00")));

        var ex = assertThrows(ApiException.class, () -> service.createOrder(resident, 8L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(gateway, never()).createOrder(anyLong(), anyString(), anyString(), anyMap());
    }

    @Test
    void createOrder_paidInvoiceCannotBePaidTwice() {
        inv.setStatus(InvoiceStatus.PAID);
        when(gateway.configured()).thenReturn(true);
        when(invoices.findById(7L)).thenReturn(Optional.of(inv));

        var ex = assertThrows(ApiException.class, () -> service.createOrder(resident, 7L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void createOrder_rejectsAmountsBelowOneRupee() {
        inv.setTotal(bd("0.50"));
        when(gateway.configured()).thenReturn(true);
        when(invoices.findById(7L)).thenReturn(Optional.of(inv));

        var ex = assertThrows(ApiException.class, () -> service.createOrder(resident, 7L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void createOrder_usesTheServerSideAmountInPaiseAndStoresThePayment() {
        when(gateway.configured()).thenReturn(true);
        when(gateway.keyId()).thenReturn("rzp_test_abc");
        when(invoices.findById(7L)).thenReturn(Optional.of(inv));
        when(gateway.createOrder(32000L, "INR", "inv_7", Map.of("invoiceId", "7", "unit", "A-1")))
                .thenReturn(new RazorpayGateway.Order("order_1", 32000L, "INR"));

        OrderRes res = service.createOrder(resident, 7L);

        assertEquals("order_1", res.orderId());
        assertEquals(32000L, res.amountPaise());
        assertEquals("INR", res.currency());
        assertEquals("rzp_test_abc", res.keyId());
        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(payments).save(saved.capture());
        assertEquals("order_1", saved.getValue().getRazorpayOrderId());
        assertEquals(Payment.Status.CREATED, saved.getValue().getStatus());
        assertEquals(0, bd("320.00").compareTo(saved.getValue().getAmount()));
    }

    @Test
    void createOrder_reusesTheStillOpenOrderForTheSameAmount() {
        when(gateway.configured()).thenReturn(true);
        when(gateway.keyId()).thenReturn("rzp_test_abc");
        when(invoices.findById(7L)).thenReturn(Optional.of(inv));
        when(payments.findFirstByInvoiceIdAndStatusOrderByCreatedAtDesc(7L, Payment.Status.CREATED))
                .thenReturn(Optional.of(openPayment("order_old")));

        OrderRes res = service.createOrder(resident, 7L);

        assertEquals("order_old", res.orderId());
        verify(gateway, never()).createOrder(anyLong(), anyString(), anyString(), anyMap());
    }

    // ================= verify (browser confirms) =================
    @Test
    void verify_rejectsAForgedSignature_andLeavesTheInvoiceUnpaid() {
        when(payments.findByRazorpayOrderId("order_1")).thenReturn(Optional.of(openPayment("order_1")));
        when(invoices.findById(7L)).thenReturn(Optional.of(inv));

        var ex = assertThrows(ApiException.class, () -> service.verify(resident, "order_1", "pay_1", "deadbeef"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(InvoiceStatus.UNPAID, inv.getStatus());
        verify(events, never()).email(anyString(), anyString(), anyString());
    }

    @Test
    void verify_withAGenuineSignature_marksPaidAndEmailsAReceipt() {
        Payment p = openPayment("order_1");
        when(payments.findByRazorpayOrderId("order_1")).thenReturn(Optional.of(p));
        when(invoices.findById(7L)).thenReturn(Optional.of(inv));
        when(users.findByHouseholdId(1L)).thenReturn(List.of(resident));
        String signature = RazorpaySignature.hmacSha256Hex("order_1|pay_1", KEY_SECRET);

        Invoice paid = service.verify(resident, "order_1", "pay_1", signature);

        assertEquals(InvoiceStatus.PAID, paid.getStatus());
        assertEquals(Payment.Status.PAID, p.getStatus());
        assertEquals("pay_1", p.getRazorpayPaymentId());
        assertNotNull(p.getPaidAt());
        verify(invoices).save(inv);
        verify(events).email(eq("res@x.com"), contains("Payment received"), contains("INR 320.00"));
    }

    @Test
    void verify_anotherHouseholdsPaymentIsNotFound() {
        User stranger = user(11L, "other@x.com", Role.RESIDENT, apt, neighbour);
        when(payments.findByRazorpayOrderId("order_1")).thenReturn(Optional.of(openPayment("order_1")));
        when(invoices.findById(7L)).thenReturn(Optional.of(inv));

        var ex = assertThrows(ApiException.class,
                () -> service.verify(stranger, "order_1", "pay_1", RazorpaySignature.hmacSha256Hex("order_1|pay_1", KEY_SECRET)));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals(InvoiceStatus.UNPAID, inv.getStatus());
    }

    // ================= webhook =================
    @Test
    void webhook_withABadSignatureIsRejected() {
        var ex = assertThrows(ApiException.class, () -> service.handleWebhook(capturedBody("order_1", "pay_9", 32000), "nope"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verifyNoInteractions(payments);
    }

    @Test
    void webhook_captured_marksTheInvoicePaid_evenIfTheBrowserNeverReturned() {
        Payment p = openPayment("order_1");
        when(payments.findByRazorpayOrderId("order_1")).thenReturn(Optional.of(p));
        when(users.findByHouseholdId(1L)).thenReturn(List.of(resident));
        String body = capturedBody("order_1", "pay_9", 32000);

        service.handleWebhook(body, sign(body));

        assertEquals(InvoiceStatus.PAID, inv.getStatus());
        assertEquals("pay_9", p.getRazorpayPaymentId());
    }

    @Test
    void webhook_ignoresAPaymentWhoseAmountDoesNotMatchTheOrder() {
        when(payments.findByRazorpayOrderId("order_1")).thenReturn(Optional.of(openPayment("order_1")));
        String body = capturedBody("order_1", "pay_9", 100);

        service.handleWebhook(body, sign(body));

        assertEquals(InvoiceStatus.UNPAID, inv.getStatus());
        verify(events, never()).email(anyString(), anyString(), anyString());
    }

    @Test
    void webhook_ignoresOrdersThatAreNotOurs() {
        String body = capturedBody("order_unknown", "pay_9", 32000);

        assertDoesNotThrow(() -> service.handleWebhook(body, sign(body)));

        verify(invoices, never()).save(any());
    }

    @Test
    void webhook_deliveredTwice_sendsOnlyOneReceipt() {
        Payment p = openPayment("order_1");
        when(payments.findByRazorpayOrderId("order_1")).thenReturn(Optional.of(p));
        when(users.findByHouseholdId(1L)).thenReturn(List.of(resident));
        String body = capturedBody("order_1", "pay_9", 32000);

        service.handleWebhook(body, sign(body));
        service.handleWebhook(body, sign(body));

        verify(events, times(1)).email(eq("res@x.com"), anyString(), anyString());
    }

    @Test
    void webhook_failedPayment_canStillBecomePaidAfterARetry() {
        Payment p = openPayment("order_1");
        when(payments.findByRazorpayOrderId("order_1")).thenReturn(Optional.of(p));
        when(users.findByHouseholdId(1L)).thenReturn(List.of(resident));
        String failed = "{\"event\":\"payment.failed\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_8\",\"order_id\":\"order_1\",\"amount\":32000}}}}";

        service.handleWebhook(failed, sign(failed));
        assertEquals(Payment.Status.FAILED, p.getStatus());
        assertEquals(InvoiceStatus.UNPAID, inv.getStatus());

        String captured = capturedBody("order_1", "pay_9", 32000);
        service.handleWebhook(captured, sign(captured));
        assertEquals(Payment.Status.PAID, p.getStatus());
        assertEquals(InvoiceStatus.PAID, inv.getStatus());
    }

    // ================= amounts =================
    @Test
    void toPaise_convertsRupeesToWholePaise() {
        assertEquals(32000L, PaymentService.toPaise(bd("320.00")));
        assertEquals(100L, PaymentService.toPaise(bd("1")));
        assertEquals(1001L, PaymentService.toPaise(bd("10.005")));          // half-up
    }
}
