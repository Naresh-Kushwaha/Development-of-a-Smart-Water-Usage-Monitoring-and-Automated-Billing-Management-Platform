package com.smartwater;

import com.smartwater.payment.RazorpaySignature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RazorpaySignatureTest {
    // HMAC-SHA256("order_123|pay_456", "secret") computed independently with Python's hmac module
    private static final String PAYMENT_SIG = "18bfc0baafae8f6367711ee362f2201aaa3654274683100e5367bb9a2bd29cbe";
    // HMAC-SHA256("{\"event\":\"payment.captured\"}", "whsec")
    private static final String WEBHOOK_BODY = "{\"event\":\"payment.captured\"}";
    private static final String WEBHOOK_SIG = "4673dd707ef4c41b987cb7fefe1583142dc702388c93145b7814b9ad3d3c183e";

    @Test
    void hmacMatchesTheKnownReferenceValues() {
        assertEquals(PAYMENT_SIG, RazorpaySignature.hmacSha256Hex("order_123|pay_456", "secret"));
        assertEquals(WEBHOOK_SIG, RazorpaySignature.hmacSha256Hex(WEBHOOK_BODY, "whsec"));
    }

    @Test
    void checkoutSignature_acceptsTheGenuineOneOnly() {
        assertTrue(RazorpaySignature.verifyPayment("order_123", "pay_456", PAYMENT_SIG, "secret"));
        assertTrue(RazorpaySignature.verifyPayment("order_123", "pay_456", PAYMENT_SIG.toUpperCase(), "secret"));

        assertFalse(RazorpaySignature.verifyPayment("order_123", "pay_999", PAYMENT_SIG, "secret"));    // another payment id
        assertFalse(RazorpaySignature.verifyPayment("order_999", "pay_456", PAYMENT_SIG, "secret"));    // another order id
        assertFalse(RazorpaySignature.verifyPayment("order_123", "pay_456", PAYMENT_SIG, "other"));     // wrong secret
        assertFalse(RazorpaySignature.verifyPayment("order_123", "pay_456", "deadbeef", "secret"));
    }

    @Test
    void missingValuesNeverVerify() {
        assertFalse(RazorpaySignature.verifyPayment("order_123", "pay_456", null, "secret"));
        assertFalse(RazorpaySignature.verifyPayment("order_123", "pay_456", " ", "secret"));
        assertFalse(RazorpaySignature.verifyPayment("order_123", "pay_456", PAYMENT_SIG, ""));          // secret not configured
        assertFalse(RazorpaySignature.verifyWebhook(WEBHOOK_BODY, WEBHOOK_SIG, null));
    }

    @Test
    void webhookSignature_isBoundToTheExactBody() {
        assertTrue(RazorpaySignature.verifyWebhook(WEBHOOK_BODY, WEBHOOK_SIG, "whsec"));
        assertFalse(RazorpaySignature.verifyWebhook(WEBHOOK_BODY + " ", WEBHOOK_SIG, "whsec"));          // tampered body
        assertFalse(RazorpaySignature.verifyWebhook(WEBHOOK_BODY, WEBHOOK_SIG, "wrong"));
    }
}
