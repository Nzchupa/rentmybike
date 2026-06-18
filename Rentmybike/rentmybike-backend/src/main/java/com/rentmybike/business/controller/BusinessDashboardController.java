package com.rentmybike.business.controller;

import com.rentmybike.booking.dto.BookingResponse;
import com.rentmybike.booking.service.BookingService;
import com.rentmybike.business.dto.BusinessDashboardSummaryResponse;
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
