package com.rentmybike.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One day's worth of platform-activity counters for the admin dashboard's
 * time-series charts.
 * Ein Tag an Plattform-Aktivitätszählern für die Zeitreihen-Diagramme des
 * Admin-Dashboards.
 *
 * <p>Days with zero activity are still included (zero-filled by {@code
 * AdminService.getAnalytics}) so the frontend can render a continuous chart
 * axis without having to infer missing dates itself.
 * <p>Tage ohne Aktivität sind trotzdem enthalten (von {@code
 * AdminService.getAnalytics} mit Null aufgefüllt), damit das Frontend eine
 * durchgehende Diagrammachse rendern kann, ohne fehlende Daten selbst
 * ableiten zu müssen.
 */
@Data
@Builder
public class AdminTimeSeriesPoint {

    /** Calendar day (UTC-naive, matches createdAt's LocalDateTime) / Kalendertag */
    private LocalDate date;

    /** New user signups that day / Neue Benutzeranmeldungen an diesem Tag */
    private long newUsers;

    /** New bike listings submitted that day / Neue Fahrrad-Inserate an diesem Tag */
    private long newBikes;

    /** New bookings created that day (any status) / Neue Buchungen an diesem Tag (jeder Status) */
    private long newBookings;

    /** Revenue from bookings created that day and since COMPLETED / Umsatz aus an diesem Tag erstellten und inzwischen COMPLETED-Buchungen */
    private BigDecimal revenue;
}
