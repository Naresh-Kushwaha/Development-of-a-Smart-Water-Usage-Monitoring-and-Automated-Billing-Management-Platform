package com.smartwater.web;

import com.smartwater.payment.RazorpayGateway;
import com.smartwater.security.Current;
import com.smartwater.service.PaymentService;
import com.smartwater.web.Dtos.InvoiceRes;
import com.smartwater.web.PaymentDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService service;
    private final RazorpayGateway gateway;

    /** Lets the UI show "Pay now" only when Razorpay is configured. The key id is public by design. */
    @GetMapping("/config")
    public PaymentConfigRes config() {
        boolean on = gateway.configured();
        return new PaymentConfigRes(on, on ? gateway.keyId() : null);
    }

    @PostMapping("/orders")
    public OrderRes createOrder(@Valid @RequestBody CreateOrderReq r) {
        return service.createOrder(Current.user(), r.invoiceId());
    }

    @PostMapping("/verify")
    public InvoiceRes verify(@Valid @RequestBody VerifyPaymentReq r) {
        return Mapper.invoice(service.verify(Current.user(), r.orderId(), r.paymentId(), r.signature()));
    }

    /**
     * Called by Razorpay's servers (public URL, no login). Authenticity comes from the signature header,
     * which is computed over the RAW body - so the body is taken as a plain String, not parsed first.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String payload,
                                        @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        service.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}
