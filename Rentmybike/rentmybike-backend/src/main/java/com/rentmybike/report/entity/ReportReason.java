package com.rentmybike.report.entity;

/**
 * The category of complaint a reporter selects when filing a {@link Report}.
 * Die Beschwerdekategorie, die ein Meldender beim Einreichen einer
 * {@link Report} auswählt.
 */
public enum ReportReason {
    SPAM,
    INAPPROPRIATE_CONTENT,
    FRAUD_OR_SCAM,
    HARASSMENT,
    FAKE_LISTING,
    SAFETY_CONCERN,
    OTHER
}
