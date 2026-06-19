package com.rentmybike.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Aggregate platform statistics for the admin dashboard.
 * Aggregierte Plattformstatistiken für das Admin-Dashboard.
 *
 * <p>All counts are live — fetched from DB on each request.
 * <p>Alle Zählungen sind live — werden bei jeder Anfrage aus der DB geladen.
 */
@Data
@Builder
public class AdminStatsResponse {

    // ──────────────────────────────────────────────────────────────────────────
    // User stats / Benutzerstatistiken
    // ──────────────────────────────────────────────────────────────────────────

    /** Total non-deleted users / Gesamtanzahl nicht gelöschter Benutzer */
    private long totalUsers;

    /** Currently banned users / Aktuell gesperrte Benutzer */
    private long bannedUsers;

    /** Total admin accounts / Gesamtanzahl Admin-Konten */
    private long totalAdmins;

    // ──────────────────────────────────────────────────────────────────────────
    // Bike stats / Fahrrad-Statistiken
    // ──────────────────────────────────────────────────────────────────────────

    /** All non-deleted bikes / Alle nicht gelöschten Fahrräder */
    private long totalBikes;

    /** Awaiting admin review / Warten auf Admin-Überprüfung */
    private long pendingBikes;

    /** Live on platform / Live auf der Plattform */
    private long approvedBikes;

    /** Rejected bikes / Abgelehnte Fahrräder */
    private long rejectedBikes;

    /** Sent back to owner for edits, not a full rejection / An Eigentümer zur Bearbeitung zurückgesendet, keine vollständige Ablehnung */
    private long changesRequestedBikes;

    // ──────────────────────────────────────────────────────────────────────────
    // Booking stats / Buchungsstatistiken
    // ──────────────────────────────────────────────────────────────────────────

    /** All non-deleted bookings / Alle nicht gelöschten Buchungen */
    private long totalBookings;

    /** Waiting for owner response / Warten auf Eigentümerantwort */
    private long pendingBookings;

    /** Owner accepted, awaiting rental / Eigentümer akzeptiert, Miete ausstehend */
    private long acceptedBookings;

    /** Successfully completed / Erfolgreich abgeschlossen */
    private long completedBookings;

    /** Cancelled or expired / Storniert oder abgelaufen */
    private long cancelledBookings;

    /** Owner rejected / Vom Eigentümer abgelehnt */
    private long rejectedBookings;

    // ──────────────────────────────────────────────────────────────────────────
    // Revenue / Einnahmen
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Total transaction volume from COMPLETED bookings (sum of total_price).
     * Gesamtes Transaktionsvolumen aus COMPLETED-Buchungen (Summe von total_price).
     *
     * <p>Platform commission is calculated separately; this is gross GMV.
     * <p>Plattformgebühr wird separat berechnet; dies ist der Brutto-GMV.
     */
    private BigDecimal totalRevenue;
}
