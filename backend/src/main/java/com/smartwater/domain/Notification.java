package com.smartwater.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "notifications") @Getter @Setter @NoArgsConstructor
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id") private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Enums.NotificationType type;
    @Column(nullable = false, length = 500) private String message;
    @Column(name = "is_read", nullable = false) private boolean seen;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
}
