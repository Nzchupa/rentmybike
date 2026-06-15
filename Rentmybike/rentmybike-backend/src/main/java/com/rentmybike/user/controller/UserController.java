package com.rentmybike.user.controller;

import com.rentmybike.common.response.ApiResponse;
import com.rentmybike.user.dto.ChangePasswordRequest;
import com.rentmybike.user.dto.PublicUserResponse;
import com.rentmybike.user.dto.UpdateProfileRequest;
import com.rentmybike.user.dto.UserProfileResponse;
import com.rentmybike.user.entity.User;
import com.rentmybike.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * REST controller for user profile endpoints.
 * REST-Controller für Benutzerprofil-Endpunkte.
 *
 * <p>Base path: /api/v1/users
 *
 * <p>{@code @AuthenticationPrincipal User currentUser} injects the authenticated user
 * directly from the SecurityContext — populated by JwtAuthenticationFilter.
 * <p>{@code @AuthenticationPrincipal User currentUser} injiziert den authentifizierten Benutzer
 * direkt aus dem SecurityContext — befüllt durch JwtAuthenticationFilter.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ──────────────────────────────────────────────────────────────────────────
    // GET /api/v1/users/me
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns the full profile of the currently authenticated user.
     * Gibt das vollständige Profil des aktuell authentifizierten Benutzers zurück.
     *
     * @param currentUser injected from JWT cookie / aus JWT-Cookie injiziert
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            @AuthenticationPrincipal User currentUser) {

        UserProfileResponse profile = userService.getMyProfile(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PUT /api/v1/users/me
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Updates the authenticated user's first name, last name, and phone.
     * Aktualisiert Vorname, Nachname und Telefon des authentifizierten Benutzers.
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {

        UserProfileResponse updated = userService.updateProfile(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Profile updated / Profil aktualisiert"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PUT /api/v1/users/me/avatar
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Uploads a new avatar image for the authenticated user.
     * Lädt ein neues Avatar-Bild für den authentifizierten Benutzer hoch.
     *
     * <p>Request: multipart/form-data with field "file" (JPEG/PNG/WebP, max 5MB).
     * <p>Anfrage: multipart/form-data mit Feld "file" (JPEG/PNG/WebP, max 5MB).
     */
    @PutMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserProfileResponse>> uploadAvatar(
            @AuthenticationPrincipal User currentUser,
            @RequestParam("file") MultipartFile file) {

        UserProfileResponse updated = userService.uploadAvatar(currentUser.getId(), file);
        return ResponseEntity.ok(ApiResponse.success(updated, "Avatar uploaded / Avatar hochgeladen"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PUT /api/v1/users/me/password
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Changes the authenticated user's password.
     * Ändert das Passwort des authentifizierten Benutzers.
     *
     * <p>Requires the current password for verification.
     * <p>Erfordert das aktuelle Passwort zur Verifizierung.
     */
    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {

        userService.changePassword(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(null,
                "Password changed successfully / Passwort erfolgreich geändert"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /api/v1/users/{id}/public
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns the public profile of any user by ID.
     * Gibt das öffentliche Profil eines beliebigen Benutzers anhand der ID zurück.
     *
     * <p>Used by renters viewing a bike owner's profile and vice versa.
     * <p>Wird von Mietern verwendet, die das Profil eines Fahrrad-Eigentümers anzeigen.
     *
     * <p>This endpoint is publicly accessible (no auth required).
     * <p>Dieser Endpunkt ist öffentlich zugänglich (keine Authentifizierung erforderlich).
     *
     * @param userId the target user's UUID / UUID des Zielbenutzers
     */
    @GetMapping("/{id}/public")
    public ResponseEntity<ApiResponse<PublicUserResponse>> getPublicProfile(
            @PathVariable("id") UUID userId) {

        PublicUserResponse profile = userService.getPublicProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
}
