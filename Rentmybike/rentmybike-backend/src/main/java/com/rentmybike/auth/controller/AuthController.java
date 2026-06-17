package com.rentmybike.auth.controller;

import com.rentmybike.auth.dto.AuthResponse;
import com.rentmybike.auth.dto.LoginRequest;
import com.rentmybike.auth.dto.RegisterRequest;
import com.rentmybike.auth.service.AuthService;
import com.rentmybike.common.response.ApiResponse;
import com.rentmybike.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication endpoints.
 * REST-Controller für Authentifizierungs-Endpunkte.
 *
 * <p>Base path: /api/v1/auth
 *
 * <p>All cookie operations are handled in the service layer to keep the
 * controller thin and testable.
 * <p>Alle Cookie-Operationen werden in der Service-Schicht verwaltet,
 * um den Controller schlank und testbar zu halten.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ──────────────────────────────────────────────────────────────────────────
    // POST /api/v1/auth/register
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Register a new user account.
     * Neues Benutzerkonto registrieren.
     *
     * <p>201 Created on success. The response message reflects whether the
     * verification email actually went out — if Resend failed, the account
     * still exists, but the user is told to use "resend verification" instead
     * of being falsely told to check their inbox.
     * <p>201 Created bei Erfolg. Die Antwortnachricht spiegelt wider, ob die
     * Verifizierungs-E-Mail tatsächlich versendet wurde — falls Resend
     * fehlschlug, existiert das Konto trotzdem, aber der Benutzer wird
     * angewiesen, "Verifizierung erneut senden" zu nutzen, statt fälschlich
     * zur Prüfung des Posteingangs aufgefordert zu werden.
     *
     * @param request registration data (validated) / Registrierungsdaten (validiert)
     * @return 201 with success message / 201 mit Erfolgsmeldung
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        boolean emailSent = authService.register(request);
        String message = emailSent
                ? "Registration successful! Check your email to verify your account. / " +
                  "Registrierung erfolgreich! Überprüfe deine E-Mail zur Kontobestätigung."
                : "Registration successful, but we couldn't send the verification email right now. " +
                  "Use \"resend verification\" to try again. / " +
                  "Registrierung erfolgreich, aber die Verifizierungs-E-Mail konnte momentan nicht gesendet werden. " +
                  "Nutze \"Verifizierung erneut senden\", um es erneut zu versuchen.";
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(null, message));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /api/v1/auth/login
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Authenticate user and set JWT cookies.
     * Benutzer authentifizieren und JWT-Cookies setzen.
     *
     * <p>200 OK with user metadata + httpOnly access_token & refresh_token cookies.
     * <p>200 OK mit Benutzermetadaten + httpOnly access_token & refresh_token Cookies.
     *
     * @param request  login credentials / Anmeldedaten
     * @param response used to attach cookies / zum Anhängen von Cookies
     * @return user info for frontend state / Benutzerinfo für Frontend-Zustand
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        AuthResponse authResponse = authService.login(request, response);
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Login successful / Anmeldung erfolgreich"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /api/v1/auth/logout
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Log out the current user: clears auth cookies and revokes their
     * outstanding tokens server-side (see AuthService.logout).
     * Aktuellen Benutzer abmelden: löscht Auth-Cookies und widerruft
     * serverseitig dessen ausstehende Token (siehe AuthService.logout).
     *
     * @param currentUser the authenticated user, if the access token was still
     *                     valid, or null if it had already expired / der
     *                     authentifizierte Benutzer, falls der Zugriffstoken noch
     *                     gültig war, sonst null
     * @param response    used to clear cookies / zum Löschen von Cookies
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal User currentUser,
            HttpServletResponse response) {
        authService.logout(currentUser == null ? null : currentUser.getId(), response);
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully / Erfolgreich abgemeldet"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /api/v1/auth/refresh
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Refresh the access token using the refresh token cookie.
     * Zugriffstoken mithilfe des Refresh-Token-Cookies aktualisieren.
     *
     * <p>Called silently by the frontend when the access token is about to expire.
     * <p>Wird still vom Frontend aufgerufen, wenn der Zugriffstoken bald abläuft.
     *
     * @param request  to read refresh_token cookie / zum Lesen des refresh_token-Cookies
     * @param response to set the new access_token cookie / zum Setzen des neuen access_token-Cookies
     * @return updated user metadata / aktualisierte Benutzermetadaten
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {

        AuthResponse authResponse = authService.refresh(request, response);
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Token refreshed / Token aktualisiert"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /api/v1/auth/verify-email
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Verify the user's email using the token from the verification link.
     * E-Mail des Benutzers mithilfe des Tokens aus dem Verifizierungslink bestätigen.
     *
     * <p>GET is used here because this endpoint is opened directly from an email link.
     * <p>GET wird hier verwendet, da dieser Endpunkt direkt aus einem E-Mail-Link geöffnet wird.
     *
     * @param token the UUID token from the email / UUID-Token aus der E-Mail
     */
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success(null,
                "Email verified successfully! You can now log in. / " +
                "E-Mail erfolgreich verifiziert! Du kannst dich jetzt anmelden."));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /api/v1/auth/resend-verification
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Resend the verification email to the given address.
     * Verifizierungs-E-Mail erneut an die angegebene Adresse senden.
     *
     * @param email the user's email address / E-Mail-Adresse des Benutzers
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @RequestParam @NotBlank @Email String email) {

        authService.resendVerification(email);
        return ResponseEntity.ok(ApiResponse.success(null,
                "Verification email sent / Verifizierungs-E-Mail gesendet"));
    }
}
