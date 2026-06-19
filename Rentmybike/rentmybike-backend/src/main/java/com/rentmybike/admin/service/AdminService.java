package com.rentmybike.admin.service;

import com.rentmybike.admin.dto.AdminAnalyticsResponse;
import com.rentmybike.admin.dto.AdminStatsResponse;
import com.rentmybike.admin.dto.AdminTimeSeriesPoint;
import com.rentmybike.admin.dto.AdminUserResponse;
import com.rentmybike.audit.entity.AuditAction;
import com.rentmybike.audit.service.AuditLogService;
import com.rentmybike.bike.entity.ApprovalStatus;
import com.rentmybike.bike.entity.Bike;
import com.rentmybike.bike.repository.BikeRepository;
import com.rentmybike.booking.entity.Booking;
import com.rentmybike.booking.entity.BookingStatus;
import com.rentmybike.booking.repository.BookingRepository;
import com.rentmybike.common.exception.BusinessException;
import com.rentmybike.common.exception.ResourceNotFoundException;
import com.rentmybike.common.projection.DailyAmountProjection;
import com.rentmybike.common.projection.DailyCountProjection;
import com.rentmybike.common.response.PageResponse;
import com.rentmybike.user.entity.User;
import com.rentmybike.user.entity.UserRole;
import com.rentmybike.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin service — user management and platform statistics.
 * Admin-Service — Benutzerverwaltung und Plattformstatistiken.
 *
 * <p>Bike moderation (approve/reject/delete) lives in BikeService, exposed via
 * /api/v1/admin/bikes/** in BikeController — no duplication here.
 *
 * <p>Fahrrad-Moderation (approve/reject/delete) ist in BikeService, zugänglich über
 * /api/v1/admin/bikes/** in BikeController — keine Duplizierung hier.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminService {

    private final UserRepository userRepository;
    private final BikeRepository bikeRepository;
    private final BookingRepository bookingRepository;
    private final AuditLogService auditLogService;

    // ──────────────────────────────────────────────────────────────────────────
    // User listing / Benutzerliste
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Paginated list of all active users, with optional name/email search.
     * Paginierte Liste aller aktiven Benutzer, mit optionaler Name/E-Mail-Suche.
     *
     * @param search null or empty = return all / null oder leer = alle zurückgeben
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> listUsers(String search, Pageable pageable) {
        // Empty string → treat as no-filter (avoid matching everything with empty LIKE)
        // Leerer String → als kein Filter behandeln
        String effectiveSearch = (search != null && search.isBlank()) ? null : search;

        Page<User> page = userRepository.findAllForAdmin(effectiveSearch, pageable);
        return PageResponse.from(page.map(this::toAdminUserResponse));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // User moderation / Benutzer-Moderation
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Bans a user — sets bannedAt timestamp. Locked accounts cannot authenticate.
     * Sperrt einen Benutzer — setzt bannedAt-Zeitstempel. Gesperrte Konten können sich nicht authentifizieren.
     *
     * <p>Admins cannot ban other admins (prevents accidental lockout).
     * <p>Admins können keine anderen Admins sperren (verhindert versehentliche Aussperrung).
     *
     * @param adminId the admin performing the action / der Admin, der die Aktion durchführt
     * @param userId  the target user / der Zielbenutzer
     */
    public AdminUserResponse banUser(UUID adminId, UUID userId) {
        User target = findActiveUser(userId);

        if (target.isBanned()) {
            throw new BusinessException("User is already banned / Benutzer ist bereits gesperrt");
        }
        if (target.getRole() == UserRole.ADMIN) {
            throw new BusinessException("Cannot ban another admin / Kann keinen anderen Admin sperren");
        }
        if (target.getId().equals(adminId)) {
            throw new BusinessException("Cannot ban yourself / Kann sich nicht selbst sperren");
        }

        target.setBannedAt(LocalDateTime.now());
        log.info("Admin {} banned user {} / Admin {} hat Benutzer {} gesperrt", adminId, userId, adminId, userId);

        auditLogService.record(adminId, adminName(adminId), AuditAction.USER_BANNED,
                "USER", userId, null);

        return toAdminUserResponse(target);
    }

    /**
     * Unbans a user — clears the bannedAt timestamp.
     * Hebt die Sperrung eines Benutzers auf — löscht den bannedAt-Zeitstempel.
     */
    public AdminUserResponse unbanUser(UUID adminId, UUID userId) {
        User target = findActiveUser(userId);

        if (!target.isBanned()) {
            throw new BusinessException("User is not banned / Benutzer ist nicht gesperrt");
        }

        target.setBannedAt(null);
        log.info("Admin {} unbanned user {} / Admin {} hat Benutzer {} entsperrt", adminId, userId, adminId, userId);

        auditLogService.record(adminId, adminName(adminId), AuditAction.USER_UNBANNED,
                "USER", userId, null);

        return toAdminUserResponse(target);
    }

    /**
     * Suspends a user — sets suspendedAt timestamp. Distinct from banning,
     * but enforces the same login block (see {@code User.isAccountNonLocked()}).
     * Suspendiert einen Benutzer — setzt suspendedAt-Zeitstempel. Unterscheidet
     * sich von einer Sperrung, erzwingt aber dieselbe Anmeldesperre (siehe
     * {@code User.isAccountNonLocked()}).
     *
     * <p>Admins cannot suspend other admins (prevents accidental lockout).
     * <p>Admins können keine anderen Admins suspendieren (verhindert versehentliche Aussperrung).
     *
     * @param adminId the admin performing the action / der Admin, der die Aktion durchführt
     * @param userId  the target user / der Zielbenutzer
     */
    public AdminUserResponse suspendUser(UUID adminId, UUID userId) {
        User target = findActiveUser(userId);

        if (target.isSuspended()) {
            throw new BusinessException("User is already suspended / Benutzer ist bereits suspendiert");
        }
        if (target.getRole() == UserRole.ADMIN) {
            throw new BusinessException("Cannot suspend another admin / Kann keinen anderen Admin suspendieren");
        }
        if (target.getId().equals(adminId)) {
            throw new BusinessException("Cannot suspend yourself / Kann sich nicht selbst suspendieren");
        }

        target.setSuspendedAt(LocalDateTime.now());
        log.info("Admin {} suspended user {} / Admin {} hat Benutzer {} suspendiert", adminId, userId, adminId, userId);

        auditLogService.record(adminId, adminName(adminId), AuditAction.USER_SUSPENDED,
                "USER", userId, null);

        return toAdminUserResponse(target);
    }

    /**
     * Unsuspends a user — clears the suspendedAt timestamp.
     * Hebt die Suspendierung eines Benutzers auf — löscht den suspendedAt-Zeitstempel.
     */
    public AdminUserResponse unsuspendUser(UUID adminId, UUID userId) {
        User target = findActiveUser(userId);

        if (!target.isSuspended()) {
            throw new BusinessException("User is not suspended / Benutzer ist nicht suspendiert");
        }

        target.setSuspendedAt(null);
        log.info("Admin {} unsuspended user {} / Admin {} hat Benutzer {} entsuspendiert", adminId, userId, adminId, userId);

        auditLogService.record(adminId, adminName(adminId), AuditAction.USER_UNSUSPENDED,
                "USER", userId, null);

        return toAdminUserResponse(target);
    }

    /**
     * Soft-deletes a user account, cascading to their bikes and bookings so
     * nothing active is left pointing at a deleted account.
     * Soft-löscht ein Benutzerkonto und kaskadiert auf dessen Fahrräder und
     * Buchungen, damit nichts Aktives auf ein gelöschtes Konto verweist.
     *
     * <p>Without this, a deleted user's bike listings stayed APPROVED/visible
     * in public search, and their PENDING/ACCEPTED bookings (as renter or as
     * bike owner) stayed active with no one able to act on them.
     * <p>Ohne dies blieben die Fahrrad-Inserate eines gelöschten Benutzers
     * APPROVED/sichtbar in der öffentlichen Suche, und seine PENDING/ACCEPTED-
     * Buchungen (als Mieter oder als Fahrrad-Eigentümer) blieben aktiv, ohne
     * dass jemand darauf reagieren konnte.
     *
     * <p>Admin cannot delete themselves.
     * <p>Admin kann sich nicht selbst löschen.
     */
    public void deleteUser(UUID adminId, UUID userId) {
        User target = findActiveUser(userId);

        if (target.getId().equals(adminId)) {
            throw new BusinessException("Cannot delete your own account / Kann eigenes Konto nicht löschen");
        }
        if (target.getRole() == UserRole.ADMIN) {
            throw new BusinessException("Cannot delete another admin / Kann keinen anderen Admin löschen");
        }

        // Cancel every booking where this user is the renter or the bike owner —
        // both sides lose the ability to act on the booking once either party
        // is gone, so leaving it PENDING/ACCEPTED would strand the other side.
        // Jede Buchung stornieren, bei der dieser Benutzer Mieter oder
        // Fahrrad-Eigentümer ist — beide Seiten verlieren die Möglichkeit, auf
        // die Buchung zu reagieren, sobald eine Partei weg ist.
        for (Booking booking : bookingRepository.findActiveBookingsByUserId(userId)) {
            booking.setStatus(BookingStatus.CANCELLED);
        }

        // Soft-delete all of the user's bike listings and take them off the
        // market — an orphaned listing should not still be rentable.
        // Alle Fahrrad-Inserate des Benutzers soft löschen und vom Markt nehmen —
        // ein verwaistes Inserat sollte nicht weiter mietbar sein.
        for (Bike bike : bikeRepository.findActiveBikesByOwnerId(userId)) {
            bike.setAvailable(false);
            bike.softDelete();
        }

        target.softDelete();
        log.info("Admin {} soft-deleted user {} (cascaded to bikes + bookings) / "
                + "Admin {} hat Benutzer {} soft-gelöscht (kaskadiert auf Fahrräder + Buchungen)",
                adminId, userId, adminId, userId);

        auditLogService.record(adminId, adminName(adminId), AuditAction.USER_DELETED,
                "USER", userId, null);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Business verification / Geschäftsverifizierung
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Marks a BUSINESS account as verified — a simple admin-granted trust badge
     * (no document upload/review flow in this MVP).
     * Markiert ein BUSINESS-Konto als verifiziert — ein einfaches, vom Admin
     * vergebenes Vertrauenssiegel (kein Dokumenten-Upload/Prüfungsablauf in
     * diesem MVP).
     */
    public AdminUserResponse verifyBusiness(UUID userId) {
        User target = findActiveUser(userId);

        if (target.getRole() != UserRole.BUSINESS) {
            throw new BusinessException(
                    "User is not a business account / Benutzer ist kein Geschäftskonto");
        }
        if (target.isBusinessVerified()) {
            throw new BusinessException(
                    "Business is already verified / Unternehmen ist bereits verifiziert");
        }

        target.setBusinessVerified(true);
        log.info("Admin verified business account {} / Admin hat Geschäftskonto {} verifiziert", userId, userId);

        // No currentAdmin principal is passed into this method (the controller
        // doesn't take one for this endpoint) — recorded as a system/unattributed
        // admin action rather than guessing an actor.
        // In diese Methode wird kein currentAdmin-Principal übergeben (der
        // Controller nimmt für diesen Endpunkt keinen entgegen) — wird als
        // System-/nicht zugeordnete Admin-Aktion erfasst, statt einen Akteur zu raten.
        auditLogService.record(null, null, AuditAction.BUSINESS_VERIFIED,
                "USER", userId, null);

        return toAdminUserResponse(target);
    }

    /**
     * Revokes a BUSINESS account's verified badge.
     * Entzieht einem BUSINESS-Konto das Verifizierungssiegel.
     */
    public AdminUserResponse unverifyBusiness(UUID userId) {
        User target = findActiveUser(userId);

        if (target.getRole() != UserRole.BUSINESS) {
            throw new BusinessException(
                    "User is not a business account / Benutzer ist kein Geschäftskonto");
        }

        target.setBusinessVerified(false);
        log.info("Admin revoked verification for business account {} / "
                + "Admin hat Verifizierung für Geschäftskonto {} entzogen", userId, userId);

        // See verifyBusiness() above — no currentAdmin principal available here either.
        // Siehe verifyBusiness() oben — auch hier kein currentAdmin-Principal verfügbar.
        auditLogService.record(null, null, AuditAction.BUSINESS_UNVERIFIED,
                "USER", userId, null);

        return toAdminUserResponse(target);
    }

    /**
     * Promotes a USER account to BUSINESS.
     * Befördert ein USER-Konto zu BUSINESS.
     */
    public AdminUserResponse promoteToBusiness(UUID adminId, UUID userId) {
        User target = findActiveUser(userId);

        if (target.getRole() != UserRole.USER) {
            throw new BusinessException(
                    "User is already BUSINESS or ADMIN / Benutzer ist bereits BUSINESS oder ADMIN");
        }

        target.setRole(UserRole.BUSINESS);
        log.info("Admin {} promoted user {} to BUSINESS / Admin {} hat Benutzer {} zu BUSINESS befördert",
                adminId, userId, adminId, userId);

        auditLogService.record(adminId, adminName(adminId), AuditAction.USER_PROMOTED_TO_BUSINESS,
                "USER", userId, null);

        return toAdminUserResponse(target);
    }

    /**
     * Promotes a USER or BUSINESS account to ADMIN.
     * Befördert ein USER- oder BUSINESS-Konto zu ADMIN.
     */
    public AdminUserResponse promoteToAdmin(UUID adminId, UUID userId) {
        User target = findActiveUser(userId);

        if (target.getRole() == UserRole.ADMIN) {
            throw new BusinessException("User is already an admin / Benutzer ist bereits ein Admin");
        }

        target.setRole(UserRole.ADMIN);
        log.info("Admin {} promoted user {} to ADMIN / Admin {} hat Benutzer {} zu ADMIN befördert",
                adminId, userId, adminId, userId);

        auditLogService.record(adminId, adminName(adminId), AuditAction.USER_PROMOTED_TO_ADMIN,
                "USER", userId, null);

        return toAdminUserResponse(target);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Platform statistics / Plattformstatistiken
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Aggregates live platform statistics for the admin dashboard.
     * Aggregiert Live-Plattformstatistiken für das Admin-Dashboard.
     */
    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        return AdminStatsResponse.builder()
                // User stats / Benutzerstatistiken
                .totalUsers(userRepository.countByDeletedAtIsNull())
                .bannedUsers(userRepository.countByBannedAtIsNotNullAndDeletedAtIsNull())
                .totalAdmins(userRepository.countByRoleAndDeletedAtIsNull(UserRole.ADMIN))

                // Bike stats / Fahrrad-Statistiken
                .totalBikes(bikeRepository.countByDeletedAtIsNull())
                .pendingBikes(bikeRepository.countByApprovalStatusAndDeletedAtIsNull(ApprovalStatus.PENDING))
                .approvedBikes(bikeRepository.countByApprovalStatusAndDeletedAtIsNull(ApprovalStatus.APPROVED))
                .rejectedBikes(bikeRepository.countByApprovalStatusAndDeletedAtIsNull(ApprovalStatus.REJECTED))
                .changesRequestedBikes(bikeRepository.countByApprovalStatusAndDeletedAtIsNull(ApprovalStatus.CHANGES_REQUESTED))

                // Booking stats / Buchungsstatistiken
                .totalBookings(bookingRepository.countByDeletedAtIsNull())
                .pendingBookings(bookingRepository.countByStatusAndDeletedAtIsNull(BookingStatus.PENDING))
                .acceptedBookings(bookingRepository.countByStatusAndDeletedAtIsNull(BookingStatus.ACCEPTED))
                .completedBookings(bookingRepository.countByStatusAndDeletedAtIsNull(BookingStatus.COMPLETED))
                .cancelledBookings(bookingRepository.countByStatusAndDeletedAtIsNull(BookingStatus.CANCELLED))
                .rejectedBookings(bookingRepository.countByStatusAndDeletedAtIsNull(BookingStatus.REJECTED))

                // Revenue / Einnahmen
                .totalRevenue(bookingRepository.sumTotalPriceOfCompleted())

                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Analytics time-series / Analyse-Zeitreihe
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Daily-bucketed platform activity (new users, new bikes, new bookings,
     * revenue) for the trailing {@code days} days, including today.
     * Zeigt täglich gebündelte Plattformaktivität (neue Benutzer, neue
     * Fahrräder, neue Buchungen, Umsatz) für die zurückliegenden {@code
     * days} Tage, einschließlich heute.
     *
     * <p>Each repository query only returns rows for days that actually had
     * activity (a {@code GROUP BY} naturally skips empty days), so this
     * method builds the full continuous day range first and zero-fills any
     * day missing from a given metric's result set — the frontend chart
     * never has to infer missing dates itself.
     * <p>Jede Repository-Abfrage liefert nur Zeilen für Tage mit
     * tatsächlicher Aktivität (ein {@code GROUP BY} überspringt naturgemäß
     * leere Tage), daher baut diese Methode zuerst den vollständigen,
     * durchgehenden Tagesbereich auf und füllt jeden in einem
     * Metrik-Ergebnis fehlenden Tag mit Null auf — das Frontend-Diagramm
     * muss fehlende Daten nie selbst ableiten.
     *
     * @param days trailing window size, e.g. 7/30/90 — callers should clamp this / Größe des zurückliegenden Fensters, z. B. 7/30/90 — Aufrufer sollten dies begrenzen
     */
    @Transactional(readOnly = true)
    public AdminAnalyticsResponse getAnalytics(int days) {
        LocalDate today = LocalDate.now();
        LocalDate rangeStart = today.minusDays(days - 1L);
        LocalDateTime from = rangeStart.atStartOfDay();

        Map<LocalDate, Long> signupsByDay = toCountMap(userRepository.countDailySignupsSince(from));
        Map<LocalDate, Long> listingsByDay = toCountMap(bikeRepository.countDailyListingsSince(from));
        Map<LocalDate, Long> bookingsByDay = toCountMap(bookingRepository.countDailyBookingsSince(from));
        Map<LocalDate, BigDecimal> revenueByDay = toAmountMap(bookingRepository.sumDailyRevenueSince(from));

        List<AdminTimeSeriesPoint> series = rangeStart.datesUntil(today.plusDays(1))
                .map(day -> AdminTimeSeriesPoint.builder()
                        .date(day)
                        .newUsers(signupsByDay.getOrDefault(day, 0L))
                        .newBikes(listingsByDay.getOrDefault(day, 0L))
                        .newBookings(bookingsByDay.getOrDefault(day, 0L))
                        .revenue(revenueByDay.getOrDefault(day, BigDecimal.ZERO))
                        .build())
                .toList();

        return AdminAnalyticsResponse.builder()
                .rangeDays(days)
                .series(series)
                .build();
    }

    private Map<LocalDate, Long> toCountMap(List<DailyCountProjection> rows) {
        Map<LocalDate, Long> map = new HashMap<>();
        for (DailyCountProjection row : rows) {
            map.put(row.getDay(), row.getCount());
        }
        return map;
    }

    private Map<LocalDate, BigDecimal> toAmountMap(List<DailyAmountProjection> rows) {
        Map<LocalDate, BigDecimal> map = new HashMap<>();
        for (DailyAmountProjection row : rows) {
            map.put(row.getDay(), row.getAmount());
        }
        return map;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers / Private Hilfsmethoden
    // ──────────────────────────────────────────────────────────────────────────

    /** Loads a non-deleted user or throws 404 / Lädt einen nicht gelöschten Benutzer oder wirft 404 */
    private User findActiveUser(UUID userId) {
        return userRepository.findById(userId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    /**
     * Resolves the acting admin's display name for the audit log's denormalized
     * actorName snapshot. Falls back to null if the admin can't be loaded
     * (should not happen in practice, but the audit write must never block the
     * underlying admin action).
     * Löst den Anzeigenamen des handelnden Admins für die denormalisierte
     * actorName-Momentaufnahme des Audit-Logs auf. Fällt auf null zurück, falls
     * der Admin nicht geladen werden kann (sollte in der Praxis nicht vorkommen,
     * aber das Schreiben des Audit-Eintrags darf die eigentliche Admin-Aktion
     * nie blockieren).
     */
    private String adminName(UUID adminId) {
        if (adminId == null) {
            return null;
        }
        return userRepository.findById(adminId)
                .map(User::getFullName)
                .orElse(null);
    }

    /** Maps a User entity to AdminUserResponse DTO / Mappt User-Entity auf AdminUserResponse-DTO */
    private AdminUserResponse toAdminUserResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .emailVerified(user.isEmailVerified())
                .banned(user.isBanned())
                .bannedAt(user.getBannedAt())
                .suspendedAt(user.getSuspendedAt())
                .businessName(user.getBusinessName())
                .businessVerified(user.isBusinessVerified())
                .createdAt(user.getCreatedAt())
                .deletedAt(user.getDeletedAt())
                .bikeCount(bikeRepository.countByOwnerIdAndDeletedAtIsNull(user.getId()))
                .bookingCount(bookingRepository.countByRenterIdAndDeletedAtIsNull(user.getId()))
                // No dedicated activity-tracking table in this MVP — updatedAt
                // (bumped on any profile/account change) is the best available
                // proxy for "last activity".
                // Keine eigene Aktivitäts-Tracking-Tabelle in diesem MVP —
                // updatedAt (wird bei jeder Profil-/Kontoänderung erhöht) ist
                // der bestmögliche verfügbare Näherungswert für "letzte Aktivität".
                .lastActivityAt(user.getUpdatedAt())
                .build();
    }
}
