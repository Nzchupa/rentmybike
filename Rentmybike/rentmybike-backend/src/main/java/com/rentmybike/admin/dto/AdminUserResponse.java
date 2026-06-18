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

    /** Business display name — null unless role is BUSINESS / Geschäftsname — null außer bei Rolle BUSINESS */
    private String businessName;

    /** Whether an admin verified this business / Ob ein Admin dieses Unternehmen verifiziert hat */
    private boolean businessVerified;

    /** Account creation timestamp / Zeitpunkt der Kontoerstellung */
    private LocalDateTime createdAt;

    /** Soft-delete timestamp, null if active / Soft-Delete-Zeitpunkt, null wenn aktiv */
    private LocalDateTime deletedAt;
}
