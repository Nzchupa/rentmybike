package com.rentmybike.bike.entity;

/**
 * Admin approval status for a bike listing.
 * Admin-Genehmigungsstatus für ein Fahrrad-Inserat.
 *
 * <p>Flow: PENDING (on create) → APPROVED (admin approves) or REJECTED (admin rejects with reason).
 * <p>Ablauf: PENDING (beim Erstellen) → APPROVED (Admin genehmigt) oder REJECTED (Admin lehnt ab mit Grund).
 *
 * <p>Only APPROVED bikes appear in public search results.
 * <p>Nur APPROVED-Fahrräder erscheinen in öffentlichen Suchergebnissen.
 *
 * <p>Maps to PostgreSQL ENUM type {@code approval_status} added in V3 migration.
 * <p>Entspricht dem PostgreSQL-ENUM-Typ {@code approval_status} aus der V3-Migration.
 */
public enum ApprovalStatus {

    /** Waiting for admin review / Wartet auf Admin-Überprüfung */
    PENDING,

    /** Approved — visible in public search / Genehmigt — in öffentlicher Suche sichtbar */
    APPROVED,

    /** Rejected — owner sees rejection reason / Abgelehnt — Eigentümer sieht Ablehnungsgrund */
    REJECTED
}
