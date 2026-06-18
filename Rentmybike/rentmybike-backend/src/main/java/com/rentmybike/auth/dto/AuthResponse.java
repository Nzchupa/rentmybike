package com.rentmybike.auth.dto;

import com.rentmybike.user.entity.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO returned after successful login or token refresh.
 * DTO, das nach erfolgreichem Login oder Token-Refresh zurückgegeben wird.
 *
 * <p>Note: JWT tokens themselves are set as httpOnly cookies, NOT in this body.
 * <p>Hinweis: JWT-Token werden als httpOnly-Cookies gesetzt, NICHT in diesem Body.
 *
 * <p>Carries the full user profile (not just minimal metadata) so the
 * frontend can initialize its user state directly from the login/refresh
 * response, without an extra immediate GET /users/me round trip.
 * <p>Enthält das vollständige Benutzerprofil (nicht nur minimale Metadaten),
 * damit das Frontend seinen Benutzerzustand direkt aus der Login-/Refresh-
 * Antwort initialisieren kann, ohne einen zusätzlichen sofortigen
 * GET /users/me-Roundtrip.
 */
@Data
@Builder
public class AuthResponse {

    /**
     * The authenticated user's UUID.
     * UUID des authentifizierten Benutzers.
     */
    private UUID userId;

    /**
     * User's first name.
     * Vorname des Benutzers.
     */
    private String firstName;

    /**
     * User's last name.
     * Nachname des Benutzers.
     */
    private String lastName;

    /**
     * Full name for display in the UI navbar/header.
     * Vollständiger Name zur Anzeige in der UI-Navigationsleiste/Kopfzeile.
     */
    private String fullName;

    /**
     * Email address of the authenticated user.
     * E-Mail-Adresse des authentifizierten Benutzers.
     */
    private String email;

    /**
     * Optional phone number.
     * Optionale Telefonnummer.
     */
    private String phone;

    /**
     * User's role — determines which features are accessible on the frontend.
     * Rolle des Benutzers — bestimmt, welche Funktionen im Frontend zugänglich sind.
     */
    private UserRole role;

    /**
     * Avatar URL for displaying profile picture in the UI.
     * Avatar-URL zur Anzeige des Profilbilds in der Benutzeroberfläche.
     */
    private String avatarUrl;

    /**
     * Whether the user's email has been verified.
     * Ob die E-Mail des Benutzers verifiziert wurde.
     */
    private boolean emailVerified;

    /**
     * Account creation timestamp.
     * Zeitpunkt der Kontoerstellung.
     */
    private LocalDateTime createdAt;

    /**
     * Whether the account is banned.
     * Ob das Konto gesperrt ist.
     */
    private boolean banned;

    /**
     * Business display name, set when role is BUSINESS (Stage 3 "Business accounts").
     * Geschäftsname, gesetzt wenn Rolle BUSINESS ist (Stage 3 "Business-Konten").
     */
    private String businessName;

    /**
     * Whether the business account has been admin-verified.
     * Ob das Geschäftskonto admin-verifiziert wurde.
     */
    private boolean businessVerified;
}
