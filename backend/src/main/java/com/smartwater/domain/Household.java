package com.smartwater.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name = "households") @Getter @Setter @NoArgsConstructor
public class Household {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "apartment_id") private Apartment apartment;
    @Column(name = "unit_number", nullable = false) private String unitNumber;
    @Column(name = "area_sqft", nullable = false) private BigDecimal areaSqft;
    @Column(nullable = false) private int occupants = 1;
    @Column(name = "meter_number") private String meterNumber;

    public boolean hasMeter() { return meterNumber != null && !meterNumber.isBlank(); }
}
