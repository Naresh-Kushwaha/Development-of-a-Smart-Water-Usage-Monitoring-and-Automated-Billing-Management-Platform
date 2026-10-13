package com.smartwater.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity @Table(name = "water_usage_logs") @Getter @Setter @NoArgsConstructor
public class WaterUsageLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "household_id") private Household household;
    @Column(name = "reading_date", nullable = false) private LocalDate readingDate;
    @Column(nullable = false) private BigDecimal litres;
    @Column(nullable = false) private String source;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
}
