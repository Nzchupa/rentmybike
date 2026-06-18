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
import org.springframework.dao.DataIntegrityViolationException;
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
     * and sends a verification email.
     * Registriert einen neuen Benutzer, speichert ihn in der Datenbank (unverifiziert)
     * und sendet eine Verifizierungs-E-Mail.
     *
     * <p>The pre-check ({@code existsByEmail}) and the insert are two separate
     * statements, so two concurrent registrations with the same email can both
     * pass the check before either commits (TOCTOU). The DB's unique constraint
     * on {@code email} is the real guard — we just translate the resulting
     * {@link DataIntegrityViolationException} into a clean 400 instead of
     * letting it bubble up as an unhandled 500.
     * <p>Die Vorabprüfung ({@code existsByEmail}) und das Einfügen sind zwei
     * getrennte Anweisungen, sodass zwei gleichzeitige Registrierungen mit
     * derselben E-Mail beide die Prüfung bestehen können, bevor eine von beiden
     * committet (TOCTOU). Der eindeutige DB-Constraint auf {@code email} ist die
     * eigentliche Absicherung — wir übersetzen die resultierende
     * {@link DataIntegrityViolationException} lediglich in ein sauberes 400,
     * statt sie als unbehandeltes 500 durchschlagen zu lassen.
     *
     * @param request registration data / Registrierungsdaten
     * @return true if the verification email was actually sent (false if the
     *         user was created but the email failed, or if auto-verify is on
     *         and no email was needed) / true, wenn die Verifizierungs-E-Mail
     *         tatsächlich gesendet wurde (false, wenn der Benutzer erstellt wurde,
     *         die E-Mail aber fehlschlug, oder wenn Auto-Verify aktiv ist und
     *         keine E-Mail nötig war)
     * @throws BusinessException if email is already taken / wenn E-Mail bereits vergeben
     */
    public boolean register(RegisterRequest request) {
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

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // Another request won the race and registered this email first /
            // Eine andere Anfrage hat das Rennen gewonnen und diese E-Mail zuerst registriert
            throw new BusinessException("Email already registered / E-Mail bereits registriert: " + request.getEmail());
        }

        log.info("New user registered{}: {} / Neuer Benutzer registriert{}: {}",
                autoVerify ? " (auto-verified)" : "",
                user.getEmail(),
                autoVerify ? " (auto-verifiziert)" : "",
                user.getEmail());

        // Send verification email only when not auto-verifying / Verifizierungs-E-Mail nur senden, wenn nicht auto-verifiziert
        if (autoVerify) {
            return false;
        }
        boolean emailSent = emailService.sendVerificationEmail(user, verificationToken);
        if (!emailSent) {
            log.error("Verification email failed to send for new user: {} — account was still created / "
                    + "Verifizierungs-E-Mail für neuen Benutzer konnte nicht gesendet werden: {} — Konto wurde trotzdem erstellt",
                    user.getEmail(), user.getEmail());
        }
        return emailSent;
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
     * Clears auth cookies and revokes every outstanding access/refresh token
     * server-side, effectively logging the user out everywhere.
     * Löscht Auth-Cookies und widerruft serverseitig alle ausstehenden
     * Access-/Refresh-Token, wodurch der Benutzer überall abgemeldet wird.
     *
     * <p>Clearing cookies alone is not enough: a copy of the refresh token
     * (e.g. exfiltrated via XSS, or simply replayed by a malicious client
     * that ignored the Set-Cookie deletion) would otherwise stay valid for
     * its full 7-day lifetime. Bumping tokenVersion makes every previously
     * issued token — access or refresh — fail validation immediately.
     * <p>Das alleinige Löschen der Cookies reicht nicht aus: Eine Kopie des
     * Refresh-Tokens (z. B. über XSS exfiltriert oder von einem böswilligen
     * Client erneut verwendet, der die Set-Cookie-Löschung ignoriert) würde
     * sonst für ihre volle 7-Tage-Lebensdauer gültig bleiben. Das Erhöhen von
     * tokenVersion lässt jeden zuvor ausgestellten Token sofort ungültig werden.
     *
     * @param userId   the currently authenticated user, or null if already anonymous /
     *                 der aktuell authentifizierte Benutzer, oder null wenn bereits anonym
     * @param response HTTP response to clear cookies on / HTTP-Antwort zum Löschen der Cookies
     */
    public void logout(UUID userId, HttpServletResponse response) {
        if (userId != null) {
            userRepository.findById(userId).ifPresent(user -> {
                user.setTokenVersion(user.getTokenVersion() + 1);
                userRepository.save(user);
            });
        }

        // Clear both tokens by setting expired cookies / Beide Token durch abgelaufene Cookies löschen
        clearTokenCookies(response);
        log.debug("User logged out — cookies cleared and tokens revoked / Benutzer abgemeldet — Cookies gelöscht und Token widerrufen");
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

        if (!user.isEnabled() || !user.isAccountNonLocked()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is inactive / Konto ist inaktiv");
        }

        // Reject refresh tokens issued before the user's last logout (or any
        // other event that bumps tokenVersion) — see logout() above.
        // Refresh-Token ablehnen, die vor dem letzten Logout des Benutzers
        // ausgestellt wurden (oder einem anderen Ereignis, das tokenVersion
        // erhöht) — siehe logout() oben.
        if (jwtService.extractTokenVersion(refreshToken) != user.getTokenVersion()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Refresh token has been revoked / Aktualisierungstoken wurde widerrufen");
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
     * @throws BusinessException if the user is not found, already verified, or
     *                            if the email provider failed to accept the
     *                            resend / wenn der Benutzer nicht gefunden,
     *                            bereits verifiziert ist, oder der E-Mail-Anbieter
     *                            den erneuten Versand nicht akzeptiert hat
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

        boolean emailSent = emailService.resendVerificationEmail(user, newToken);
        if (!emailSent) {
            // Don't tell the user "email sent" when it wasn't — let them retry.
            // Dem Benutzer nicht "E-Mail gesendet" sagen, wenn es nicht stimmt — erneuten Versuch ermöglichen.
            throw new BusinessException(
                    "Failed to send verification email — please try again later / "
                    + "Verifizierungs-E-Mail konnte nicht gesendet werden — bitte später erneut versuchen");
        }
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

        // Without an explicit Domain attribute these cookies are host-only —
        // scoped to api.rentmybike.xyz only. That's fine for the backend
        // itself (it's the only one reading the JWT), but our Next.js
        // middleware (middleware.ts) runs on the frontend host
        // (rentmybike.xyz) and checks request.cookies.has("access_token") to
        // guard /dashboard and /admin — a request to rentmybike.xyz never
        // carries a cookie scoped to api.rentmybike.xyz, so that check always
        // saw "no session" and redirected straight to /auth/login even for
        // logged-in users. Setting the same shared Domain used for the
        // XSRF-TOKEN cookie (app.cookie-domain) makes access_token/
        // refresh_token visible to requests on rentmybike.xyz too.
        //
        // Ohne explizites Domain-Attribut sind diese Cookies host-only — nur
        // für api.rentmybike.xyz gültig. Das ist für das Backend selbst kein
        // Problem, aber unsere Next.js-Middleware (middleware.ts) läuft auf
        // dem Frontend-Host (rentmybike.xyz) und prüft
        // request.cookies.has("access_token"), um /dashboard und /admin zu
        // schützen — eine Anfrage an rentmybike.xyz trägt nie ein Cookie, das
        // für api.rentmybike.xyz reserviert ist, daher sah diese Prüfung
        // immer "keine Sitzung" und leitete selbst eingeloggte Benutzer sofort
        // zu /auth/login um. Die gleiche gemeinsame Domain wie beim
        // XSRF-TOKEN-Cookie (app.cookie-domain) zu setzen, macht access_token/
        // refresh_token auch für Anfragen an rentmybike.xyz sichtbar.
        String cookieDomain = appProperties.getCookieDomain();
        String domainAttr = (cookieDomain != null && !cookieDomain.isBlank())
                ? "; Domain=" + cookieDomain
                : "";

        String header = String.format(
                "%s=%s; Max-Age=%d; Path=/; HttpOnly%s%s; SameSite=%s",
                name, value, maxAge,
                secure ? "; Secure" : "",
                domainAttr,
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
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .banned(user.isBanned())
                .businessName(user.getBusinessName())
                .businessVerified(user.isBusinessVerified())
                .build();
    }
}
