package com.smartwater.domain;

public final class Enums {
    private Enums() {}
    public enum Role { ADMIN, RESIDENT }
    public enum CycleStatus { OPEN, FINALIZED, ARCHIVED }
    public enum SharedMethod { AREA, OCCUPANCY }
    public enum BillingMode { TIERED, COST_SHARING }
    public enum InvoiceStatus { UNPAID, PAID }
    public enum NotificationType { OVERUSE, LEAK, BILL }
}
