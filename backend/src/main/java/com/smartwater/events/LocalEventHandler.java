package com.smartwater.events;

import com.smartwater.service.AlertService;
import com.smartwater.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** app.kafka.enabled=false (default): handle the same events in-process, so the app still works without Kafka. */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class LocalEventHandler {
    private final EmailService emailService;
    private final AlertService alertService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onEmail(EmailRequested e) {
        emailService.send(e.to(), e.subject(), e.body());      // never throws
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUsage(UsageIngested e) {
        try {
            alertService.check(e.apartmentId());               // REQUIRES_NEW transaction
        } catch (RuntimeException ex) {
            log.warn("Alert check failed for apartment {}: {}", e.apartmentId(), ex.getMessage());
        }
    }
}
