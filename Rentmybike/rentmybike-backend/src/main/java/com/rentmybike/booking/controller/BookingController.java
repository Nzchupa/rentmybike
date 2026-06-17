package com.rentmybike.booking.controller;

import com.rentmybike.booking.dto.BookedDateRangeResponse;
import com.rentmybike.booking.dto.BookingResponse;
import com.rentmybike.booking.dto.CreateBookingRequest;
import com.rentmybike.booking.entity.BookingStatus;
import com.rentmybike.booking.service.BookingService;
import com.rentmybike.common.response.ApiResponse;
import com.rentmybike.common.response.PageResponse;
import com.rentmybike.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for booking lifecycle endpoints.
 * REST-Controller für Buchungslebenszyklus-Endpunkte.
 *
 * <p>Endpoint groups / Endpunktgruppen:
 * <ul>
 *   <li>POST   /api/v1/bookings              — renter creates booking</li>
 *   <li>GET    /api/v1/bookings/{id}          — participant views booking</li>
 *   <li>GET    /api/v1/bookings/my/renter     — renter's booking history</li>
 *   <li>GET    /api/v1/bookings/my/owner      — owner's incoming requests</li>
 *   <li>POST   /api/v1/bookings/{id}/accept   — owner accepts</li>
 *   <li>POST   /api/v1/bookings/{id}/reject   — owner rejects</li>
 *   <li>POST   /api/v1/bookings/{id}/cancel   — renter cancels</li>
 *   <li>POST   /api/v1/admin/bookings/{id}/complete — admin completes</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // ══════════════════════════════════════════════════════════════════════════
    // RENTER ENDPOINTS / MIETER-ENDPUNKTE
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Renter submits a new booking request.
     * Mieter reicht eine neue Buchungsanfrage ein.
     *
     * <p>POST /api/v1/bookings → 201 Created
     */
    @PostMapping("/api/v1/bookings")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateBookingRequest request) {

        BookingResponse created = bookingService.createBooking(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created,
                        "Booking request sent — waiting for owner approval / Buchungsanfrage gesendet — wartet auf Eigentümergenehmigung"));
    }

    /**
     * Get a single booking by ID (accessible by renter, owner, or admin).
     * Einzelne Buchung nach ID abrufen (zugänglich für Mieter, Eigentümer oder Admin).
     *
     * <p>GET /api/v1/bookings/{id}
     */
    @GetMapping("/api/v1/bookings/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {

        BookingResponse booking = bookingService.getBooking(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(booking));
    }

    /**
     * Renter's booking history — all rentals they've made.
     * Buchungshistorie des Mieters — alle seine Mieten.
     *
     * <p>GET /api/v1/bookings/my/renter?page=0&size=20
     */
    @GetMapping("/api/v1/bookings/my/renter")
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> getRenterBookings(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 50),
                Sort.by("createdAt").descending());

        PageResponse<BookingResponse> result = bookingService.getRenterBookings(
                currentUser.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Renter cancels their booking (PENDING or ACCEPTED only).
     * Mieter storniert seine Buchung (nur PENDING oder ACCEPTED).
     *
     * <p>POST /api/v1/bookings/{id}/cancel
     */
    @PostMapping("/api/v1/bookings/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {

        BookingResponse cancelled = bookingService.cancelBookingAsRenter(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(cancelled,
                "Booking cancelled / Buchung storniert"));
    }

    /**
     * Occupied date ranges for a bike — public, used by the booking calendar
     * to disable already-taken dates and validate overlap before submitting.
     * Belegte Datumsbereiche für ein Fahrrad — öffentlich, wird vom Buchungs-
     * kalender verwendet, um bereits vergebene Termine zu deaktivieren und
     * Überschneidungen vor dem Senden zu validieren.
     *
     * <p>GET /api/v1/bookings/bike/{bikeId}/booked-dates
     */
    @GetMapping("/api/v1/bookings/bike/{bikeId}/booked-dates")
    public ResponseEntity<ApiResponse<List<BookedDateRangeResponse>>> getBookedDates(
            @PathVariable UUID bikeId) {

        List<BookedDateRangeResponse> ranges = bookingService.getBookedDateRanges(bikeId);
        return ResponseEntity.ok(ApiResponse.success(ranges));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // OWNER ENDPOINTS / EIGENTÜMER-ENDPUNKTE
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Owner's incoming booking requests, filtered by status.
     * Eingehende Buchungsanfragen des Eigentümers, nach Status gefiltert.
     *
     * <p>GET /api/v1/bookings/my/owner?status=PENDING&page=0&size=20
     */
    @GetMapping("/api/v1/bookings/my/owner")
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> getOwnerBookings(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 50),
                Sort.by("createdAt").descending());

        PageResponse<BookingResponse> result = bookingService.getOwnerBookings(
                currentUser.getId(), status, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Owner accepts a PENDING booking request.
     * Eigentümer akzeptiert eine PENDING-Buchungsanfrage.
     *
     * <p>POST /api/v1/bookings/{id}/accept
     */
    @PostMapping("/api/v1/bookings/{id}/accept")
    public ResponseEntity<ApiResponse<BookingResponse>> acceptBooking(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {

        BookingResponse accepted = bookingService.acceptBooking(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(accepted,
                "Booking accepted / Buchung akzeptiert"));
    }

    /**
     * Owner rejects a PENDING booking request.
     * Eigentümer lehnt eine PENDING-Buchungsanfrage ab.
     *
     * <p>POST /api/v1/bookings/{id}/reject
     */
    @PostMapping("/api/v1/bookings/{id}/reject")
    public ResponseEntity<ApiResponse<BookingResponse>> rejectBooking(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {

        BookingResponse rejected = bookingService.rejectBooking(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(rejected,
                "Booking rejected / Buchung abgelehnt"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ADMIN ENDPOINTS / ADMIN-ENDPUNKTE
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Admin marks an ACCEPTED booking as COMPLETED after the rental period ends.
     * Admin markiert eine ACCEPTED-Buchung als COMPLETED nach Ende der Mietzeit.
     *
     * <p>POST /api/v1/admin/bookings/{id}/complete
     * Unlocks review flow for renter and owner.
     */
    @PostMapping("/api/v1/admin/bookings/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BookingResponse>> completeBooking(
            @PathVariable UUID id) {

        BookingResponse completed = bookingService.completeBooking(id);
        return ResponseEntity.ok(ApiResponse.success(completed,
                "Booking completed — reviews now available / Buchung abgeschlossen — Bewertungen jetzt verfügbar"));
    }
}
