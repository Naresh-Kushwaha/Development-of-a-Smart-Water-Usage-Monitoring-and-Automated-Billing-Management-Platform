package com.smartwater.service;

import com.smartwater.billing.BillingCalc;
import com.smartwater.domain.*;
import com.smartwater.domain.Enums.*;
import com.smartwater.repo.*;
import com.smartwater.web.ApiException;
import com.smartwater.web.Dtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BillingService {
    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);

    private final BillingCycleRepository cycles;
    private final ApartmentRepository apartments;
    private final HouseholdRepository households;
    private final UsageRepository usage;
    private final TariffRepository tariffs;
    private final PurchaseRepository purchases;
    private final InvoiceRepository invoices;
    private final UserRepository users;
    private final NotificationRepository notifications;
    private final EmailService email;

    public record Draft(Household household, BigDecimal consumptionKl, BigDecimal base, BigDecimal shared) {
        public BigDecimal total() { return base.add(shared); }
    }

    // ---------- tariff ----------
    public TariffPlan tariff(Long apartmentId) {
        return tariffs.findByApartmentId(apartmentId)
                .orElseThrow(() -> ApiException.bad("Configure a tariff plan first"));
    }

    @Transactional
    public TariffPlan updateTariff(Long apartmentId, TariffDto d) {
        TariffPlan t = tariffs.findByApartmentId(apartmentId).orElseGet(() -> {
            TariffPlan n = new TariffPlan();
            n.setApartment(apartments.getReferenceById(apartmentId));
            return n;
        });
        t.setTier1LimitKl(d.tier1LimitKl()); t.setTier1Rate(d.tier1Rate()); t.setTier2Rate(d.tier2Rate());
        t.setFixedCharge(d.fixedCharge()); t.setSharedMethod(d.sharedMethod()); t.setBillingMode(d.billingMode());
        t.setAlertThresholdKl(d.alertThresholdKl()); t.setUpdatedAt(Instant.now());
        return tariffs.save(t);
    }

    // ---------- bulk purchases ----------
    @Transactional
    public BulkPurchase addPurchase(Long apartmentId, PurchaseReq r) {
        BulkPurchase p = new BulkPurchase();
        p.setApartment(apartments.getReferenceById(apartmentId));
        p.setPurchaseDate(r.date()); p.setSource(r.source().trim()); p.setVolumeKl(r.volumeKl());
        p.setUnitCost(r.unitCost()); p.setNotes(r.notes());
        return purchases.save(p);
    }

    @Transactional
    public void deletePurchase(Long apartmentId, Long id) {
        purchases.delete(purchases.findByIdAndApartmentId(id, apartmentId).orElseThrow(() -> ApiException.notFound("Purchase")));
    }

    // ---------- cycle lifecycle: OPEN -> FINALIZED -> ARCHIVED ----------
    @Transactional
    public BillingCycle open(Long apartmentId, CycleReq r) {
        if (r.endDate().isBefore(r.startDate())) throw ApiException.bad("End date must not be before start date");
        boolean overlap = cycles.findByApartmentIdOrderByStartDateDesc(apartmentId).stream()
                .anyMatch(c -> !r.startDate().isAfter(c.getEndDate()) && !r.endDate().isBefore(c.getStartDate()));
        if (overlap) throw ApiException.conflict("Dates overlap an existing billing cycle");
        BillingCycle c = new BillingCycle();
        c.setApartment(apartments.getReferenceById(apartmentId));
        c.setLabel(r.label().trim()); c.setStartDate(r.startDate()); c.setEndDate(r.endDate());
        c.setSharedAreaKl(r.sharedAreaKl() == null ? BigDecimal.ZERO : r.sharedAreaKl());
        return cycles.save(c);
    }

    public BillingCycle cycle(Long apartmentId, Long id) {
        return cycles.findByIdAndApartmentId(id, apartmentId).orElseThrow(() -> ApiException.notFound("Billing cycle"));
    }

    @Transactional
    public List<Invoice> finalizeCycle(Long apartmentId, Long cycleId) {
        BillingCycle c = cycle(apartmentId, cycleId);
        if (c.getStatus() != CycleStatus.OPEN) throw ApiException.conflict("Only an OPEN cycle can be finalized");
        List<Invoice> out = new ArrayList<>();
        for (Draft d : compute(c)) {
            Invoice i = new Invoice();
            i.setBillingCycle(c); i.setHousehold(d.household()); i.setConsumptionKl(d.consumptionKl());
            i.setBaseCharge(d.base()); i.setSharedCharge(d.shared()); i.setAdjustment(BigDecimal.ZERO);
            i.setTotal(d.total());
            out.add(invoices.save(i));
        }
        c.setStatus(CycleStatus.FINALIZED);
        c.setFinalizedAt(Instant.now());
        cycles.save(c);
        out.forEach(this::notifyBill);
        return out;
    }

    @Transactional
    public BillingCycle archive(Long apartmentId, Long cycleId) {
        BillingCycle c = cycle(apartmentId, cycleId);
        if (c.getStatus() != CycleStatus.FINALIZED) throw ApiException.conflict("Only a FINALIZED cycle can be archived");
        c.setStatus(CycleStatus.ARCHIVED);
        return cycles.save(c);
    }

    @Transactional
    public Invoice adjust(Long apartmentId, Long invoiceId, AdjustmentReq r) {
        Invoice i = invoice(apartmentId, invoiceId);
        if (i.getBillingCycle().getStatus() == CycleStatus.OPEN) throw ApiException.conflict("Cycle is not finalized");
        i.setAdjustment(r.amount()); i.setAdjustmentNote(r.note());
        i.setTotal(i.getBaseCharge().add(i.getSharedCharge()).add(r.amount()));
        return invoices.save(i);
    }

    @Transactional
    public Invoice markPaid(Long apartmentId, Long invoiceId) {
        Invoice i = invoice(apartmentId, invoiceId);
        i.setStatus(InvoiceStatus.PAID);
        return invoices.save(i);
    }

    private Invoice invoice(Long apartmentId, Long id) {
        return invoices.findById(id)
                .filter(i -> i.getHousehold().getApartment().getId().equals(apartmentId))
                .orElseThrow(() -> ApiException.notFound("Invoice"));
    }

    // ---------- the billing engine ----------
    /**
     * Computes (without saving) every household's charges for a cycle.
     * - Shared-area cost = shared kL (garden/pool/lobby) x weighted average purchase cost per kL,
     *   split by flat area or occupancy.
     * - Base charge = TIERED: per-household tiered tariff on metered kL (+ fixed charge)
     *                 COST_SHARING: remaining purchase cost split proportionally to metered consumption,
     *                 unmetered households falling back to a flat-area basis.
     */
    public List<Draft> compute(BillingCycle c) {
        Long aid = c.getApartment().getId();
        TariffPlan t = tariff(aid);
        List<Household> hs = households.findByApartmentIdOrderByUnitNumber(aid);

        Map<Long, BigDecimal> litres = new HashMap<>();
        for (Object[] row : usage.totalsByHousehold(aid, c.getStartDate(), c.getEndDate()))
            litres.put((Long) row[0], (BigDecimal) row[1]);

        List<BillingCalc.Unit> units = hs.stream().map(h -> new BillingCalc.Unit(h.getId(), h.getAreaSqft(),
                h.getOccupants(), h.hasMeter(),
                litres.getOrDefault(h.getId(), BigDecimal.ZERO).divide(THOUSAND, 3, RoundingMode.HALF_UP))).toList();

        List<BulkPurchase> ps = purchases.findByApartmentIdAndPurchaseDateBetween(aid, c.getStartDate(), c.getEndDate());
        BigDecimal volume = ps.stream().map(BulkPurchase::getVolumeKl).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cost = ps.stream().map(BulkPurchase::totalCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unitCost = volume.signum() > 0 ? cost.divide(volume, 4, RoundingMode.HALF_UP) : t.getTier1Rate();
        BigDecimal sharedCost = c.getSharedAreaKl().multiply(unitCost).setScale(2, RoundingMode.HALF_UP);

        Map<Long, BigDecimal> shared = BillingCalc.distribute(sharedCost,
                BillingCalc.sharedWeights(units, t.getSharedMethod() == SharedMethod.AREA));

        Map<Long, BigDecimal> base;
        if (t.getBillingMode() == BillingMode.TIERED) {
            base = new HashMap<>();
            for (var u : units) base.put(u.id(), BillingCalc.tiered(u.kl(), t.getTier1LimitKl(), t.getTier1Rate(),
                    t.getTier2Rate(), t.getFixedCharge()));
        } else {
            base = BillingCalc.distribute(cost.subtract(sharedCost).max(BigDecimal.ZERO), BillingCalc.costWeights(units));
        }

        List<Draft> drafts = new ArrayList<>();
        for (int i = 0; i < hs.size(); i++) {
            Household h = hs.get(i);
            drafts.add(new Draft(h, units.get(i).kl(), base.getOrDefault(h.getId(), BigDecimal.ZERO),
                    shared.getOrDefault(h.getId(), BigDecimal.ZERO)));
        }
        return drafts;
    }

    private void notifyBill(Invoice i) {
        String msg = String.format(Locale.US, "Your water bill for %s (unit %s) is ready: INR %,.2f for %.2f kL.",
                i.getBillingCycle().getLabel(), i.getHousehold().getUnitNumber(), i.getTotal(), i.getConsumptionKl());
        for (User u : users.findByHouseholdId(i.getHousehold().getId())) {
            Notification n = new Notification();
            n.setUser(u); n.setType(NotificationType.BILL); n.setMessage(msg);
            notifications.save(n);
            email.send(u.getEmail(), "Water bill ready - " + i.getBillingCycle().getLabel(),
                    "Hello " + u.getFullName() + ",\n\n" + msg + "\nLog in to view the itemised invoice and download the PDF.\n");
        }
    }
}
