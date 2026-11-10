package com.smartwater.events;

/** "Please send this email" - produced by billing/alert code, consumed by the email worker. */
public record EmailRequested(String to, String subject, String body) {}
