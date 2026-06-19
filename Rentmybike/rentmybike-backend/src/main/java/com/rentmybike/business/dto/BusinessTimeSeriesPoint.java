package com.rentmybike.business.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One day's worth of booking/revenue counters for a business's analytics
 * chart. Mirrors {@code AdminTimeSeriesPoint}, scoped to a single owner.
 * Ein Tag an Buchungs-/Umsatzzählern für das Analytics-Diagramm eines
 * Unternehmens. Entspricht {@code AdminTimeSeriesPoint}, beschränkt auf
 * einen einzelnen Eigentümer.
 *
 * <p>Days with zero activity are still included (zero-filled by {@code
 * BusinessDashboardService.getAnalytics}) so the frontend can render a
 * continuous chart axis without having to infer missing dates itself.
 * <p>Tage ohne Aktivität sind trotzdem enthalten (von {@code
 * BusinessDashboardService.getAnalytics} mit Null aufgefüllt), damit das
 * Frontend eine durchgehende Diagrammachse rendern kann, ohne fehlende
 * Daten selbst ableiten zu müssen.
 */
@Data
@Builder
public class BusinessTimeSeriesPoint {

    /** Calendar day (UTC-naive, matches createdAt's LocalDateTime) / Kalendertag */
    private LocalDate date;

    /** New bookings created that day for this owner's bikes (any status) / Neue Buchungen an diesem Tag für die Fahrräder dieses Eigentümers (jeder Status) */
    private long newBookings;

    /** Revenue from this owner's bookings COMPLETED that day / Umsatz aus an diesem Tag COMPLETED-Buchungen dieses Eigentümers */
    private BigDecimal revenue;
}
