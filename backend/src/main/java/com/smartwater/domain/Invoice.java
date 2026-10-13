package com.smartwater.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "invoices") @Getter @Setter @NoArgsConstructor
public class Invoice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "billing_cycle_id") private BillingCycle billingCycle;
    @ManyToOne(optional = false) @JoinColumn(name = "household_id") private Household household;
    @Column(name = "consumption_kl", nullable = false) private BigDecimal consumptionKl;
    @Column(name = "base_charge", nullable = false) private BigDecimal baseCharge;
    @Column(name = "shared_charge", nullable = false) private BigDecimal sharedCharge;
    @Column(nullable = false) private BigDecimal adjustment = BigDecimal.ZERO;
    @Column(name = "adjustment_note") private String adjustmentNote;
    @Column(nullable = false) private BigDecimal total;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Enums.InvoiceStatus status = Enums.InvoiceStatus.UNPAID;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
}
