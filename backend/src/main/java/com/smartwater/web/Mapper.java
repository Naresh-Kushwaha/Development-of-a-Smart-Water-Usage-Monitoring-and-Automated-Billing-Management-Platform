package com.smartwater.web;

import com.smartwater.domain.*;
import com.smartwater.web.Dtos.*;

public final class Mapper {
    private Mapper() {}

    public static UserRes user(User u) {
        Household h = u.getHousehold();
        return new UserRes(u.getId(), u.getEmail(), u.getFullName(), u.getRole().name(), u.getApartment().getName(),
                u.getApartment().getCode(), h == null ? null : h.getId(), h == null ? null : h.getUnitNumber());
    }

    public static InvoiceRes invoice(Invoice i) {
        BillingCycle c = i.getBillingCycle();
        return new InvoiceRes(i.getId(), c.getLabel(), c.getStartDate(), c.getEndDate(), i.getHousehold().getUnitNumber(),
                i.getConsumptionKl(), i.getBaseCharge(), i.getSharedCharge(), i.getAdjustment(), i.getAdjustmentNote(),
                i.getTotal(), i.getStatus());
    }

    public static CycleRes cycle(BillingCycle c) {
        return new CycleRes(c.getId(), c.getLabel(), c.getStartDate(), c.getEndDate(), c.getStatus(), c.getSharedAreaKl());
    }

    public static TariffDto tariff(TariffPlan t) {
        return new TariffDto(t.getTier1LimitKl(), t.getTier1Rate(), t.getTier2Rate(), t.getFixedCharge(),
                t.getSharedMethod(), t.getBillingMode(), t.getAlertThresholdKl());
    }

    public static PurchaseRes purchase(BulkPurchase p) {
        return new PurchaseRes(p.getId(), p.getPurchaseDate(), p.getSource(), p.getVolumeKl(), p.getUnitCost(),
                p.totalCost().setScale(2, java.math.RoundingMode.HALF_UP), p.getNotes());
    }

    public static NotificationRes notification(Notification n) {
        return new NotificationRes(n.getId(), n.getType(), n.getMessage(), n.isSeen(), n.getCreatedAt());
    }
}
