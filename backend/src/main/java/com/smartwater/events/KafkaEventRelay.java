package com.smartwater.events;

import com.smartwater.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** app.kafka.enabled=true: after the DB commit, forward events to Kafka. */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class KafkaEventRelay {
    private final KafkaTemplate<String, Object> kafka;
    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onEmail(EmailRequested e) {
        try {
            kafka.send(Topics.EMAIL, e.to(), e).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.warn("Kafka publish failed, sending email directly: {}", ex.getMessage());
                    emailService.send(e.to(), e.subject(), e.body());
                }
            });
        } catch (RuntimeException ex) {
            log.warn("Kafka unavailable, sending email directly: {}", ex.getMessage());
            emailService.send(e.to(), e.subject(), e.body());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUsage(UsageIngested e) {
        try {
            // key = apartment id => all events of one apartment stay in order on one partition
            kafka.send(Topics.USAGE_INGESTED, String.valueOf(e.apartmentId()), e).whenComplete((result, ex) -> {
                if (ex != null) log.warn("Could not publish usage event (the daily alert job will catch up): {}", ex.getMessage());
            });
        } catch (RuntimeException ex) {
            log.warn("Kafka unavailable, usage event dropped (the daily alert job will catch up): {}", ex.getMessage());
        }
    }
}
