package com.smartwater.events;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * The only thing business services know about messaging. Events are published inside the DB transaction but
 * only acted on AFTER it commits (see KafkaEventRelay / LocalEventHandler), so consumers never see data that
 * is later rolled back.
 */
@Component
@RequiredArgsConstructor
public class DomainEvents {
    private final ApplicationEventPublisher publisher;

    public void email(String to, String subject, String body) {
        publisher.publishEvent(new EmailRequested(to, subject, body));
    }

    public void usageIngested(Long apartmentId) {
        publisher.publishEvent(new UsageIngested(apartmentId));
    }
}
