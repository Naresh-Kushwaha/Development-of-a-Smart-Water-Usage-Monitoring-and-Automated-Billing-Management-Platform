package com.smartwater.service;

import com.smartwater.domain.*;
import com.smartwater.domain.Enums.CycleStatus;
import com.smartwater.repo.*;
import com.smartwater.web.ApiException;
import com.smartwater.web.Dtos.*;
import com.smartwater.web.Mapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);

    private final UsageRepository usage;
    private final BillingCycleRepository cycles;
    private final BillingService billing;
    private final InvoiceRepository invoices;
    private final TariffRepository tariffs;
    private final HouseholdRepository households;
    private final NotificationRepository notifications;

    public DashboardRes resident(User u) {
        Household h = requireHousehold(u);
        Long aid = u.getApartment().getId();
        LocalDate today = LocalDate.now();

        List<DailyPoint> daily = usage.findByHouseholdIdAndReadingDateBetweenOrderByReadingDate(h.getId(), today.minusDays(29), today)
                .stream().map(l -> new DailyPoint(l.getReadingDate(), l.getLitres())).toList();

        List<MonthPoint> monthly = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = YearMonth.from(today).minusMonths(i);
            BigDecimal l = nz(usage.sumLitres(h.getId(), ym.atDay(1), ym.atEndOfMonth()));
            monthly.add(new MonthPoint(ym.toString(), l.divide(THOUSAND, 2, RoundingMode.HALF_UP)));
        }

        CycleSummary summary = cycles.findFirstByApartmentIdAndStatusOrderByStartDateDesc(aid, CycleStatus.OPEN)
                .flatMap(c -> billing.compute(c).stream().filter(d -> d.household().getId().equals(h.getId())).findFirst()
                        .map(d -> new CycleSummary(c.getLabel(), c.getStartDate(), c.getEndDate(), d.consumptionKl(),
                                d.base(), d.shared(), d.total())))
                .orElse(null);

        BigDecimal mtd = nz(usage.sumLitres(h.getId(), today.withDayOfMonth(1), today)).divide(THOUSAND, 2, RoundingMode.HALF_UP);
        BigDecimal threshold = tariffs.findByApartmentId(aid).map(TariffPlan::getAlertThresholdKl).orElse(null);
        List<InvoiceRes> inv = invoices.findByHouseholdIdOrderByCreatedAtDesc(h.getId()).stream().limit(12).map(Mapper::invoice).toList();

        return new DashboardRes(h.getUnitNumber(), daily, monthly, summary, mtd, threshold, inv,
                Tips.daily(4, today.getDayOfYear()), notifications.countByUserIdAndSeenFalse(u.getId()));
    }

    /** Peer benchmarking over the last 30 days: me vs apartment average vs similar-sized (+/-20% area) households. */
    public CompareRes compare(User u) {
        Household me = requireHousehold(u);
        Long aid = u.getApartment().getId();
        LocalDate to = LocalDate.now(), from = to.minusDays(29);

        Map<Long, BigDecimal> litres = new HashMap<>();
        for (Object[] row : usage.totalsByHousehold(aid, from, to)) litres.put((Long) row[0], (BigDecimal) row[1]);
        List<Household> all = households.findByApartmentIdOrderByUnitNumber(aid).stream().filter(Household::hasMeter).toList();

        BigDecimal myAvg = avgDaily(litres.get(me.getId()));
        BigDecimal low = me.getAreaSqft().multiply(new BigDecimal("0.8")), high = me.getAreaSqft().multiply(new BigDecimal("1.2"));
        List<Household> similar = all.stream().filter(h -> !h.getId().equals(me.getId()))
                .filter(h -> h.getAreaSqft().compareTo(low) >= 0 && h.getAreaSqft().compareTo(high) <= 0).toList();

        BigDecimal aptAvg = mean(all.stream().map(h -> avgDaily(litres.get(h.getId()))).toList());
        BigDecimal simAvg = mean(similar.stream().map(h -> avgDaily(litres.get(h.getId()))).toList());
        BigDecimal perPerson = myAvg.divide(BigDecimal.valueOf(Math.max(1, me.getOccupants())), 1, RoundingMode.HALF_UP);
        BigDecimal aptPerPerson = mean(all.stream().map(h -> avgDaily(litres.get(h.getId()))
                .divide(BigDecimal.valueOf(Math.max(1, h.getOccupants())), 1, RoundingMode.HALF_UP)).toList());

        List<Household> pool = new ArrayList<>(similar);
        long better = pool.stream().filter(h -> avgDaily(litres.get(h.getId())).compareTo(myAvg) > 0).count();
        int pct = pool.isEmpty() ? 50 : (int) Math.round(100.0 * better / pool.size());
        return new CompareRes(myAvg, aptAvg, simAvg, similar.size(), perPerson, aptPerPerson, pct);
    }

    private Household requireHousehold(User u) {
        if (u.getHousehold() == null)
            throw ApiException.bad("No household is linked to your account yet - ask your apartment admin to assign your unit.");
        return u.getHousehold();
    }

    private static BigDecimal avgDaily(BigDecimal total) { return nz(total).divide(BigDecimal.valueOf(30), 1, RoundingMode.HALF_UP); }
    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private static BigDecimal mean(List<BigDecimal> v) {
        if (v.isEmpty()) return BigDecimal.ZERO;
        return v.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(v.size()), 1, RoundingMode.HALF_UP);
    }
}
