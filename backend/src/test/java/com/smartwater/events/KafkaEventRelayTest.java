package com.smartwater.events;

import com.smartwater.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaEventRelayTest {
    @Mock KafkaTemplate<String, Object> kafka;
    @Mock EmailService emailService;
    @InjectMocks KafkaEventRelay relay;

    private final EmailRequested mail = new EmailRequested("a@x.com", "Hi", "Body");

    @Test
    void email_isPublishedToTheEmailTopic_keyedByRecipient() {
        when(kafka.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

        relay.onEmail(mail);

        verify(kafka).send(Topics.EMAIL, "a@x.com", mail);
        verifyNoInteractions(emailService);
    }

    @Test
    void email_isSentDirectlyWhenKafkaThrows() {
        when(kafka.send(anyString(), anyString(), any())).thenThrow(new RuntimeException("broker down"));

        relay.onEmail(mail);

        verify(emailService).send("a@x.com", "Hi", "Body");
    }

    @Test
    void email_isSentDirectlyWhenThePublishFailsAsynchronously() {
        when(kafka.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("timeout")));

        relay.onEmail(mail);

        verify(emailService).send("a@x.com", "Hi", "Body");
    }

    @Test
    void usageEvent_isKeyedByApartment_andFailuresAreSwallowed() {
        when(kafka.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));
        relay.onUsage(new UsageIngested(5L));
        verify(kafka).send(eq(Topics.USAGE_INGESTED), eq("5"), eq(new UsageIngested(5L)));

        when(kafka.send(anyString(), anyString(), any())).thenThrow(new RuntimeException("down"));
        assertDoesNotThrow(() -> relay.onUsage(new UsageIngested(6L)));
    }
}
