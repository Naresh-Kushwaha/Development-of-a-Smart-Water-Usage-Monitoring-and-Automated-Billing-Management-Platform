package com.smartwater.events;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DomainEventsTest {
    @Mock ApplicationEventPublisher publisher;
    @InjectMocks DomainEvents events;

    @Test
    void emailPublishesAnEmailRequestedEvent() {
        events.email("a@x.com", "Subject", "Body");
        verify(publisher).publishEvent(new EmailRequested("a@x.com", "Subject", "Body"));
    }

    @Test
    void usageIngestedPublishesTheApartmentId() {
        events.usageIngested(7L);
        verify(publisher).publishEvent(new UsageIngested(7L));
    }
}
