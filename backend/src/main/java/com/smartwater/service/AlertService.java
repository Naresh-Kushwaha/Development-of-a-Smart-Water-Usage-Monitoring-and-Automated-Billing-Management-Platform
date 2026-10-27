package com.smartwater.service;

import com.smartwater.billing.BillingCalc;
import com.smartwater.domain.*;
import com.smartwater.domain.Enums.*;
import com.smartwater.repo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {
    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);

    private final ApartmentRepository apartments;
    private final HouseholdRepository households;
    private final UsageRepository usage;
    private final TariffRepository tariffs;
    private final UserRepository users;
    private final NotificationRepository notifications;
    private final EmailService email;

    public record Report(int overuse, int anomalies) {}

    /** Runs daily (configurable via app.alerts.cron) for every apartment. */
    @Scheduled(cron = "${app.alerts.cron}")
    public void scheduledRun() {
        for (Apartment a : apartments.findAll()) {
            try { 
                Report r = check(a.getId());
                log.info("Alert run for {}: {}", a.getName(), r); 
            }
            catch (Exception e) { log.error("Alert run failed for apartment {}", a.getId(), e); }
        }
    }

    /**
     * 1) OVERUSE: month-to-date usage above the apartment's configurable threshold (once per month per resident).
     * 2) LEAK: latest daily reading more than 2 sigma above the household's own recent average.
     */
    @Transactional
    public Report check(Long apartmentId) {
        TariffPlan tariff = tariffs.findByApartmentId(apartmentId).orElse(null);
        if (tariff == null) return new Report(0, 0);

        LocalDate today = LocalDate.now();
        Instant monthStart = today.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        List<User> admins = users.findByApartmentIdAndRole(apartmentId, Role.ADMIN);
        List<String> newAdminLines = new ArrayList<>();
        int over = 0, anomalies = 0;

        for (Household h : households.findByApartmentIdOrderByUnitNumber(apartmentId)) {
            List<User> residents = users.findByHouseholdId(h.getId());

            // ---- overuse threshold ----
            BigDecimal mtdKl = nz(usage.sumLitres(h.getId(), today.withDayOfMonth(1), today))
                    .divide(THOUSAND, 2, RoundingMode.HALF_UP);
            if (mtdKl.compareTo(tariff.getAlertThresholdKl()) > 0) {
                String msg = String.format(Locale.US, "Unit %s has used %.2f kL this month, above the %.2f kL alert threshold.",
                        h.getUnitNumber(), mtdKl, tariff.getAlertThresholdKl());
                for (User u : residents) {
                    if (notifyOnce(u, NotificationType.OVERUSE, msg, monthStart)) {
                        over++;
                        email.send(u.getEmail(), "Water overuse alert - unit " + h.getUnitNumber(),
                                msg + "\n\nTips to cut back:\n- " + String.join("\n- ", Tips.daily(3, today.getDayOfYear())));
                    }
                }
            }

            // ---- statistical anomaly (potential leak) ----
            List<WaterUsageLog> logs = usage.findByHouseholdIdAndReadingDateBetweenOrderByReadingDate(
                    h.getId(), today.minusDays(31), today);
            if (logs.size() >= 8) {
                WaterUsageLog latest = logs.get(logs.size() - 1);
                double[] baseline = logs.subList(0, logs.size() - 1).stream().mapToDouble(l -> l.getLitres().doubleValue()).toArray();
                if (!latest.getReadingDate().isBefore(today.minusDays(1))
                        && BillingCalc.isAnomaly(latest.getLitres().doubleValue(), baseline)) {
                    anomalies++;
                    String detail = String.format(Locale.US, "Unit %s used %.0f L on %s versus a recent daily average of %.0f L - possible leak.",
                            h.getUnitNumber(), latest.getLitres(), latest.getReadingDate(), BillingCalc.mean(baseline));
                    for (User u : residents) {
                        if (notifyOnce(u, NotificationType.LEAK, detail, null))
                            email.send(u.getEmail(), "Possible water leak - unit " + h.getUnitNumber(),
                                    detail + "\n\nCheck taps, toilet cisterns and pipes. If usage stays high, contact your apartment admin.");
                    }
                    for (User a : admins) if (notifyOnce(a, NotificationType.LEAK, detail, null)) newAdminLines.add(detail);
                }
            }
        }
        if (!newAdminLines.isEmpty()) {
            String body = "Anomaly report:\n\n- " + String.join("\n- ", newAdminLines.stream().distinct().toList());
            for (User a : admins) email.send(a.getEmail(), "Anomaly detection report", body);
        }
        return new Report(over, anomalies);
    }

    /** Creates an in-app notification unless an equivalent one exists. Returns true if newly created. */
    private boolean notifyOnce(User u, NotificationType type, String msg, Instant since) {
        boolean exists = since != null
                ? notifications.existsByUserIdAndTypeAndCreatedAtAfter(u.getId(), type, since)
                : notifications.existsByUserIdAndTypeAndMessage(u.getId(), type, msg);
        if (exists) return false;
        Notification n = new Notification();
        n.setUser(u); n.setType(type); n.setMessage(msg);
        notifications.save(n);
        return true;
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
