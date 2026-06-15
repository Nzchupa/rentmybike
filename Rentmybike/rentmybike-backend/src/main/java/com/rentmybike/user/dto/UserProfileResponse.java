package com.rentmybike.user.dto;

import com.rentmybike.user.entity.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Full user profile DTO — returned for GET /api/v1/users/me (own profile only).
 * Vollständiges Benutzerprofil-DTO — wird für GET /api/v1/users/me zurückgegeben (nur eigenes Profil).
 *
 * <p>Contains sensitive fields (email, emailVerified) not shown in public profile.
 * <p>Enthält sensible Felder (E-Mail, emailVerified), die im öffentlichen Profil nicht angezeigt werden.
 */
@Data
@Builder
public class UserProfileResponse {

    private UUID id;

    /** Email — only visible to the user themselves / E-Mail — nur für den Benutzer selbst sichtbar */
    private String email;

    private String firstName;
    private String lastName;

    /** Computed full name for display / Berechneter vollständiger Name zur Anzeige */
    private String fullName;

    private String phone;
    private String avatarUrl;
    private UserRole role;

    /** Whether the email has been verified / Ob die E-Mail verifiziert wurde */
    private boolean emailVerified;

    /** Whether the account is banned / Ob das Konto gesperrt ist */
    private boolean banned;

    private LocalDateTime createdAt;
}
