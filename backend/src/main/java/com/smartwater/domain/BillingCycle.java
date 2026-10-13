package com.smartwater.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity @Table(name = "billing_cycles") @Getter @Setter @NoArgsConstructor
public class BillingCycle {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "apartment_id") private Apartment apartment;
    @Column(nullable = false) private String label;
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "end_date", nullable = false) private LocalDate endDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Enums.CycleStatus status = Enums.CycleStatus.OPEN;
    @Column(name = "shared_area_kl", nullable = false) private BigDecimal sharedAreaKl = BigDecimal.ZERO;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    @Column(name = "finalized_at") private Instant finalizedAt;
}
