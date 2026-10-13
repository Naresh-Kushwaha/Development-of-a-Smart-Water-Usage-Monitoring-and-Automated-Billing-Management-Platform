package com.smartwater.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "tariff_plans") @Getter @Setter @NoArgsConstructor
public class TariffPlan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(optional = false) @JoinColumn(name = "apartment_id") private Apartment apartment;
    @Column(name = "tier1_limit_kl", nullable = false) private BigDecimal tier1LimitKl = BigDecimal.TEN;
    @Column(name = "tier1_rate", nullable = false) private BigDecimal tier1Rate;
    @Column(name = "tier2_rate", nullable = false) private BigDecimal tier2Rate;
    @Column(name = "fixed_charge", nullable = false) private BigDecimal fixedCharge = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING) @Column(name = "shared_method", nullable = false) private Enums.SharedMethod sharedMethod = Enums.SharedMethod.AREA;
    @Enumerated(EnumType.STRING) @Column(name = "billing_mode", nullable = false) private Enums.BillingMode billingMode = Enums.BillingMode.TIERED;
    @Column(name = "alert_threshold_kl", nullable = false) private BigDecimal alertThresholdKl = new BigDecimal("15");
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();
}
