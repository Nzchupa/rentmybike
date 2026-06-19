package com.rentmybike.report.entity;

/**
 * Lifecycle status of a {@link Report} as it moves through admin triage.
 * Lebenszyklusstatus einer {@link Report} während der Admin-Triage.
 *
 * <p>PENDING -&gt; UNDER_REVIEW -&gt; RESOLVED | DISMISSED. An admin may also
 * jump straight from PENDING to RESOLVED/DISMISSED without an explicit
 * "review" step — UNDER_REVIEW exists so a report can be visibly claimed/
 * in-progress in a list of many.
 * <p>PENDING -&gt; UNDER_REVIEW -&gt; RESOLVED | DISMISSED. Ein Admin kann auch
 * direkt von PENDING zu RESOLVED/DISMISSED springen, ohne einen expliziten
 * "Prüfen"-Schritt — UNDER_REVIEW existiert, damit eine Meldung in einer
 * Liste vieler sichtbar übernommen/in Bearbeitung markiert werden kann.
 */
public enum ReportStatus {
    PENDING,
    UNDER_REVIEW,
    RESOLVED,
    DISMISSED
}
