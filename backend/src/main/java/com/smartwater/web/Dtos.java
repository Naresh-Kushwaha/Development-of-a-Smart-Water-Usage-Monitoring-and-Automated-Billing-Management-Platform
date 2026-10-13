package com.smartwater.web;

import com.smartwater.domain.Enums.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class Dtos {
    private Dtos() {}

    // ---- auth ----
    public record RegisterAdminReq(@NotBlank @Email String email, @NotBlank @Size(min = 8) String password,
                                   @NotBlank String fullName, @NotBlank String apartmentName, String address) {}
    public record RegisterResidentReq(@NotBlank @Email String email, @NotBlank @Size(min = 8) String password,
                                      @NotBlank String fullName, @NotBlank String apartmentCode, @NotBlank String unitNumber) {}
    public record LoginReq(@NotBlank String email, @NotBlank String password) {}
    public record ProfileReq(@NotBlank String fullName, String newPassword) {}
    public record UserRes(Long id, String email, String fullName, String role, String apartmentName,
                          String apartmentCode, Long householdId, String unitNumber) {}
    public record AuthRes(String token, UserRes user) {}

    // ---- households ----
    public record HouseholdReq(@NotBlank String unitNumber, @NotNull @Positive BigDecimal areaSqft,
                               @Min(1) int occupants, String meterNumber) {}
    public record HouseholdRes(Long id, String unitNumber, BigDecimal areaSqft, int occupants, String meterNumber,
                               boolean metered, List<String> residents) {}
    public record AssignReq(@NotBlank @Email String email) {}

    // ---- usage ----
    public record UsageReq(Long householdId, @NotNull LocalDate date, @NotNull @PositiveOrZero BigDecimal litres) {}
    public record UsageRes(LocalDate date, BigDecimal litres, String source) {}
    public record CsvResult(int imported, int duplicates, List<String> errors) {}
    public record UnitUsage(String unitNumber, BigDecimal kl, BigDecimal areaSqft, int occupants, BigDecimal litresPerPersonPerDay) {}

    // ---- tariff / purchases / cycles ----
    public record TariffDto(@NotNull @Positive BigDecimal tier1LimitKl, @NotNull @PositiveOrZero BigDecimal tier1Rate,
                            @NotNull @PositiveOrZero BigDecimal tier2Rate, @NotNull @PositiveOrZero BigDecimal fixedCharge,
                            @NotNull SharedMethod sharedMethod, @NotNull BillingMode billingMode,
                            @NotNull @Positive BigDecimal alertThresholdKl) {}
    public record PurchaseReq(@NotNull LocalDate date, @NotBlank String source, @NotNull @Positive BigDecimal volumeKl,
                              @NotNull @PositiveOrZero BigDecimal unitCost, String notes) {}
    public record PurchaseRes(Long id, LocalDate date, String source, BigDecimal volumeKl, BigDecimal unitCost,
                              BigDecimal totalCost, String notes) {}
    public record CycleReq(@NotBlank String label, @NotNull LocalDate startDate, @NotNull LocalDate endDate,
                           @PositiveOrZero BigDecimal sharedAreaKl) {}
    public record CycleRes(Long id, String label, LocalDate startDate, LocalDate endDate, CycleStatus status,
                           BigDecimal sharedAreaKl) {}
    public record AdjustmentReq(@NotNull BigDecimal amount, String note) {}
    public record InvoiceRes(Long id, String cycleLabel, LocalDate startDate, LocalDate endDate, String unitNumber,
                             BigDecimal consumptionKl, BigDecimal baseCharge, BigDecimal sharedCharge,
                             BigDecimal adjustment, String adjustmentNote, BigDecimal total, InvoiceStatus status) {}

    // ---- dashboard ----
    public record DailyPoint(LocalDate date, BigDecimal litres) {}
    public record MonthPoint(String month, BigDecimal kl) {}
    public record CycleSummary(String label, LocalDate startDate, LocalDate endDate, BigDecimal consumptionKl,
                               BigDecimal estimatedBase, BigDecimal estimatedShared, BigDecimal estimatedTotal) {}
    public record DashboardRes(String unitNumber, List<DailyPoint> daily, List<MonthPoint> monthly, CycleSummary cycle,
                               BigDecimal monthToDateKl, BigDecimal thresholdKl, List<InvoiceRes> invoices,
                               List<String> tips, long unread) {}
    public record CompareRes(BigDecimal myAvgDaily, BigDecimal apartmentAvgDaily, BigDecimal similarAvgDaily,
                             int similarCount, BigDecimal myPerPerson, BigDecimal apartmentPerPerson,
                             int betterThanPercent) {}
    public record NotificationRes(Long id, NotificationType type, String message, boolean read, Instant createdAt) {}
}
