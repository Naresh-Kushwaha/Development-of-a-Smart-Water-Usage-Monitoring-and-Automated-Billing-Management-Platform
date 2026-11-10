package com.smartwater.events;

/** New meter readings were saved for this apartment - triggers overuse/leak evaluation. */
public record UsageIngested(Long apartmentId) {}
