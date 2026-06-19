package com.rentmybike.admin.dto;

import com.rentmybike.user.entity.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User summary DTO for admin user management endpoints.
 * Benutzer-Zusammenfassungs-DTO für Admin-Benutzerverwaltungs-Endpunkte.
 *
 * <p>Contains all user fields including sensitive admin-only fields (bannedAt, deletedAt).
 * <p>Enthält alle Benutzerfelder einschließlich sensibler Nur-Admin-Felder (bannedAt, deletedAt).
 */
@Data
@Builder
public class AdminUserResponse {

    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String avatarUrl;

    /** Platform role / Plattform-Rolle */
    private UserRole role;

    /** Whether email has been verified / Ob E-Mail verifiziert wurde */
    private boolean emailVerified;

    /** Whether account is currently banned / Ob Konto aktuell gesperrt ist */
    private boolean banned;

    /** Timestamp when banned, null if not banned / Zeitpunkt der Sperrung, null wenn nicht gesperrt */
    private LocalDateTime bannedAt;

    /** Timestamp when suspended, null if not suspended / Zeitpunkt der Suspendierung, null wenn nicht suspendiert */
    private LocalDateTime suspendedAt;

    /** Business display name — null unless role is BUSINESS / Geschäftsname — null außer bei Rolle BUSINESS */
    private String businessName;

    /** Whether an admin verified this business / Ob ein Admin dieses Unternehmen verifiziert hat */
    private boolean businessVerified;

    /** Account creation timestamp / Zeitpunkt der Kontoerstellung */
    private LocalDateTime createdAt;

    /** Soft-delete timestamp, null if active / Soft-Delete-Zeitpunkt, null wenn aktiv */
    private LocalDateTime deletedAt;

    /** Count of non-deleted bikes owned by this user / Anzahl nicht gelöschter Fahrräder dieses Benutzers */
    private long bikeCount;

    /** Count of non-deleted bookings made by this user as renter / Anzahl nicht gelöschter Buchungen dieses Benutzers als Mieter */
    private long bookingCount;

    /**
     * Best-available "last activity" signal for this user. There is no
     * dedicated activity-tracking table in this MVP, so this is simply the
     * user's own {@code updatedAt} timestamp (bumped on any profile/account
     * change) — a proxy, not a true last-seen/last-action timestamp.
     * Bestmögliches verfügbares "letzte Aktivität"-Signal für diesen Benutzer.
     * Es gibt in diesem MVP keine eigene Aktivitäts-Tracking-Tabelle, daher ist
     * dies einfach der eigene {@code updatedAt}-Zeitstempel des Benutzers
     * (wird bei jeder Profil-/Kontoänderung erhöht) — ein Näherungswert, kein
     * echter Last-Seen-/Letzte-Aktion-Zeitstempel.
     */
    private LocalDateTime lastActivityAt;
}
