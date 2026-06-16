package com.rentmybike.auth.service;

import com.rentmybike.auth.dto.AuthResponse;
import com.rentmybike.auth.dto.LoginRequest;
import com.rentmybike.auth.dto.RegisterRequest;
import com.rentmybike.common.config.AppProperties;
import com.rentmybike.common.exception.BusinessException;
import com.rentmybike.common.exception.ResourceNotFoundException;
import com.rentmybike.user.entity.User;
import com.rentmybike.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

/**
 * Core authentication service — handles registration, login, logout,
 * token refresh, and email verification.
 * Kern-Authentifizierungsservice — verarbeitet Registrierung, Anmeldung, Abmeldung,
 * Token-Aktualisierung und E-Mail-Verifizierung.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    // Cookie names — must match those read in JwtAuthenticationFilter
    // Cookie-Namen — müssen mit denen in JwtAuthenticationFilter übereinstimmen
    public static final String ACCESS_TOKEN_COOKIE = "access_token";
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final AppProperties appProperties;

    // ──────────────────────────────────────────────────────────────────────────
    // Registration / Registrierung
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Registers a new user, saves them to the database (unverified),
     * and sends a verification email asynchronously.
     * Registriert einen neuen Benutzer, speichert ihn in der Datenbank (unverifiziert)
     * und sendet asynchron eine Verifizierungs-E-Mail.
     *
     * @param request registration data / Registrierungsdaten
     * @throws BusinessException if email is already taken / wenn E-Mail bereits vergeben
     */
    public void register(RegisterRequest request) {
        // Check for duplicate email / Auf doppelte E-Mail prüfen
        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new BusinessException("Email already registered / E-Mail bereits registriert: " + request.getEmail());
        }

        // Generate email verification token / E-Mail-Verifizierungstoken generieren
        String verificationToken = UUID.randomUUID().toString();

        // In dev with auto-verify enabled, skip email flow entirely
        // In dev mit auto-verify: E-Mail-Fluss komplett überspringen
        boolean autoVerify = appProperties.isAutoVerifyEmail();

        // Build and save the new user / Neuen Benutzer erstellen und speichern
        User user = User.builder()
                .email(request.getEmail().toLowerCase().strip())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().strip())
                .lastName(request.getLastName().strip())
                .emailVerified(autoVerify)
                .emailVerificationToken(autoVerify ? null : verificationToken)
                .emailVerificationTokenExpiry(autoVerify ? null : LocalDateTime.now().plusHours(24))
                .build();

        userRepository.save(user);
        log.info("New user registered{}: {} / Neuer Benutzer registriert{}: {}",
                autoVerify ? " (auto-verified)" : "",
                user.getEmail(),
                autoVerify ? " (auto-verifiziert)" : "",
                user.getEmail());

        // Send verification email only when not auto-verifying
        // Verifizierungs-E-Mail nur senden, wenn nicht auto-verifiziert
        if (!autoVerify) {
            emailService.sendVerificationEmail(user, verificationToken);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Login / Anmeldung
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Authenticates the user and sets JWT tokens as httpOnly cookies.
     * Authentifiziert den Benutzer und setzt JWT-Token als httpOnly-Cookies.
     *
     * @param request  login credentials / Anmeldedaten
     * @param response HTTP response to attach cookies / HTTP-Antwort zum Anhängen von Cookies
     * @return user metadata for frontend state initialization / Benutzermetadaten für Frontend-Zustandsinitialisierung
     */
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        // Delegate to Spring Security's AuthenticationManager
        // An Spring Securitys AuthenticationManager delegieren
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().toLowerCase(),
                            request.getPassword()
                    )
            );
        } catch (DisabledException e) {
            // Account exists but email not verified / Konto existiert, aber E-Mail nicht verifiziert
            throw new BusinessException("Please verify your email first / Bitte bestätige zuerst deine E-Mail");
        } catch (LockedException e) {
            // Account is banned / Konto ist gesperrt
            throw new BusinessException("Account is banned / Konto ist gesperrt");
        } catch (BadCredentialsException e) {
            // Wrong email or password / Falsche E-Mail oder falsches Passwort
            throw new BusinessException("Invalid credentials / Ungültige Anmeldedaten");
        }

        // Load the user entity / Benutzer-Entity laden
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found / Benutzer nicht gefunden"));

        // Generate tokens and set cookies / Token generieren und Cookies setzen
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        setTokenCookies(response, accessToken, refreshToken);

        log.info("User logged in: {} / Benutzer angemeldet: {}", user.getEmail(), user.getEmail());

        // Return minimal user info for frontend — tokens stay in cookies!
        // Minimale Benutzerinformationen für Frontend zurückgeben — Token bleiben in Cookies!
        return buildAuthResponse(user);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Logout / Abmeldung
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Clears auth cookies, effectively logging the user out.
     * Löscht Auth-Cookies und meldet den Benutzer damit ab.
     *
     * @param response HTTP response to clear cookies on / HTTP-Antwort zum Löschen der Cookies
     */
    public void logout(HttpServletResponse response) {
        // Clear both tokens by setting expired cookies / Beide Token durch abgelaufene Cookies löschen
        clearTokenCookies(response);
        log.debug("User logged out — cookies cleared / Benutzer abgemeldet — Cookies gelöscht");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Token refresh / Token-Aktualisierung
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Reads the refresh token cookie, validates it, and issues a new access token.
     * Liest das Refresh-Token-Cookie, validiert es und gibt einen neuen Zugriffstoken aus.
     *
     * @param request  HTTP request containing the refresh cookie / HTTP-Anfrage mit Refresh-Cookie
     * @param response HTTP response to set the new access token cookie / HTTP-Antwort mit neuem Access-Token-Cookie
     * @return updated user metadata / aktualisierte Benutzermetadaten
     */
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        // Extract refresh token from cookie / Refresh-Token aus Cookie extrahieren
        String refreshToken = extractCookieValue(request, REFRESH_TOKEN_COOKIE);

        if (refreshToken == null || !jwtService.isRefreshTokenValid(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Invalid or expired refresh token / Ungültiger oder abgelaufener Aktualisierungstoken");
        }

        // Load user and issue new access token / Benutzer laden und neuen Zugriffstoken ausgeben
        UUID userId = jwtService.extractUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found / Benutzer nicht gefunden"));

        if (!user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is inactive / Konto ist inaktiv");
        }

        // Only rotate the access token — keep the refresh token / Nur Zugriffstoken rotieren — Refresh-Token behalten
        String newAccessToken = jwtService.generateAccessToken(user);
        addCookie(response, ACCESS_TOKEN_COOKIE, newAccessToken,
                (int) (appProperties.getJwt().getAccessTokenExpiration() / 1000));

        return buildAuthResponse(user);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Email verification / E-Mail-Verifizierung
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Marks the user's email as verified using the token from the verification link.
     * Markiert die E-Mail des Benutzers mithilfe des Tokens aus dem Verifizierungslink als verifiziert.
     *
     * @param token the UUID token from the email link / UUID-Token aus dem E-Mail-Link
     * @throws BusinessException if token is invalid or expired / wenn Token ungültig oder abgelaufen
     */
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new BusinessException(
                        "Invalid verification token / Ungültiger Verifizierungstoken"));

        // Check token expiry / Token-Ablauf prüfen
        if (LocalDateTime.now().isAfter(user.getEmailVerificationTokenExpiry())) {
            throw new BusinessException(
                    "Verification token expired — request a new one / " +
                    "Verifizierungstoken abgelaufen — neuen anfordern");
        }

        // Mark as verified and clear the token / Als verifiziert markieren und Token löschen
        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationTokenExpiry(null);
        userRepository.save(user);

        log.info("Email verified for user: {} / E-Mail verifiziert für Benutzer: {}", user.getEmail(), user.getEmail());
    }

    /**
     * Generates a new verification token and resends the email.
     * Generiert einen neuen Verifizierungstoken und sendet die E-Mail erneut.
     *
     * @param email the user's email address / E-Mail-Adresse des Benutzers
     */
    public void resendVerification(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found / Benutzer nicht gefunden"));

        if (user.isEmailVerified()) {
            throw new BusinessException("Email already verified / E-Mail bereits verifiziert");
        }

        // Generate a fresh token / Neuen Token generieren
        String newToken = UUID.randomUUID().toString();
        user.setEmailVerificationToken(newToken);
        user.setEmailVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        emailService.resendVerificationEmail(user, newToken);
        log.info("Verification email resent to: {} / Verifizierungs-E-Mail erneut gesendet an: {}", email, email);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Cookie utilities / Cookie-Hilfsmethoden
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Sets both access and refresh token cookies on the response.
     * Setzt sowohl Access- als auch Refresh-Token-Cookies in der Antwort.
     */
    private void setTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        addCookie(response, ACCESS_TOKEN_COOKIE, accessToken,
                (int) (appProperties.getJwt().getAccessTokenExpiration() / 1000));
        addCookie(response, REFRESH_TOKEN_COOKIE, refreshToken,
                (int) (appProperties.getJwt().getRefreshTokenExpiration() / 1000));
    }

    /**
     * Clears both token cookies by setting them to empty with maxAge=0.
     * Löscht beide Token-Cookies durch Setzen auf leer mit maxAge=0.
     */
    private void clearTokenCookies(HttpServletResponse response) {
        addCookie(response, ACCESS_TOKEN_COOKIE, "", 0);
        addCookie(response, REFRESH_TOKEN_COOKIE, "", 0);
    }

    /**
     * Adds a single httpOnly, secure cookie to the response.
     * Fügt der Antwort ein einzelnes httpOnly, sicheres Cookie hinzu.
     *
     * @param response HTTP response / HTTP-Antwort
     * @param name     cookie name / Cookie-Name
     * @param value    cookie value / Cookie-Wert
     * @param maxAge   cookie max age in seconds / Cookie-Maximalalter in Sekunden
     */
    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        // Build Set-Cookie header manually to control all attributes precisely.
        // Set-Cookie-Header manuell erstellen, um alle Attribute präzise zu steuern.
        //
        // secureCookie=false for localhost (HTTP), true for production (HTTPS).
        // secureCookie=false für localhost (HTTP), true für Produktion (HTTPS).
        //
        // Frontend (rentmybike.xyz) and backend (railway.app) are different
        // registrable domains, so this is a cross-site request from the
        // browser's point of view. Cross-site cookies are ONLY sent by the
        // browser when SameSite=None (combined with Secure) — both Strict
        // and Lax are stripped from XHR/fetch requests across origins.
        // Frontend (rentmybike.xyz) und Backend (railway.app) sind unterschiedliche
        // Domains, daher ist dies aus Sicht des Browsers ein Cross-Site-Request.
        // Cross-Site-Cookies werden vom Browser nur bei SameSite=None (zusammen
        // mit Secure) gesendet — sowohl Strict als auch Lax werden bei
        // XHR/fetch-Anfragen über Origins hinweg entfernt.
        boolean secure = appProperties.isSecureCookie();
        String sameSite = secure ? "None" : "Lax";

        String header = String.format(
                "%s=%s; Max-Age=%d; Path=/; HttpOnly%s; SameSite=%s",
                name, value, maxAge,
                secure ? "; Secure" : "",
                sameSite
        );
        response.addHeader("Set-Cookie", header);
    }

    /**
     * Extracts a cookie value by name from the request.
     * Extrahiert einen Cookie-Wert anhand des Namens aus der Anfrage.
     *
     * @return cookie value or null if not found / Cookie-Wert oder null wenn nicht gefunden
     */
    private String extractCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> cookieName.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Response builders / Antwort-Builder
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Constructs an AuthResponse from a User entity.
     * Erstellt eine AuthResponse aus einer User-Entity.
     */
    private AuthResponse buildAuthResponse(User user) {
        return AuthResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
