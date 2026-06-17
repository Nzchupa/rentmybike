package com.rentmybike.admin.service;

import com.rentmybike.admin.dto.AdminStatsResponse;
import com.rentmybike.admin.dto.AdminUserResponse;
import com.rentmybike.bike.entity.ApprovalStatus;
import com.rentmybike.bike.entity.Bike;
import com.rentmybike.bike.repository.BikeRepository;
import com.rentmybike.booking.entity.Booking;
import com.rentmybike.booking.entity.BookingStatus;
import com.rentmybike.booking.repository.BookingRepository;
import com.rentmybike.common.exception.BusinessException;
import com.rentmybike.common.exception.ResourceNotFoundException;
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

import java.time.LocalDateTime;
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
    // Private helpers / Private Hilfsmethoden
    // ──────────────────────────────────────────────────────────────────────────

    /** Loads a non-deleted user or throws 404 / Lädt einen nicht gelöschten Benutzer oder wirft 404 */
    private User findActiveUser(UUID userId) {
        return userRepository.findById(userId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
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
                .createdAt(user.getCreatedAt())
                .deletedAt(user.getDeletedAt())
                .build();
    }
}
