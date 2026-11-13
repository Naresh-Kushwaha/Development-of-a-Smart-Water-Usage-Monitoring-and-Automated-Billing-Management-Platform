package com.smartwater.payment;

import java.util.Map;

/** The only place that talks to Razorpay's API (an interface, so services can be unit tested with a fake). */
public interface RazorpayGateway {
    record Order(String id, long amountPaise, String currency) {}

    /** true when RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET are set. */
    boolean configured();

    /** The PUBLIC key id (safe to send to the browser). */
    String keyId();

    Order createOrder(long amountPaise, String currency, String receipt, Map<String, String> notes);
}
