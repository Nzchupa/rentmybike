package com.rentmybike.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed configuration properties for the RentMyBike application.
 * Stark typisierte Konfigurationseigenschaften für die RentMyBike-Anwendung.
 *
 * <p>Bound from the {@code app:} prefix in application.yml.
 * <p>Gebunden vom {@code app:} Präfix in application.yml.
 *
 * <p>Registered via {@code @EnableConfigurationProperties(AppProperties.class)}
 * in {@link com.rentmybike.RentMyBikeApplication}.
 */
@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /**
     * JWT configuration / JWT-Konfiguration.
     */
    private Jwt jwt = new Jwt();

    /**
     * Frontend URL — used for CORS and email verification links.
     * Frontend-URL — für CORS und E-Mail-Verifizierungslinks verwendet.
     */
    private String frontendUrl = "http://localhost:3000";

    /**
     * Whether to set Secure flag on auth cookies.
     * Must be false for localhost (HTTP), true for production (HTTPS).
     * Ob das Secure-Flag auf Auth-Cookies gesetzt werden soll.
     * Muss false für localhost (HTTP) und true für Produktion (HTTPS) sein.
     */
    private boolean secureCookie = false;

    /**
     * Domain attribute for the readable XSRF-TOKEN cookie (double-submit CSRF).
     * Empty = host-only cookie (fine for localhost, where frontend and backend
     * share the same host). In production, frontend (rentmybike.xyz) and backend
     * (api.rentmybike.xyz) are different hosts under the same registrable domain
     * — a host-only cookie set by the backend is invisible to frontend JS, so
     * axios can never read it to echo it back as X-XSRF-TOKEN, and every
     * state-changing request (e.g. logout) is rejected with 403. Setting this to
     * "rentmybike.xyz" (NO leading dot — Tomcat's Rfc6265CookieProcessor rejects
     * domain values starting with "." as invalid per RFC 6265) makes the cookie
     * shared across subdomains so the SPA can actually read it.
     * Domain-Attribut für das lesbare XSRF-TOKEN-Cookie (Double-Submit-CSRF).
     * Leer = Host-only-Cookie (passt für localhost). In der Produktion sind
     * Frontend (rentmybike.xyz) und Backend (api.rentmybike.xyz) unterschiedliche
     * Hosts — ein Host-only-Cookie des Backends ist für Frontend-JS unsichtbar,
     * daher kann axios es nicht lesen und jede zustandsändernde Anfrage (z. B.
     * Logout) wird mit 403 abgelehnt. "rentmybike.xyz" (OHNE führenden Punkt —
     * Tomcats Rfc6265CookieProcessor lehnt ihn ab) macht das Cookie über
     * Subdomains hinweg gemeinsam nutzbar.
     */
    private String cookieDomain = "";

    /**
     * If true, newly registered users are automatically email-verified.
     * Use only in development — never in production!
     * Wenn true, werden neu registrierte Benutzer automatisch verifiziert.
     * Nur in der Entwicklung verwenden — niemals in der Produktion!
     */
    private boolean autoVerifyEmail = false;

    // ──────────────────────────────────────────────────────────────────────────
    // Nested configuration classes / Verschachtelte Konfigurationsklassen
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * JWT-specific settings / JWT-spezifische Einstellungen.
     */
    @Data
    public static class Jwt {

        /**
         * HMAC-SHA256 secret key — must be at least 64 characters in production.
         * HMAC-SHA256-Geheimschlüssel — muss in der Produktion mindestens 64 Zeichen lang sein.
         *
         * <p>Set via JWT_SECRET environment variable in Railway.
         * <p>Wird über die JWT_SECRET Umgebungsvariable in Railway gesetzt.
         */
        private String secret = "change-this-in-production-must-be-at-least-64-characters-long!!";

        /**
         * Access token lifetime in milliseconds (default: 15 minutes).
         * Zugriffstoken-Lebensdauer in Millisekunden (Standard: 15 Minuten).
         */
        private long accessTokenExpiration = 900_000L;       // 15 min

        /**
         * Refresh token lifetime in milliseconds (default: 7 days).
         * Aktualisierungstoken-Lebensdauer in Millisekunden (Standard: 7 Tage).
         */
        private long refreshTokenExpiration = 604_800_000L;  // 7 days
    }
}
