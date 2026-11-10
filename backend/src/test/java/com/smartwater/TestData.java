package com.smartwater;

import com.smartwater.domain.*;
import com.smartwater.domain.Enums.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Small factory helpers shared by the service tests (no database needed). */
public final class TestData {
    private TestData() {}

    public static BigDecimal bd(String s) { return new BigDecimal(s); }

    public static Apartment apartment(long id) {
        Apartment a = new Apartment();
        a.setId(id);
        a.setName("Green Meadows");
        a.setCode("ABC123");
        return a;
    }

    /** meter == null means an unmetered household. */
    public static Household household(long id, Apartment a, String unit, String area, int occupants, String meter) {
        Household h = new Household();
        h.setId(id);
        h.setApartment(a);
        h.setUnitNumber(unit);
        h.setAreaSqft(new BigDecimal(area));
        h.setOccupants(occupants);
        h.setMeterNumber(meter);
        return h;
    }

    public static User user(long id, String email, Role role, Apartment a, Household h) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setFullName("Test " + role);
        u.setPasswordHash("hash");
        u.setRole(role);
        u.setApartment(a);
        u.setHousehold(h);
        return u;
    }

    /** 10 kL @ 12, then 20, fixed 50, shared by AREA, TIERED, alert at 15 kL. */
    public static TariffPlan tariff(Apartment a) {
        TariffPlan t = new TariffPlan();
        t.setApartment(a);
        t.setTier1LimitKl(bd("10"));
        t.setTier1Rate(bd("12.00"));
        t.setTier2Rate(bd("20.00"));
        t.setFixedCharge(bd("50.00"));
        t.setSharedMethod(SharedMethod.AREA);
        t.setBillingMode(BillingMode.TIERED);
        t.setAlertThresholdKl(bd("15"));
        return t;
    }

    public static BillingCycle cycle(long id, Apartment a, LocalDate start, LocalDate end, String sharedKl, CycleStatus status) {
        BillingCycle c = new BillingCycle();
        c.setId(id);
        c.setApartment(a);
        c.setLabel("Cycle " + id);
        c.setStartDate(start);
        c.setEndDate(end);
        c.setSharedAreaKl(new BigDecimal(sharedKl));
        c.setStatus(status);
        return c;
    }

    public static Invoice invoice(long id, BillingCycle c, Household h, String base, String shared) {
        Invoice i = new Invoice();
        i.setId(id);
        i.setBillingCycle(c);
        i.setHousehold(h);
        i.setConsumptionKl(bd("15.000"));
        i.setBaseCharge(new BigDecimal(base));
        i.setSharedCharge(new BigDecimal(shared));
        i.setAdjustment(BigDecimal.ZERO);
        i.setTotal(new BigDecimal(base).add(new BigDecimal(shared)));
        return i;
    }
}
