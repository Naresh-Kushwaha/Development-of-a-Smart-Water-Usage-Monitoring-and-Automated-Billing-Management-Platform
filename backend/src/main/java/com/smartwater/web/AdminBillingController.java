package com.smartwater.web;

import com.smartwater.repo.*;
import com.smartwater.security.Current;
import com.smartwater.service.AlertService;
import com.smartwater.service.BillingService;
import com.smartwater.web.Dtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminBillingController {
    private final BillingService billing;
    private final AlertService alerts;
    private final PurchaseRepository purchases;
    private final BillingCycleRepository cycles;
    private final InvoiceRepository invoices;

    @GetMapping("/tariff")
    public TariffDto tariff() { return Mapper.tariff(billing.tariff(Current.apartmentId())); }

    @PutMapping("/tariff")
    public TariffDto updateTariff(@Valid @RequestBody TariffDto d) {
        return Mapper.tariff(billing.updateTariff(Current.apartmentId(), d));
    }

    @GetMapping("/purchases")
    public List<PurchaseRes> purchases() {
        return purchases.findByApartmentIdOrderByPurchaseDateDesc(Current.apartmentId()).stream().map(Mapper::purchase).toList();
    }

    @PostMapping("/purchases")
    public PurchaseRes addPurchase(@Valid @RequestBody PurchaseReq r) {
        return Mapper.purchase(billing.addPurchase(Current.apartmentId(), r));
    }

    @DeleteMapping("/purchases/{id}")
    public void deletePurchase(@PathVariable Long id) { billing.deletePurchase(Current.apartmentId(), id); }

    @GetMapping("/cycles")
    public List<CycleRes> cycles() {
        return cycles.findByApartmentIdOrderByStartDateDesc(Current.apartmentId()).stream().map(Mapper::cycle).toList();
    }

    @PostMapping("/cycles")
    public CycleRes open(@Valid @RequestBody CycleReq r) { return Mapper.cycle(billing.open(Current.apartmentId(), r)); }

    @PostMapping("/cycles/{id}/finalize")
    public List<InvoiceRes> finalizeCycle(@PathVariable Long id) {
        return billing.finalizeCycle(Current.apartmentId(), id).stream().map(Mapper::invoice).toList();
    }

    @PostMapping("/cycles/{id}/archive")
    public CycleRes archive(@PathVariable Long id) { return Mapper.cycle(billing.archive(Current.apartmentId(), id)); }

    @GetMapping("/cycles/{id}/invoices")
    public List<InvoiceRes> cycleInvoices(@PathVariable Long id) {
        var c = billing.cycle(Current.apartmentId(), id);
        if (c.getStatus() == com.smartwater.domain.Enums.CycleStatus.OPEN) {
            return billing.compute(c).stream().map(d -> new InvoiceRes(null, c.getLabel(), c.getStartDate(), c.getEndDate(),
                    d.household().getUnitNumber(), d.consumptionKl(), d.base(), d.shared(), java.math.BigDecimal.ZERO, null,
                    d.total(), com.smartwater.domain.Enums.InvoiceStatus.UNPAID)).toList();
        }
        return invoices.findByBillingCycleIdOrderByHouseholdUnitNumber(c.getId()).stream().map(Mapper::invoice).toList();
    }

    @PutMapping("/invoices/{id}/adjustment")
    public InvoiceRes adjust(@PathVariable Long id, @Valid @RequestBody AdjustmentReq r) {
        return Mapper.invoice(billing.adjust(Current.apartmentId(), id, r));
    }

    @PutMapping("/invoices/{id}/paid")
    public InvoiceRes paid(@PathVariable Long id) { return Mapper.invoice(billing.markPaid(Current.apartmentId(), id)); }

    @PostMapping("/alerts/run")
    public AlertService.Report runAlerts() { return alerts.check(Current.apartmentId()); }
}
