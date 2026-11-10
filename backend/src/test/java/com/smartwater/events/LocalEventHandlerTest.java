package com.smartwater.events;

import com.smartwater.service.AlertService;
import com.smartwater.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalEventHandlerTest {
    @Mock EmailService emailService;
    @Mock AlertService alertService;
    @InjectMocks LocalEventHandler handler;

    @Test
    void emailEventSendsTheMailDirectly() {
        handler.onEmail(new EmailRequested("a@x.com", "Hi", "Body"));
        verify(emailService).send("a@x.com", "Hi", "Body");
    }

    @Test
    void usageEventRunsTheAlertCheck() {
        handler.onUsage(new UsageIngested(3L));
        verify(alertService).check(3L);
    }

    @Test
    void aFailingAlertCheckDoesNotPropagate() {
        when(alertService.check(3L)).thenThrow(new RuntimeException("db down"));
        assertDoesNotThrow(() -> handler.onUsage(new UsageIngested(3L)));
    }
}
