package com.smartwater.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "apartments") @Getter @Setter @NoArgsConstructor
public class Apartment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String name;
    @Column(nullable = false, unique = true) private String code;
    private String address;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
}
