package com.rentmybike.business.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Richer analytics payload for a business account — daily booking/revenue
 * series, top-performing bikes, average rental duration, and a
 * views-to-bookings conversion rate. This is a separate endpoint from
 * {@code BusinessDashboardSummaryResponse}, which intentionally stays
 * "no charts, just totals" per the original Stage 3 product decision —
 * this response is the new, richer layer built on top of it.
 * Umfangreicheres Analytics-Payload für ein Geschäftskonto — tägliche
 * Buchungs-/Umsatzreihe, leistungsstärkste Fahrräder, durchschnittliche
 * Mietdauer und eine Konversionsrate von Aufrufen zu Buchungen. Dies ist
 * ein separater Endpunkt von {@code BusinessDashboardSummaryResponse},
 * welcher bewusst bei "keine Diagramme, nur Summen" bleibt — diese
 * Antwort ist die neue, umfangreichere Schicht darüber.
 */
@Data
@Builder
public class BusinessAnalyticsResponse {

    /** Number of trailing days requested (e.g. 7, 30, 90) / Angeforderte Anzahl zurückliegender Tage */
    private int rangeDays;

    /** One zero-filled point per day in the range, oldest first / Ein nullaufgefüllter Punkt pro Tag im Zeitraum, älteste zuerst */
    private List<BusinessTimeSeriesPoint> series;

    /** Top bikes by booking count, capped to a small fixed list / Top-Fahrräder nach Buchungsanzahl, begrenzt auf eine kleine feste Liste */
    private List<PopularBikeResponse> popularBikes;

    /** Average length of COMPLETED bookings in days, 0 if none / Durchschnittliche Länge von COMPLETED-Buchungen in Tagen, 0 falls keine */
    private double averageBookingDurationDays;

    /**
     * (totalBookings / totalViews) * 100 across this owner's bikes, 0 if no
     * views recorded yet. Not an industry-standard metric — a deliberate
     * pairing of the bike-view-count feature with this analytics layer.
     * (Gesamtbuchungen / Gesamtaufrufe) * 100 über die Fahrräder dieses
     * Eigentümers, 0 falls noch keine Aufrufe erfasst wurden.
     */
    private double conversionRate;
}
