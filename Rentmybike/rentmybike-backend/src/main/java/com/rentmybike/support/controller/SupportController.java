package com.rentmybike.support.controller;

import com.rentmybike.common.response.ApiResponse;
import com.rentmybike.common.response.PageResponse;
import com.rentmybike.support.dto.CreateSupportTicketRequest;
import com.rentmybike.support.dto.SendSupportMessageRequest;
import com.rentmybike.support.dto.SupportTicketResponse;
import com.rentmybike.support.dto.UpdateSupportTicketStatusRequest;
import com.rentmybike.support.entity.SupportCategory;
import com.rentmybike.support.entity.SupportTicketStatus;
import com.rentmybike.support.service.SupportService;
import com.rentmybike.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for the support ticket system — a user-facing help desk
 * and the admin triage/reply workflow behind it.
 * REST-Controller für das Support-Ticket-System — ein nutzerseitiges
 * Support-System und der dahinterliegende Admin-Triage-/Antwort-Workflow.
 *
 * <p>Endpoint groups:
 * <ul>
 *   <li>/api/v1/support/tickets/**       — any authenticated user, scoped to their own tickets</li>
 *   <li>/api/v1/admin/support/tickets/** — admin triage (hasRole('ADMIN') via SecurityConfig's
 *       blanket /api/v1/admin/** rule, same as ReportController's admin routes)</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
public class SupportController {

    private final SupportService supportService;

    // ──────────────────────────────────────────────────────────────────────────
    // User-facing / Benutzerseitig
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Opens a new support ticket with its first message.
     * Eröffnet ein neues Support-Ticket mit der ersten Nachricht.
     *
     * <p>POST /api/v1/support/tickets → 201 Created
     */
    @PostMapping("/api/v1/support/tickets")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> createTicket(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateSupportTicketRequest request) {

        SupportTicketResponse created = supportService.createTicket(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Ticket submitted — our team will reply soon / "
                        + "Ticket eingereicht — unser Team antwortet in Kürze"));
    }

    /**
     * The current user's own tickets, newest-activity first.
     * Die eigenen Tickets des aktuellen Benutzers, neueste Aktivität zuerst.
     *
     * <p>GET /api/v1/support/tickets?page=0&size=20
     */
    @GetMapping("/api/v1/support/tickets")
    public ResponseEntity<ApiResponse<PageResponse<SupportTicketResponse>>> listMyTickets(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(ApiResponse.success(supportService.listMyTickets(currentUser.getId(), pageable)));
    }

    /**
     * A single one of the current user's tickets, with its full thread.
     * Ein einzelnes Ticket des aktuellen Benutzers, mit vollständigem Verlauf.
     *
     * <p>GET /api/v1/support/tickets/{id}
     */
    @GetMapping("/api/v1/support/tickets/{id}")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> getMyTicket(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id) {

        return ResponseEntity.ok(ApiResponse.success(supportService.getMyTicket(currentUser.getId(), id)));
    }

    /**
     * Adds a reply from the current user to their own ticket.
     * Fügt eine Antwort des aktuellen Benutzers zu seinem eigenen Ticket hinzu.
     *
     * <p>POST /api/v1/support/tickets/{id}/messages
     */
    @PostMapping("/api/v1/support/tickets/{id}/messages")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> addMyMessage(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody SendSupportMessageRequest request) {

        SupportTicketResponse updated = supportService.addMyMessage(currentUser.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Admin / Admin
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Admin: paginated, filterable list of every support ticket.
     * Admin: paginierte, filterbare Liste aller Support-Tickets.
     *
     * <p>GET /api/v1/admin/support/tickets?status=OPEN&category=PAYMENT&search=paypal&page=0&size=20
     */
    @GetMapping("/api/v1/admin/support/tickets")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<SupportTicketResponse>>> adminListTickets(
            @RequestParam(required = false) SupportTicketStatus status,
            @RequestParam(required = false) SupportCategory category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(ApiResponse.success(
                supportService.adminList(status, category, search, pageable)));
    }

    /**
     * Admin: get a single ticket with its full thread.
     * Admin: ein einzelnes Ticket mit vollständigem Verlauf abrufen.
     *
     * <p>GET /api/v1/admin/support/tickets/{id}
     */
    @GetMapping("/api/v1/admin/support/tickets/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> adminGetTicket(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(supportService.adminGetTicket(id)));
    }

    /**
     * Admin: reply to a ticket.
     * Admin: auf ein Ticket antworten.
     *
     * <p>POST /api/v1/admin/support/tickets/{id}/messages
     */
    @PostMapping("/api/v1/admin/support/tickets/{id}/messages")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> adminAddMessage(
            @AuthenticationPrincipal User currentAdmin,
            @PathVariable UUID id,
            @Valid @RequestBody SendSupportMessageRequest request) {

        SupportTicketResponse updated = supportService.adminAddMessage(currentAdmin.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    /**
     * Admin: change a ticket's status (e.g. mark RESOLVED or CLOSED).
     * Admin: den Status eines Tickets ändern (z. B. als RESOLVED oder CLOSED markieren).
     *
     * <p>POST /api/v1/admin/support/tickets/{id}/status
     */
    @PostMapping("/api/v1/admin/support/tickets/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> adminUpdateStatus(
            @AuthenticationPrincipal User currentAdmin,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSupportTicketStatusRequest request) {

        SupportTicketResponse updated = supportService.adminUpdateStatus(currentAdmin.getId(), id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success(updated));
    }
}
