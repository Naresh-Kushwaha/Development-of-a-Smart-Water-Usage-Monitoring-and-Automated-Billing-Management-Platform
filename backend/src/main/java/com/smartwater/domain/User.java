package com.smartwater.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "users") @Getter @Setter @NoArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true) private String email;
    @Column(name = "password_hash", nullable = false) private String passwordHash;
    @Column(name = "full_name", nullable = false) private String fullName;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Enums.Role role;
    @ManyToOne(optional = false) @JoinColumn(name = "apartment_id") private Apartment apartment;
    @ManyToOne @JoinColumn(name = "household_id") private Household household;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
}
