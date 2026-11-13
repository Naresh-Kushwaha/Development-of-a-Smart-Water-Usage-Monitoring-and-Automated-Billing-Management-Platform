package com.smartwater.events;

import com.smartwater.service.AlertService;
import com.smartwater.service.EmailService;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Workers. If a handler throws, DefaultErrorHandler (KafkaConfig) retries 3 times and then moves the
 * message to "<topic>.DLT" so one bad message never blocks the queue.
 * Delivery is at-least-once, so in rare cases a user can receive the same email twice.
 */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
public class KafkaConsumers {
    private final EmailService emailService;
    private final AlertService alertService;
    @Value("${app.mail.enabled:true}")
    private boolean enabled;

    @Value("${app.mail.from}")
    private String from;
    @Value("${app.public-url:http://localhost:5173}")
    private String appUrl;
    @KafkaListener(topics = Topics.EMAIL, groupId = "smartwater-email")
    public void onEmail(EmailRequested e) {
        emailService.deliver(e.to(), e.subject(), e.body());   // throws on SMTP failure -> retry -> DLT
    }

    @KafkaListener(topics = Topics.USAGE_INGESTED, groupId = "smartwater-alerts")
    public void onUsage(UsageIngested e) {
        alertService.check(e.apartmentId());
    }
}
