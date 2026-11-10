package com.smartwater.service;

import com.smartwater.domain.*;
import com.smartwater.domain.Enums.*;
import com.smartwater.events.DomainEvents;
import com.smartwater.repo.*;
import com.smartwater.web.ApiException;
import com.smartwater.web.Dtos.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.smartwater.TestData.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {
    @Mock BillingCycleRepository cycles;
    @Mock ApartmentRepository apartments;
    @Mock HouseholdRepository households;
    @Mock UsageRepository usage;
    @Mock TariffRepository tariffs;
    @Mock PurchaseRepository purchaseRepo;
    @Mock InvoiceRepository invoices;
    @Mock UserRepository users;
    @Mock NotificationRepository notifications;
    @Mock DomainEvents events;
    @InjectMocks BillingService service;

    private final Apartment apt = apartment(1L);
    private final Household h1 = household(1L, apt, "A-1", "1000", 3, "M1");
    private final Household h2 = household(2L, apt, "A-2", "1000", 2, "M2");
    private final LocalDate start = LocalDate.of(2026, 9, 1);
    private final LocalDate end = LocalDate.of(2026, 9, 30);
    private final BillingCycle cycle = cycle(1L, apt, start, end, "10", CycleStatus.OPEN);
    private final TariffPlan tariff = tariff(apt);

    // ---------- helpers ----------
    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    private static BulkPurchase purchase(String kl, String unitCost) {
        BulkPurchase p = new BulkPurchase();
        p.setVolumeKl(bd(kl));
        p.setUnitCost(bd(unitCost));
        return p;
    }

    /** h1 used 15 kL, h2 used 8 kL during the cycle. */
    private void stubCycleData(List<BulkPurchase> bought) {
        when(tariffs.findByApartmentId(1L)).thenReturn(Optional.of(tariff));
        when(households.findByApartmentIdOrderByUnitNumber(1L)).thenReturn(List.of(h1, h2));
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{1L, bd("15000")});
        rows.add(new Object[]{2L, bd("8000")});
        when(usage.totalsByHousehold(1L, start, end)).thenReturn(rows);
        when(purchaseRepo.findByApartmentIdAndPurchaseDateBetween(1L, start, end)).thenReturn(bought);
    }

    // ================= compute: tiered =================
    @Test
    void compute_tiered_appliesSlabsFixedChargeAndSharedAreaSplit() {
        stubCycleData(List.of(purchase("100", "10")));   // 100 kL @ 10 => unit cost 10; shared 10 kL => 100.00

        List<BillingService.Draft> d = service.compute(cycle);

        // h1: 10*12 + 5*20 + 50 = 270 ; h2: 8*12 + 50 = 146 ; shared 100 split by equal area = 50 each
        assertMoney("15", d.get(0).consumptionKl());
        assertMoney("270.00", d.get(0).base());
        assertMoney("50.00", d.get(0).shared());
        assertMoney("320.00", d.get(0).total());
        assertMoney("146.00", d.get(1).base());
        assertMoney("196.00", d.get(1).total());
    }

    @Test
    void compute_sharedAreaCostFallsBackToTier1RateWhenNothingWasPurchased() {
        stubCycleData(List.of());                        // 10 kL shared * tier-1 rate 12 = 120 => 60 each

        List<BillingService.Draft> d = service.compute(cycle);

        assertMoney("60.00", d.get(0).shared());
        assertMoney("60.00", d.get(1).shared());
    }

    @Test
    void compute_sharedAreaCanBeSplitByOccupancy() {
        tariff.setSharedMethod(SharedMethod.OCCUPANCY);  // h1 has 3 people, h2 has 2 => 3:2
        stubCycleData(List.of(purchase("100", "10")));   // shared cost 100

        List<BillingService.Draft> d = service.compute(cycle);

        assertMoney("60.00", d.get(0).shared());
        assertMoney("40.00", d.get(1).shared());
    }

    // ================= compute: cost sharing =================
    @Test
    void compute_costSharing_splitsPurchaseCostByConsumptionAndEverythingAddsUp() {
        tariff.setBillingMode(BillingMode.COST_SHARING);
        stubCycleData(List.of(purchase("100", "10")));   // total cost 1000; shared 100; pool 900 split 15:8

        List<BillingService.Draft> d = service.compute(cycle);

        assertMoney("586.96", d.get(0).base());
        assertMoney("313.04", d.get(1).base());
        BigDecimal billed = d.get(0).total().add(d.get(1).total());
        assertMoney("1000.00", billed);                  // residents repay exactly what the apartment paid
    }

    @Test
    void compute_costSharing_unmeteredHouseholdUsesFlatAreaFallback() {
        Household unmetered = household(3L, apt, "A-3", "1000", 2, null);
        tariff.setBillingMode(BillingMode.COST_SHARING);
        when(tariffs.findByApartmentId(1L)).thenReturn(Optional.of(tariff));
        when(households.findByApartmentIdOrderByUnitNumber(1L)).thenReturn(List.of(h1, unmetered));
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{1L, bd("15000")});         // only the metered flat has readings
        when(usage.totalsByHousehold(1L, start, end)).thenReturn(rows);
        when(purchaseRepo.findByApartmentIdAndPurchaseDateBetween(1L, start, end))
                .thenReturn(List.of(purchase("100", "10")));

        List<BillingService.Draft> d = service.compute(cycle);

        // same area => unmetered flat is estimated at the metered flat's 15 kL => 900 split 450 / 450
        assertMoney("450.00", d.get(0).base());
        assertMoney("450.00", d.get(1).base());
    }

    @Test
    void compute_requiresATariff() {
        when(tariffs.findByApartmentId(1L)).thenReturn(Optional.empty());
        var ex = assertThrows(ApiException.class, () -> service.compute(cycle));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    // ================= open cycle =================
    @Test
    void open_rejectsEndBeforeStart() {
        var ex = assertThrows(ApiException.class,
                () -> service.open(1L, new CycleReq("Bad", LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 1), null)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void open_rejectsOverlappingCycle() {
        when(cycles.findByApartmentIdOrderByStartDateDesc(1L))
                .thenReturn(List.of(cycle(9L, apt, start, end, "0", CycleStatus.FINALIZED)));

        var ex = assertThrows(ApiException.class,
                () -> service.open(1L, new CycleReq("Overlap", LocalDate.of(2026, 9, 15), LocalDate.of(2026, 10, 15), null)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(cycles, never()).save(any());
    }

    @Test
    void open_createsOpenCycleAfterAnExistingOne_defaultingSharedAreaToZero() {
        when(cycles.findByApartmentIdOrderByStartDateDesc(1L))
                .thenReturn(List.of(cycle(9L, apt, start, end, "0", CycleStatus.FINALIZED)));
        when(apartments.getReferenceById(1L)).thenReturn(apt);
        when(cycles.save(any())).thenAnswer(i -> i.getArgument(0));

        BillingCycle c = service.open(1L, new CycleReq("  October 2026 ", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), null));

        assertEquals("October 2026", c.getLabel());
        assertEquals(CycleStatus.OPEN, c.getStatus());
        assertMoney("0", c.getSharedAreaKl());
    }

    // ================= finalize / archive =================
    @Test
    void finalize_createsInvoices_closesCycle_andNotifiesResidents() {
        stubCycleData(List.of(purchase("100", "10")));
        User resident = user(20L, "res@x.com", Role.RESIDENT, apt, h1);
        when(cycles.findByIdAndApartmentId(1L, 1L)).thenReturn(Optional.of(cycle));
        when(invoices.save(any())).thenAnswer(i -> i.getArgument(0));
        when(cycles.save(any())).thenAnswer(i -> i.getArgument(0));
        when(users.findByHouseholdId(1L)).thenReturn(List.of(resident));

        List<Invoice> out = service.finalizeCycle(1L, 1L);

        assertEquals(2, out.size());
        assertMoney("320.00", out.get(0).getTotal());
        assertEquals(CycleStatus.FINALIZED, cycle.getStatus());
        assertNotNull(cycle.getFinalizedAt());
        verify(notifications, times(1)).save(any(Notification.class));     // only h1 has a resident
        verify(events).email(eq("res@x.com"), contains("Water bill ready"), contains("INR 320.00"));
    }

    @Test
    void finalize_onlyWorksForOpenCycles() {
        cycle.setStatus(CycleStatus.FINALIZED);
        when(cycles.findByIdAndApartmentId(1L, 1L)).thenReturn(Optional.of(cycle));

        var ex = assertThrows(ApiException.class, () -> service.finalizeCycle(1L, 1L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(invoices, never()).save(any());
    }

    @Test
    void finalize_cycleOfAnotherApartmentIsNotFound() {
        when(cycles.findByIdAndApartmentId(1L, 2L)).thenReturn(Optional.empty());
        var ex = assertThrows(ApiException.class, () -> service.finalizeCycle(2L, 1L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void archive_movesFinalizedCycleToArchived() {
        cycle.setStatus(CycleStatus.FINALIZED);
        when(cycles.findByIdAndApartmentId(1L, 1L)).thenReturn(Optional.of(cycle));
        when(cycles.save(any())).thenAnswer(i -> i.getArgument(0));
        assertEquals(CycleStatus.ARCHIVED, service.archive(1L, 1L).getStatus());
    }

    @Test
    void archive_openCycleIsRejected() {
        when(cycles.findByIdAndApartmentId(1L, 1L)).thenReturn(Optional.of(cycle));
        var ex = assertThrows(ApiException.class, () -> service.archive(1L, 1L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    // ================= invoice adjustments =================
    @Test
    void adjust_recalculatesTotalFromBaseSharedAndAdjustment() {
        cycle.setStatus(CycleStatus.FINALIZED);
        Invoice inv = invoice(7L, cycle, h1, "270.00", "50.00");
        when(invoices.findById(7L)).thenReturn(Optional.of(inv));
        when(invoices.save(any())).thenAnswer(i -> i.getArgument(0));

        Invoice out = service.adjust(1L, 7L, new AdjustmentReq(bd("-25.00"), "Goodwill credit"));

        assertMoney("295.00", out.getTotal());
        assertEquals("Goodwill credit", out.getAdjustmentNote());
    }

    @Test
    void adjust_invoiceOfAnotherApartmentIsNotFound() {
        cycle.setStatus(CycleStatus.FINALIZED);
        when(invoices.findById(7L)).thenReturn(Optional.of(invoice(7L, cycle, h1, "270.00", "50.00")));
        var ex = assertThrows(ApiException.class, () -> service.adjust(2L, 7L, new AdjustmentReq(bd("5"), null)));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void adjust_notAllowedWhileCycleIsStillOpen() {
        when(invoices.findById(7L)).thenReturn(Optional.of(invoice(7L, cycle, h1, "270.00", "50.00")));
        var ex = assertThrows(ApiException.class, () -> service.adjust(1L, 7L, new AdjustmentReq(bd("5"), null)));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void markPaid_setsStatusPaid() {
        Invoice inv = invoice(7L, cycle, h1, "270.00", "50.00");
        when(invoices.findById(7L)).thenReturn(Optional.of(inv));
        when(invoices.save(any())).thenAnswer(i -> i.getArgument(0));
        assertEquals(InvoiceStatus.PAID, service.markPaid(1L, 7L).getStatus());
    }

    // ================= tariff =================
    @Test
    void tariff_missingIsABadRequest() {
        when(tariffs.findByApartmentId(1L)).thenReturn(Optional.empty());
        var ex = assertThrows(ApiException.class, () -> service.tariff(1L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void updateTariff_createsPlanWhenNoneExists() {
        when(tariffs.findByApartmentId(1L)).thenReturn(Optional.empty());
        when(apartments.getReferenceById(1L)).thenReturn(apt);
        when(tariffs.save(any())).thenAnswer(i -> i.getArgument(0));

        TariffPlan t = service.updateTariff(1L, new TariffDto(bd("8"), bd("10"), bd("18"), bd("0"),
                SharedMethod.OCCUPANCY, BillingMode.COST_SHARING, bd("12")));

        assertSame(apt, t.getApartment());
        assertMoney("18", t.getTier2Rate());
        assertEquals(SharedMethod.OCCUPANCY, t.getSharedMethod());
        assertEquals(BillingMode.COST_SHARING, t.getBillingMode());
        assertNotNull(t.getUpdatedAt());
    }

    @Test
    void updateTariff_updatesTheExistingPlan() {
        when(tariffs.findByApartmentId(1L)).thenReturn(Optional.of(tariff));
        when(tariffs.save(any())).thenAnswer(i -> i.getArgument(0));

        TariffPlan t = service.updateTariff(1L, new TariffDto(bd("20"), bd("9"), bd("15"), bd("25"),
                SharedMethod.AREA, BillingMode.TIERED, bd("30")));

        assertSame(tariff, t);
        assertMoney("20", tariff.getTier1LimitKl());
        assertMoney("30", tariff.getAlertThresholdKl());
    }

    // ================= bulk purchases =================
    @Test
    void addPurchase_savesTrimmedSourceForTheApartment() {
        when(apartments.getReferenceById(1L)).thenReturn(apt);
        when(purchaseRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        BulkPurchase p = service.addPurchase(1L, new PurchaseReq(LocalDate.of(2026, 9, 5), "  Tanker ", bd("80"), bd("14"), null));

        assertEquals("Tanker", p.getSource());
        assertSame(apt, p.getApartment());
        assertMoney("1120", p.totalCost());
    }

    @Test
    void deletePurchase_removesItOrReportsNotFound() {
        BulkPurchase p = purchase("10", "5");
        when(purchaseRepo.findByIdAndApartmentId(3L, 1L)).thenReturn(Optional.of(p));
        when(purchaseRepo.findByIdAndApartmentId(4L, 1L)).thenReturn(Optional.empty());

        service.deletePurchase(1L, 3L);
        verify(purchaseRepo).delete(p);

        var ex = assertThrows(ApiException.class, () -> service.deletePurchase(1L, 4L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
