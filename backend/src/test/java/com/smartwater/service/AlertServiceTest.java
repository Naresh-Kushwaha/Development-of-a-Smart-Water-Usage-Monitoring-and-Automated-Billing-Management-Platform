package com.smartwater.service;

import com.smartwater.domain.*;
import com.smartwater.domain.Enums.*;
import com.smartwater.events.DomainEvents;
import com.smartwater.store.KeyValueStore;
import com.smartwater.repo.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.smartwater.TestData.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {
    @Mock ApartmentRepository apartments;
    @Mock HouseholdRepository households;
    @Mock UsageRepository usage;
    @Mock TariffRepository tariffs;
    @Mock UserRepository users;
    @Mock NotificationRepository notifications;
    @Mock DomainEvents events;
    @Mock KeyValueStore store;
    @InjectMocks AlertService service;

    private final Apartment apt = apartment(1L);
    private final Household h1 = household(1L, apt, "A-1", "900", 3, "M1");
    private final User resident = user(10L, "res@x.com", Role.RESIDENT, apt, h1);
    private final User admin = user(1L, "admin@x.com", Role.ADMIN, apt, null);

    private static final double[] NORMAL = {390, 410, 390, 410, 390, 410, 390, 410};

    // ---------- helpers ----------
    private void stubApartment() {
        when(tariffs.findByApartmentId(1L)).thenReturn(Optional.of(tariff(apt)));      // threshold 15 kL
        when(households.findByApartmentIdOrderByUnitNumber(1L)).thenReturn(List.of(h1));
        when(users.findByHouseholdId(1L)).thenReturn(List.of(resident));
        when(users.findByApartmentIdAndRole(1L, Role.ADMIN)).thenReturn(List.of(admin));
    }

    private void stubMonthToDateLitres(String litres) {
        when(usage.sumLitres(eq(1L), any(), any())).thenReturn(bd(litres));
    }

    /** Builds consecutive daily logs; the last log is dated `lastDaysAgo` days before today. */
    private List<WaterUsageLog> logs(double[] baseline, double latest, int lastDaysAgo) {
        double[] all = new double[baseline.length + 1];
        System.arraycopy(baseline, 0, all, 0, baseline.length);
        all[baseline.length] = latest;
        List<WaterUsageLog> list = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < all.length; i++) {
            WaterUsageLog l = new WaterUsageLog();
            l.setHousehold(h1);
            l.setReadingDate(today.minusDays(lastDaysAgo + (all.length - 1 - i)));
            l.setLitres(BigDecimal.valueOf(all[i]));
            list.add(l);
        }
        return list;
    }

    private void stubLogs(List<WaterUsageLog> logs) {
        when(usage.findByHouseholdIdAndReadingDateBetweenOrderByReadingDate(eq(1L), any(), any())).thenReturn(logs);
    }

    // ================= no tariff =================
    @Test
    void check_doesNothingWhenApartmentHasNoTariff() {
        when(tariffs.findByApartmentId(1L)).thenReturn(Optional.empty());

        AlertService.Report r = service.check(1L);

        assertEquals(0, r.overuse());
        assertEquals(0, r.anomalies());
        verifyNoInteractions(households, notifications, events);
    }

    // ================= overuse =================
    @Test
    void check_overuse_notifiesAndEmailsResident() {
        stubApartment();
        stubMonthToDateLitres("16000");                    // 16 kL > 15 kL threshold

        AlertService.Report r = service.check(1L);

        assertEquals(1, r.overuse());
        ArgumentCaptor<Notification> cap = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).save(cap.capture());
        assertEquals(NotificationType.OVERUSE, cap.getValue().getType());
        assertSame(resident, cap.getValue().getUser());
        assertTrue(cap.getValue().getMessage().contains("16.00 kL"));
        verify(events).email(eq("res@x.com"), contains("overuse"), anyString());
    }

    @Test
    void check_overuse_isSentOnlyOncePerMonth() {
        stubApartment();
        stubMonthToDateLitres("16000");
        when(notifications.existsByUserIdAndTypeAndCreatedAtAfter(eq(10L), eq(NotificationType.OVERUSE), any()))
                .thenReturn(true);

        AlertService.Report r = service.check(1L);

        assertEquals(0, r.overuse());
        verify(notifications, never()).save(any());
        verify(events, never()).email(anyString(), anyString(), anyString());
    }

    @Test
    void check_belowThresholdSendsNothing() {
        stubApartment();
        stubMonthToDateLitres("5000");

        AlertService.Report r = service.check(1L);

        assertEquals(0, r.overuse());
        assertEquals(0, r.anomalies());
        verify(notifications, never()).save(any());
    }

    // ================= leak / anomaly =================
    @Test
    void check_leak_alertsResidentAndAdmin() {
        stubApartment();
        stubLogs(logs(NORMAL, 2000, 1));                   // yesterday's reading is far above the ~400 L norm

        AlertService.Report r = service.check(1L);

        assertEquals(1, r.anomalies());
        assertEquals(0, r.overuse());
        verify(notifications, times(2)).save(any(Notification.class));               // resident + admin
        verify(events).email(eq("res@x.com"), contains("Possible water leak"), anyString());
        verify(events).email(eq("admin@x.com"), eq("Anomaly detection report"), contains("A-1"));
    }

    @Test
    void check_leak_sameAlertIsNotRepeated() {
        stubApartment();
        stubLogs(logs(NORMAL, 2000, 1));
        when(notifications.existsByUserIdAndTypeAndMessage(anyLong(), eq(NotificationType.LEAK), anyString()))
                .thenReturn(true);

        service.check(1L);

        verify(notifications, never()).save(any());
        verify(events, never()).email(anyString(), anyString(), anyString());
    }

    @Test
    void check_noLeakForNormalUsage() {
        stubApartment();
        stubLogs(logs(NORMAL, 410, 1));

        assertEquals(0, service.check(1L).anomalies());
        verify(notifications, never()).save(any());
    }

    @Test
    void check_noLeakWithTooLittleHistory() {
        stubApartment();
        stubLogs(logs(new double[]{390, 410, 390, 410, 390, 410}, 2000, 1));   // only 7 readings in total

        assertEquals(0, service.check(1L).anomalies());
    }

    @Test
    void check_ignoresASpikeThatIsNotRecent() {
        stubApartment();
        stubLogs(logs(NORMAL, 2000, 5));                   // spike was 5 days ago

        assertEquals(0, service.check(1L).anomalies());
        verify(notifications, never()).save(any());
    }

    // ================= scheduler =================
    @Test
    void scheduledRun_continuesWithOtherApartmentsWhenOneFails() {
        when(store.setIfAbsent(anyString(), any())).thenReturn(true);
        when(apartments.findAll()).thenReturn(List.of(apartment(1L), apartment(2L)));
        when(tariffs.findByApartmentId(1L)).thenThrow(new RuntimeException("boom"));
        when(tariffs.findByApartmentId(2L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.scheduledRun());

        verify(tariffs).findByApartmentId(2L);
    }

    @Test
    void scheduledRun_doesNothingWhenAnotherInstanceHoldsTheLock() {
        when(store.setIfAbsent(anyString(), any())).thenReturn(false);

        service.scheduledRun();

        verifyNoInteractions(apartments, tariffs);
    }
}
