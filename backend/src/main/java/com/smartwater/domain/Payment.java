package com.smartwater.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

/** A Razorpay order for one invoice. CREATED -> PAID (or FAILED, which can still turn PAID if the customer retries). */
@Entity @Table(name = "payments") @Getter @Setter @NoArgsConstructor
public class Payment {
    public enum Status { CREATED, PAID, FAILED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "invoice_id") private Invoice invoice;
    @Column(name = "razorpay_order_id", nullable = false, unique = true) private String razorpayOrderId;
    @Column(name = "razorpay_payment_id") private String razorpayPaymentId;
    @Column(nullable = false) private BigDecimal amount;
    @Column(nullable = false) private String currency = "INR";
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status = Status.CREATED;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    @Column(name = "paid_at") private Instant paidAt;
}