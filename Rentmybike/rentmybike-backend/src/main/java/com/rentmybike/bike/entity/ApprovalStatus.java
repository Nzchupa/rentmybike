package com.rentmybike.bike.entity;

/**
 * Admin approval status for a bike listing.
 * Admin-Genehmigungsstatus für ein Fahrrad-Inserat.
 *
 * <p>Flow: PENDING (on create) → APPROVED (admin approves), REJECTED (admin
 * rejects with reason — final, owner must create a new listing in spirit but
 * may still edit and resubmit), or CHANGES_REQUESTED (admin asks for specific
 * fixes — a softer, non-final alternative to REJECTED; owner edits and the
 * bike automatically returns to PENDING for re-review).
 * <p>Ablauf: PENDING (beim Erstellen) → APPROVED (Admin genehmigt), REJECTED
 * (Admin lehnt mit Grund ab — endgültig) oder CHANGES_REQUESTED (Admin
 * fordert konkrete Korrekturen an — eine mildere, nicht endgültige
 * Alternative zu REJECTED; nach Bearbeitung kehrt das Fahrrad automatisch
 * zu PENDING zur erneuten Überprüfung zurück).
 *
 * <p>Only APPROVED bikes appear in public search results.
 * <p>Nur APPROVED-Fahrräder erscheinen in öffentlichen Suchergebnissen.
 *
 * <p>Maps to PostgreSQL ENUM type {@code approval_status} added in V3
 * migration, converted to a plain VARCHAR in V5 — so adding the
 * CHANGES_REQUESTED constant here requires no further migration.
 * <p>Entspricht dem PostgreSQL-ENUM-Typ {@code approval_status} aus der
 * V3-Migration, in V5 zu einem einfachen VARCHAR konvertiert — das
 * Hinzufügen der Konstante CHANGES_REQUESTED erfordert daher keine weitere
 * Migration.
 */
public enum ApprovalStatus {

    /** Waiting for admin review / Wartet auf Admin-Überprüfung */
    PENDING,

    /** Approved — visible in public search / Genehmigt — in öffentlicher Suche sichtbar */
    APPROVED,

    /** Rejected — owner sees rejection reason / Abgelehnt — Eigentümer sieht Ablehnungsgrund */
    REJECTED,

    /**
     * Admin asked the owner to fix specific issues before approval — not a
     * full rejection. Owner sees the feedback in {@code rejectionReason}.
     * Admin hat den Eigentümer aufgefordert, bestimmte Probleme vor der
     * Genehmigung zu beheben — keine vollständige Ablehnung. Der Eigentümer
     * sieht das Feedback in {@code rejectionReason}.
     */
    CHANGES_REQUESTED
}
