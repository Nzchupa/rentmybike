package com.rentmybike.report.entity;

/**
 * The kind of entity a {@link Report} is filed against.
 * Die Art der Entität, gegen die eine {@link Report} eingereicht wird.
 *
 * <p>Polymorphic reference — {@code Report.targetId} points at a row in the
 * bikes, users, or reviews table depending on this value, mirroring the
 * targetType/targetId pattern already used by {@code AuditLog}.
 * <p>Polymorphe Referenz — {@code Report.targetId} verweist je nach diesem
 * Wert auf eine Zeile in der Tabelle bikes, users oder reviews, analog zum
 * targetType/targetId-Muster, das bereits von {@code AuditLog} verwendet wird.
 */
public enum ReportTargetType {
    BIKE,
    USER,
    REVIEW
}
