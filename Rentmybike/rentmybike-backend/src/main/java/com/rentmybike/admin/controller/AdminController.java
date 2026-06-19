package com.rentmybike.admin.controller;

import com.rentmybike.admin.dto.AdminAnalyticsResponse;
import com.rentmybike.admin.dto.AdminStatsResponse;
import com.rentmybike.admin.dto.AdminUserResponse;
import com.rentmybike.admin.service.AdminService;
import com.rentmybike.audit.dto.AuditLogResponse;
import com.rentmybike.audit.entity.AuditAction;
import com.rentmybike.audit.service.AuditLogService;
import com.rentmybike.common.response.ApiResponse;
import com.rentmybike.common.response.PageResponse;
import com.rentmybike.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin REST endpoints — all routes require ROLE_ADMIN.
 * Admin REST-Endpunkte — alle Routen erfordern ROLE_ADMIN.
 *
 * <p>Base path: /api/v1/admin
 *
 * <p>User management / Benutzerverwaltung:
 * <ul>
 *   <li>GET    /users                       — paginated list with optional search</li>
 *   <li>GET    /users/{id}                  — single user detail</li>
 *   <li>POST   /users/{id}/ban               — ban user</li>
 *   <li>POST   /users/{id}/unban             — unban user</li>
 *   <li>POST   /users/{id}/suspend           — suspend user</li>
 *   <li>POST   /users/{id}/unsuspend         — unsuspend user</li>
 *   <li>PATCH  /users/{id}/promote-to-business — promote USER to BUSINESS</li>
 *   <li>PATCH  /users/{id}/promote-to-admin    — promote USER/BUSINESS to ADMIN</li>
 *   <li>DELETE /users/{id}                  — soft delete user</li>
 * </ul>
 *
 * <p>Stats / Statistiken:
 * <ul>
 *   <li>GET    /stats             — platform aggregate statistics</li>
 * </ul>
 *
 * <p>Audit log / Audit-Log:
 * <ul>
 *   <li>GET    /audit-log         — paginated, filterable admin/moderation/account event log</li>
 * </ul>
 *
 * <p>Bike moderation / Fahrrad-Moderation:
 * <ul>
 *   <li>Handled by BikeController at /api/v1/admin/bikes/** / Von BikeController unter /api/v1/admin/bikes/** verarbeitet</li>
 * </ul>
 *
 * <p>Booking management / Buchungsverwaltung:
 * <ul>
 *   <li>Handled by BookingController at /api/v1/admin/bookings/** / Von BookingController unter /api/v1/admin/bookings/** verarbeitet</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")   // Class-level guard — redundant with SecurityConfig but explicit
                                     // Klassen-Level-Schutz — redundant mit SecurityConfig, aber explizit
public class AdminController {

    private final AdminService adminService;
    private final AuditLogService auditLogService;

    // ──────────────────────────────────────────────────────────────────────────
    // User management / Benutzerverwaltung
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Paginated user list with optional name/email search.
     * Paginierte Benutzerliste mit optionaler Name/E-Mail-Suche.
     *
     * <p>GET /api/v1/admin/users?search=john&page=0&size=20
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PageResponse<AdminUserResponse>>> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // findAllForAdmin's @Query already has an explicit ORDER BY u.createdAt DESC.
        // Passing a Pageable with its own Sort here made Spring Data JPA append a
        // second ORDER BY clause to the generated SQL ("... ORDER BY ... ORDER BY ..."),
        // which Postgres rejects as a syntax error — surfaced to the user as a generic
        // 500 "unexpected error" on the admin users page. Sort is already handled by
        // the query itself, so the Pageable here must stay unsorted.
        //
        // findAllForAdmins @Query hat bereits ein explizites ORDER BY u.createdAt DESC.
        // Ein Pageable mit eigenem Sort führte dazu, dass Spring Data JPA eine zweite
        // ORDER BY-Klausel an das generierte SQL anhängte ("... ORDER BY ... ORDER BY
        // ..."), was Postgres als Syntaxfehler ablehnt — dem Benutzer als generischer
        // 500er "unerwarteter Fehler" auf der Admin-Benutzerseite angezeigt. Die
        // Sortierung wird bereits von der Query selbst übernommen, daher muss das
        // Pageable hier unsortiert bleiben.
        PageRequest pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(adminService.listUsers(search, pageable)));
    }

    /**
     * Ban a user — sets bannedAt. Cannot ban other admins.
     * Benutzer sperren — setzt bannedAt. Kann keine anderen Admins sperren.
     *
     * <p>POST /api/v1/admin/users/{id}/ban
     */
    @PostMapping("/users/{id}/ban")
    public ResponseEntity<ApiResponse<AdminUserResponse>> banUser(
            @PathVariable("id") UUID userId,
            @AuthenticationPrincipal User currentAdmin) {

        AdminUserResponse response = adminService.banUser(currentAdmin.getId(), userId);
        return ResponseEntity.ok(ApiResponse.success(response, "User banned / Benutzer gesperrt"));
    }

    /**
     * Unban a user — clears bannedAt.
     * Benutzer entsperren — löscht bannedAt.
     *
     * <p>POST /api/v1/admin/users/{id}/unban
     */
    @PostMapping("/users/{id}/unban")
    public ResponseEntity<ApiResponse<AdminUserResponse>> unbanUser(
            @PathVariable("id") UUID userId,
            @AuthenticationPrincipal User currentAdmin) {

        AdminUserResponse response = adminService.unbanUser(currentAdmin.getId(), userId);
        return ResponseEntity.ok(ApiResponse.success(response, "User unbanned / Benutzer entsperrt"));
    }

    /**
     * Suspend a user — sets suspendedAt. Cannot suspend other admins.
     * Benutzer suspendieren — setzt suspendedAt. Kann keine anderen Admins suspendieren.
     *
     * <p>POST /api/v1/admin/users/{id}/suspend
     */
    @PostMapping("/users/{id}/suspend")
    public ResponseEntity<ApiResponse<AdminUserResponse>> suspendUser(
            @PathVariable("id") UUID userId,
            @AuthenticationPrincipal User currentAdmin) {

        AdminUserResponse response = adminService.suspendUser(currentAdmin.getId(), userId);
        return ResponseEntity.ok(ApiResponse.success(response, "User suspended / Benutzer suspendiert"));
    }

    /**
     * Unsuspend a user — clears suspendedAt.
     * Benutzer entsuspendieren — löscht suspendedAt.
     *
     * <p>POST /api/v1/admin/users/{id}/unsuspend
     */
    @PostMapping("/users/{id}/unsuspend")
    public ResponseEntity<ApiResponse<AdminUserResponse>> unsuspendUser(
            @PathVariable("id") UUID userId,
            @AuthenticationPrincipal User currentAdmin) {

        AdminUserResponse response = adminService.unsuspendUser(currentAdmin.getId(), userId);
        return ResponseEntity.ok(ApiResponse.success(response, "User unsuspended / Benutzer entsuspendiert"));
    }

    /**
     * Promote a USER account to BUSINESS.
     * Befördert ein USER-Konto zu BUSINESS.
     *
     * <p>PATCH /api/v1/admin/users/{id}/promote-to-business
     */
    @PatchMapping("/users/{id}/promote-to-business")
    public ResponseEntity<ApiResponse<AdminUserResponse>> promoteToBusiness(
            @PathVariable("id") UUID userId,
            @AuthenticationPrincipal User currentAdmin) {

        AdminUserResponse response = adminService.promoteToBusiness(currentAdmin.getId(), userId);
        return ResponseEntity.ok(ApiResponse.success(response, "User promoted to BUSINESS / Benutzer zu BUSINESS befördert"));
    }

    /**
     * Promote a USER or BUSINESS account to ADMIN.
     * Befördert ein USER- oder BUSINESS-Konto zu ADMIN.
     *
     * <p>PATCH /api/v1/admin/users/{id}/promote-to-admin
     */
    @PatchMapping("/users/{id}/promote-to-admin")
    public ResponseEntity<ApiResponse<AdminUserResponse>> promoteToAdmin(
            @PathVariable("id") UUID userId,
            @AuthenticationPrincipal User currentAdmin) {

        AdminUserResponse response = adminService.promoteToAdmin(currentAdmin.getId(), userId);
        return ResponseEntity.ok(ApiResponse.success(response, "User promoted to ADMIN / Benutzer zu ADMIN befördert"));
    }

    /**
     * Soft-delete a user account. Cannot delete other admins or self.
     * Soft-Löscht ein Benutzerkonto. Kann keine anderen Admins oder sich selbst löschen.
     *
     * <p>DELETE /api/v1/admin/users/{id}
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable("id") UUID userId,
            @AuthenticationPrincipal User currentAdmin) {

        adminService.deleteUser(currentAdmin.getId(), userId);
        return ResponseEntity.ok(ApiResponse.<Void>success(
                null, "User deleted / Benutzer gelöscht"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Business verification / Geschäftsverifizierung
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Grants the "verified" badge to a BUSINESS account.
     * Vergibt das "verifiziert"-Siegel an ein BUSINESS-Konto.
     *
     * <p>PATCH /api/v1/admin/business/{id}/verify
     */
    @PatchMapping("/business/{id}/verify")
    public ResponseEntity<ApiResponse<AdminUserResponse>> verifyBusiness(
            @PathVariable("id") UUID userId) {

        AdminUserResponse response = adminService.verifyBusiness(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Business verified / Unternehmen verifiziert"));
    }

    /**
     * Revokes the "verified" badge from a BUSINESS account.
     * Entzieht einem BUSINESS-Konto das "verifiziert"-Siegel.
     *
     * <p>PATCH /api/v1/admin/business/{id}/unverify
     */
    @PatchMapping("/business/{id}/unverify")
    public ResponseEntity<ApiResponse<AdminUserResponse>> unverifyBusiness(
            @PathVariable("id") UUID userId) {

        AdminUserResponse response = adminService.unverifyBusiness(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Business verification revoked / Verifizierung entzogen"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Platform statistics / Plattformstatistiken
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Live aggregate statistics for the admin dashboard.
     * Live-Aggregatstatistiken für das Admin-Dashboard.
     *
     * <p>GET /api/v1/admin/stats
     * <p>Returns user counts, bike counts by approval status, booking counts by status,
     * and total gross revenue from completed bookings.
     * <p>Gibt Benutzerzählungen, Fahrradzählungen nach Genehmigungsstatus, Buchungszählungen
     * nach Status und den gesamten Bruttoumsatz aus abgeschlossenen Buchungen zurück.
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getStats()));
    }

    /**
     * Daily-bucketed activity time-series (new users, new bikes, new
     * bookings, revenue) for the admin dashboard's charts.
     * Täglich gebündelte Aktivitäts-Zeitreihe (neue Benutzer, neue
     * Fahrräder, neue Buchungen, Umsatz) für die Diagramme des
     * Admin-Dashboards.
     *
     * <p>GET /api/v1/admin/analytics?days=30
     *
     * @param days trailing window in days, clamped to [1, 365], defaults to 30 / zurückliegendes Fenster in Tagen, begrenzt auf [1, 365], Standard 30
     */
    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<AdminAnalyticsResponse>> getAnalytics(
            @RequestParam(defaultValue = "30") int days) {

        int clamped = Math.max(1, Math.min(days, 365));
        return ResponseEntity.ok(ApiResponse.success(adminService.getAnalytics(clamped)));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Audit log / Audit-Log
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Paginated, filterable audit log of admin/moderation/account events.
     * Paginiertes, filterbares Audit-Log von Admin-/Moderations-/Kontoereignissen.
     *
     * <p>GET /api/v1/admin/audit-log?action=USER_BANNED&targetType=USER&search=john&page=0&size=20
     */
    @GetMapping("/audit-log")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getAuditLog(
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Same pitfall as listUsers above: AuditLogRepository.findAllForAdmin's
        // @Query already has an explicit ORDER BY a.createdAt DESC, so the
        // Pageable passed here must stay unsorted to avoid a double ORDER BY.
        //
        // Gleiche Falle wie oben bei listUsers: AuditLogRepository.findAllForAdmin
        // hat bereits ein explizites ORDER BY a.createdAt DESC, daher muss das hier
        // übergebene Pageable unsortiert bleiben, um ein doppeltes ORDER BY zu vermeiden.
        PageRequest pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(auditLogService.list(action, targetType, search, pageable)));
    }
}
