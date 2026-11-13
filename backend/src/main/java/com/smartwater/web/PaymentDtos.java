package com.smartwater.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class PaymentDtos {
    private PaymentDtos() {}

    public record PaymentConfigRes(boolean enabled, String keyId) {}
    public record CreateOrderReq(@NotNull Long invoiceId) {}
    public record OrderRes(String orderId, long amountPaise, String currency, String keyId, String description) {}
    public record VerifyPaymentReq(@NotBlank String orderId, @NotBlank String paymentId, @NotBlank String signature) {}
}
