package com.rentmybike.auth.dto;

import com.rentmybike.user.entity.UserRole;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * DTO returned after successful login or token refresh.
 * DTO, das nach erfolgreichem Login oder Token-Refresh zurückgegeben wird.
 *
 * <p>Note: JWT tokens themselves are set as httpOnly cookies, NOT in this body.
 * <p>Hinweis: JWT-Token werden als httpOnly-Cookies gesetzt, NICHT in diesem Body.
 * This DTO only contains user metadata for the frontend to use immediately.
 * Dieses DTO enthält nur Benutzermetadaten für das sofortige Frontend-Rendering.
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
     * User's role — determines which features are accessible on the frontend.
     * Rolle des Benutzers — bestimmt, welche Funktionen im Frontend zugänglich sind.
     */
    private UserRole role;

    /**
     * Avatar URL for displaying profile picture in the UI.
     * Avatar-URL zur Anzeige des Profilbilds in der Benutzeroberfläche.
     */
    private String avatarUrl;
}
