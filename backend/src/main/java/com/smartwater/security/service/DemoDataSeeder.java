package com.smartwater.service;

import com.smartwater.domain.*;
import com.smartwater.domain.Enums.*;
import com.smartwater.repo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Creates a ready-to-demo apartment (SEED_DEMO=true, only when the database is empty). */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.seed-demo", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {
    private final ApartmentRepository apartments;
    private final HouseholdRepository households;
    private final UserRepository users;
    private final TariffRepository tariffs;
    private final UsageRepository usage;
    private final PurchaseRepository purchases;
    private final BillingCycleRepository cycles;
    private final BillingService billing;
    private final AlertService alerts;
    private final PasswordEncoder encoder;

    @Override
    public void run(ApplicationArguments args) {
        if (apartments.count() > 0) return;
        Apartment a = new Apartment();
        a.setName("Green Meadows Residency"); a.setCode("GREEN1"); a.setAddress("12 Lake View Road");
        a = apartments.save(a);

        TariffPlan t = new TariffPlan();
        t.setApartment(a); t.setTier1LimitKl(BigDecimal.TEN); t.setTier1Rate(new BigDecimal("12.00"));
        t.setTier2Rate(new BigDecimal("20.00")); t.setFixedCharge(new BigDecimal("50.00"));
        t.setAlertThresholdKl(new BigDecimal("14"));
        tariffs.save(t);

        createUser("admin@demo.com", "Admin@123", "Asha Admin", Role.ADMIN, a, null);

        String[] units = {"A-101", "A-102", "A-103", "A-104", "B-201", "B-202", "B-203", "B-204", "C-301", "C-302", "C-303", "C-304"};
        List<Household> hs = new ArrayList<>();
        for (int i = 0; i < units.length; i++) {
            Household h = new Household();
            h.setApartment(a); h.setUnitNumber(units[i]);
            h.setAreaSqft(BigDecimal.valueOf(650 + (i % 4) * 150L));
            h.setOccupants(2 + i % 4);
            h.setMeterNumber(i == units.length - 1 ? null : "MTR-" + (1000 + i));   // last unit is unmetered
            hs.add(households.save(h));
        }
        for (int i = 0; i < 4; i++)
            createUser("resident" + (i + 1) + "@demo.com", "Resident@123", "Resident " + (i + 1), Role.RESIDENT, a, hs.get(i));

        Random rnd = new Random(42);
        LocalDate today = LocalDate.now();
        List<WaterUsageLog> logs = new ArrayList<>();
        for (int i = 0; i < hs.size(); i++) {
            Household h = hs.get(i);
            if (!h.hasMeter()) continue;
            for (int d = 75; d >= 1; d--) {
                double litres = h.getOccupants() * 135 * (0.85 + 0.3 * rnd.nextDouble());
                if (i == 1 && d == 1) litres *= 4.5;            // simulated leak on A-102 yesterday
                WaterUsageLog l = new WaterUsageLog();
                l.setHousehold(h); l.setReadingDate(today.minusDays(d));
                l.setLitres(BigDecimal.valueOf(Math.round(litres * 10) / 10.0)); l.setSource("CSV");
                logs.add(l);
            }
        }
        usage.saveAll(logs);

        LocalDate curStart = today.withDayOfMonth(1);
        LocalDate prevStart = curStart.minusMonths(1);
        purchase(a, prevStart.plusDays(2), "Tanker delivery", "80", "14.00");
        purchase(a, prevStart.plusDays(10), "Municipal supply", "120", "6.50");
        purchase(a, curStart, "Municipal supply", "100", "6.50");

        BillingCycle prev = cycle(a, prevStart.getMonth() + " " + prevStart.getYear(), prevStart, curStart.minusDays(1), "30");
        billing.finalizeCycle(a.getId(), prev.getId());
        cycle(a, curStart.getMonth() + " " + curStart.getYear(), curStart, curStart.plusMonths(1).minusDays(1), "30");

        alerts.check(a.getId());
        log.info("Demo data ready. Admin: admin@demo.com / Admin@123 | Resident: resident1@demo.com / Resident@123 | Apartment code: GREEN1");
    }

    private void createUser(String email, String pw, String name, Role role, Apartment a, Household h) {
        User u = new User();
        u.setEmail(email); u.setPasswordHash(encoder.encode(pw)); u.setFullName(name);
        u.setRole(role); u.setApartment(a); u.setHousehold(h);
        users.save(u);
    }

    private void purchase(Apartment a, LocalDate date, String source, String kl, String cost) {
        BulkPurchase p = new BulkPurchase();
        p.setApartment(a); p.setPurchaseDate(date); p.setSource(source);
        p.setVolumeKl(new BigDecimal(kl)); p.setUnitCost(new BigDecimal(cost));
        purchases.save(p);
    }

    private BillingCycle cycle(Apartment a, String label, LocalDate s, LocalDate e, String sharedKl) {
        BillingCycle c = new BillingCycle();
        c.setApartment(a); c.setLabel(label); c.setStartDate(s); c.setEndDate(e); c.setSharedAreaKl(new BigDecimal(sharedKl));
        return cycles.save(c);
    }
}
