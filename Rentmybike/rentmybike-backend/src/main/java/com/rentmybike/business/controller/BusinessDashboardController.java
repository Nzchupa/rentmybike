package com.rentmybike.business.controller;

import com.rentmybike.booking.dto.BookingResponse;
import com.rentmybike.booking.service.BookingService;
import com.rentmybike.business.dto.BusinessAnalyticsResponse;
import com.rentmybike.business.dto.BusinessDashboardSummaryResponse;
import com.rentmybike.business.dto.BusinessOverviewExtrasResponse;
import com.rentmybike.business.service.BusinessDashboardService;
import com.rentmybike.common.response.ApiResponse;
import com.rentmybike.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Business dashboard endpoints — base path /api/v1/business/dashboard.
 * Business-Dashboard-Endpunkte — Basis-Pfad /api/v1/business/dashboard.
 *
 * <p>Stage 3 "Business accounts". Restricted to BUSINESS role accounts — the
 * upgrade itself happens via {@link BusinessController#upgrade}.
 * <p>Stage 3 "Business-Konten". Beschränkt auf Konten mit BUSINESS-Rolle —
 * das Upgrade selbst erfolgt über {@link BusinessController#upgrade}.
 */
@RestController
@RequestMapping("/api/v1/business")
@RequiredArgsConstructor
@PreAuthorize("hasRole('BUSINESS')")
public class BusinessDashboardController {

    private final BusinessDashboardService businessDashboardService;
    private final BookingService bookingService;

    /**
     * Simple aggregate dashboard numbers — total revenue, active bike count,
     * total bookings, average rating. No charts (per product decision).
     * Einfache aggregierte Dashboard-Zahlen — Gesamtumsatz, aktive
     * Fahrradanzahl, Gesamtbuchungen, Durchschnittsbewertung. Keine Diagramme.
     *
     * <p>GET /api/v1/business/dashboard/summary
     */
    @GetMapping("/dashboard/summary")
    public ResponseEntity<ApiResponse<BusinessDashboardSummaryResponse>> getDashboardSummary(
            @AuthenticationPrincipal User currentUser) {

        BusinessDashboardSummaryResponse summary =
                businessDashboardService.getDashboardSummary(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * Richer analytics — daily booking/revenue chart, popular bikes, average
     * rental duration, and views-to-bookings conversion rate.
     * Umfangreichere Analytik — tägliches Buchungs-/Umsatzdiagramm, beliebte
     * Fahrräder, durchschnittliche Mietdauer und Konversionsrate von
     * Aufrufen zu Buchungen.
     *
     * <p>GET /api/v1/business/dashboard/analytics?days=30
     *
     * @param days trailing window in days, clamped to [1, 365], defaults to 30 / zurückliegendes Fenster in Tagen, begrenzt auf [1, 365], Standard 30
     */
    @GetMapping("/dashboard/analytics")
    public ResponseEntity<ApiResponse<BusinessAnalyticsResponse>> getAnalytics(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "30") int days) {

        int clamped = Math.max(1, Math.min(days, 365));
        BusinessAnalyticsResponse analytics =
                businessDashboardService.getAnalytics(currentUser.getId(), clamped);
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }

    /**
     * Supplementary "at a glance" lists for the overview page: bookings
     * awaiting accept/reject, the soonest upcoming confirmed bookings, and
     * the most recently received reviews.
     * Ergänzende "auf einen Blick"-Listen für die Übersichtsseite: Buchungen,
     * die auf Annahme/Ablehnung warten, die nächsten anstehenden
     * bestätigten Buchungen und die zuletzt erhaltenen Bewertungen.
     *
     * <p>GET /api/v1/business/dashboard/overview-extras
     */
    @GetMapping("/dashboard/overview-extras")
    public ResponseEntity<ApiResponse<BusinessOverviewExtrasResponse>> getOverviewExtras(
            @AuthenticationPrincipal User currentUser) {

        BusinessOverviewExtrasResponse extras =
                businessDashboardService.getOverviewExtras(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(extras));
    }

    /**
     * Bookings for this business's bikes overlapping the given date range —
     * backs the rental calendar view.
     * Buchungen für die Fahrräder dieses Unternehmens, die sich mit dem
     * angegebenen Datumsbereich überschneiden — Grundlage für die
     * Mietkalender-Ansicht.
     *
     * <p>GET /api/v1/business/bookings/calendar?from=2026-06-01&to=2026-06-30
     */
    @GetMapping("/bookings/calendar")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookingCalendar(
            @AuthenticationPrincipal User currentUser,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<BookingResponse> bookings =
                bookingService.getOwnerBookingCalendar(currentUser.getId(), from, to);
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }
}
