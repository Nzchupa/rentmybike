package com.rentmybike.report.controller;

import com.rentmybike.common.response.ApiResponse;
import com.rentmybike.common.response.PageResponse;
import com.rentmybike.report.dto.CreateReportRequest;
import com.rentmybike.report.dto.ReportResponse;
import com.rentmybike.report.dto.ResolveReportRequest;
import com.rentmybike.report.entity.ReportStatus;
import com.rentmybike.report.entity.ReportTargetType;
import com.rentmybike.report.service.ReportService;
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
 * REST controller for the reports system — user-filed complaints about
 * bikes/users/reviews, and the admin moderation workflow that handles them.
 * REST-Controller für das Meldungssystem — von Benutzern eingereichte
 * Beschwerden über Fahrräder/Benutzer/Bewertungen sowie der Admin-
 * Moderations-Workflow, der sie bearbeitet.
 *
 * <p>Endpoint groups:
 * <ul>
 *   <li>POST /api/v1/reports                — any authenticated user files a report</li>
 *   <li>/api/v1/admin/reports/**            — admin triage (hasRole('ADMIN') via SecurityConfig)</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // ──────────────────────────────────────────────────────────────────────────
    // User-facing: file a report / Benutzerseitig: Meldung einreichen
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * File a new report against a bike, user, or review.
     * Eine neue Meldung gegen ein Fahrrad, einen Benutzer oder eine Bewertung einreichen.
     *
     * <p>POST /api/v1/reports → 201 Created
     */
    @PostMapping("/api/v1/reports")
    public ResponseEntity<ApiResponse<ReportResponse>> createReport(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateReportRequest request) {

        ReportResponse created = reportService.createReport(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Report submitted — our team will review it / Meldung eingereicht — unser Team wird sie prüfen"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Admin: list / detail / Admin: Liste / Detail
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Admin: paginated, filterable list of reports.
     * Admin: paginierte, filterbare Liste von Meldungen.
     *
     * <p>GET /api/v1/admin/reports?status=PENDING&targetType=BIKE&search=spam&page=0&size=20
     */
    @GetMapping("/api/v1/admin/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<ReportResponse>>> adminListReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportTargetType targetType,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // findAllForAdmin's @Query already has an explicit ORDER BY — keep this
        // Pageable unsorted (see the same comment on AdminController.getAuditLog).
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        PageResponse<ReportResponse> result = reportService.list(status, targetType, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Admin: get a single report by id.
     * Admin: einzelne Meldung nach ID abrufen.
     *
     * <p>GET /api/v1/admin/reports/{id}
     */
    @GetMapping("/api/v1/admin/reports/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReportResponse>> adminGetReport(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getById(id)));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Admin: triage actions / Admin: Triage-Aktionen
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Admin: mark a report as under review.
     * Admin: eine Meldung als in Prüfung markieren.
     *
     * <p>POST /api/v1/admin/reports/{id}/review
     */
    @PostMapping("/api/v1/admin/reports/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReportResponse>> adminReviewReport(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentAdmin) {

        ReportResponse updated = reportService.markUnderReview(id, currentAdmin.getId());
        return ResponseEntity.ok(ApiResponse.success(updated, "Report marked as under review / Meldung als in Prüfung markiert"));
    }

    /**
     * Admin: resolve a report — no further account action taken.
     * Admin: eine Meldung lösen — keine weitere Kontoaktion.
     *
     * <p>POST /api/v1/admin/reports/{id}/resolve
     */
    @PostMapping("/api/v1/admin/reports/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReportResponse>> adminResolveReport(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentAdmin,
            @Valid @RequestBody(required = false) ResolveReportRequest request) {

        ReportResponse updated = reportService.resolve(id, currentAdmin.getId(), orEmpty(request));
        return ResponseEntity.ok(ApiResponse.success(updated, "Report resolved / Meldung gelöst"));
    }

    /**
     * Admin: dismiss a report as unfounded.
     * Admin: eine Meldung als unbegründet ablehnen.
     *
     * <p>POST /api/v1/admin/reports/{id}/dismiss
     */
    @PostMapping("/api/v1/admin/reports/{id}/dismiss")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReportResponse>> adminDismissReport(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentAdmin,
            @Valid @RequestBody(required = false) ResolveReportRequest request) {

        ReportResponse updated = reportService.dismiss(id, currentAdmin.getId(), orEmpty(request));
        return ResponseEntity.ok(ApiResponse.success(updated, "Report dismissed / Meldung abgelehnt"));
    }

    /**
     * Admin: warn the user responsible for the reported target, and resolve the report.
     * Admin: den für das gemeldete Ziel verantwortlichen Benutzer verwarnen und die Meldung lösen.
     *
     * <p>POST /api/v1/admin/reports/{id}/warn
     */
    @PostMapping("/api/v1/admin/reports/{id}/warn")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReportResponse>> adminWarnReport(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentAdmin,
            @Valid @RequestBody(required = false) ResolveReportRequest request) {

        ReportResponse updated = reportService.warn(id, currentAdmin.getId(), orEmpty(request));
        return ResponseEntity.ok(ApiResponse.success(updated, "User warned, report resolved / Benutzer verwarnt, Meldung gelöst"));
    }

    /**
     * Admin: ban the user responsible for the reported target, and resolve the report.
     * Admin: den für das gemeldete Ziel verantwortlichen Benutzer sperren und die Meldung lösen.
     *
     * <p>POST /api/v1/admin/reports/{id}/ban
     */
    @PostMapping("/api/v1/admin/reports/{id}/ban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReportResponse>> adminBanFromReport(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentAdmin,
            @Valid @RequestBody(required = false) ResolveReportRequest request) {

        ReportResponse updated = reportService.banResponsibleUser(id, currentAdmin.getId(), orEmpty(request));
        return ResponseEntity.ok(ApiResponse.success(updated, "User banned, report resolved / Benutzer gesperrt, Meldung gelöst"));
    }

    private ResolveReportRequest orEmpty(ResolveReportRequest request) {
        return request != null ? request : new ResolveReportRequest();
    }
}
