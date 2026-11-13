package com.smartwater.payment;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Razorpay signs everything with HMAC-SHA256 (hex):
 *  - checkout success:  HMAC(order_id + "|" + payment_id, KEY SECRET)  sent as razorpay_signature
 *  - webhooks:          HMAC(raw request body, WEBHOOK SECRET)         sent in the X-Razorpay-Signature header
 * A request whose signature does not match was not produced by Razorpay and must be ignored.
 */
public final class RazorpaySignature {
    private RazorpaySignature() {}

    public static boolean verifyPayment(String orderId, String paymentId, String signature, String keySecret) {
        return matches(orderId + "|" + paymentId, signature, keySecret);
    }

    public static boolean verifyWebhook(String rawBody, String signature, String webhookSecret) {
        return matches(rawBody, signature, webhookSecret);
    }

    public static String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is not available", e);
        }
    }

    private static boolean matches(String data, String signature, String secret) {
        if (data == null || signature == null || signature.isBlank() || secret == null || secret.isBlank()) return false;
        String expected = hmacSha256Hex(data, secret);
        // constant-time comparison: do not leak how many characters matched
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                signature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
    }
}
