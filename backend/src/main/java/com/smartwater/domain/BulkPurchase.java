package com.smartwater.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity @Table(name = "bulk_purchases") @Getter @Setter @NoArgsConstructor
public class BulkPurchase {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "apartment_id") private Apartment apartment;
    @Column(name = "purchase_date", nullable = false) private LocalDate purchaseDate;
    @Column(nullable = false) private String source;
    @Column(name = "volume_kl", nullable = false) private BigDecimal volumeKl;
    @Column(name = "unit_cost", nullable = false) private BigDecimal unitCost;
    private String notes;

    public BigDecimal totalCost() { return volumeKl.multiply(unitCost); }
}
