package com.smartwater.repo;

import com.smartwater.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndSeenFalse(Long userId);
    boolean existsByUserIdAndTypeAndCreatedAtAfter(Long userId, Enums.NotificationType type, Instant after);
    boolean existsByUserIdAndTypeAndMessage(Long userId, Enums.NotificationType type, String message);
}
