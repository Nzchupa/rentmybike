package com.rentmybike.bike.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Per-bike statistics panel — view count plus a breakdown of bookings by
 * status and total completed-booking revenue, for a single bike.
 * Pro-Fahrrad-Statistikpanel — Aufrufzähler sowie eine Aufschlüsselung der
 * Buchungen nach Status und der Gesamtumsatz aus abgeschlossenen Buchungen,
 * für ein einzelnes Fahrrad.
 *
 * <p>Used by the owner's "My Bikes" detail stats and the admin bike detail
 * view. Not paginated — these are scalar aggregates for one bike.
 * <p>Wird von den Eigentümer-Statistiken in "Meine Fahrräder" und der
 * Admin-Fahrrad-Detailansicht verwendet. Nicht paginiert — dies sind
 * skalare Aggregate für ein einzelnes Fahrrad.
 */
@Data
@Builder
public class BikeStatsResponse {

    private UUID bikeId;

    /** Public detail-page view count — see Bike.viewCount / Aufrufzähler der öffentlichen Detailseite — siehe Bike.viewCount */
    private long viewCount;

    private long totalBookings;
    private long pendingBookings;
    private long acceptedBookings;
    private long completedBookings;
    private long cancelledBookings;
    private long rejectedBookings;

    /** Gross revenue from COMPLETED bookings only / Bruttoumsatz nur aus COMPLETED-Buchungen */
    private BigDecimal totalRevenue;
}
