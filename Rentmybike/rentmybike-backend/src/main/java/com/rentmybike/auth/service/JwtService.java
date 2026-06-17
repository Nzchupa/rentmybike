package com.rentmybike.auth.service;

import com.rentmybike.common.config.AppProperties;
import com.rentmybike.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Service for generating and validating JWT access and refresh tokens.
 * Service zur Generierung und Validierung von JWT-Zugriffs- und Aktualisierungstoken.
 *
 * <p>Tokens are stored in httpOnly cookies — never exposed to JavaScript.
 * <p>Token werden in httpOnly-Cookies gespeichert — niemals für JavaScript zugänglich.
 *
 * <p>Claims structure / Anspruchsstruktur:
 * <ul>
 *   <li>sub  — user UUID (string)</li>
 *   <li>role — UserRole name (USER | ADMIN)</li>
 *   <li>type — "access" | "refresh"</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_TOKEN_VERSION = "tv";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final AppProperties appProperties;

    // ──────────────────────────────────────────────────────────────────────────
    // Token generation / Token-Generierung
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Generates a short-lived access token (default 15 minutes).
     * Generiert einen kurzlebigen Zugriffstoken (Standard 15 Minuten).
     *
     * @param user the authenticated user / der authentifizierte Benutzer
     * @return signed JWT string / signierter JWT-String
     */
    public String generateAccessToken(User user) {
        return buildToken(user, appProperties.getJwt().getAccessTokenExpiration(), TYPE_ACCESS);
    }

    /**
     * Generates a long-lived refresh token (default 7 days).
     * Generiert einen langlebigen Aktualisierungstoken (Standard 7 Tage).
     *
     * @param user the authenticated user / der authentifizierte Benutzer
     * @return signed JWT string / signierter JWT-String
     */
    public String generateRefreshToken(User user) {
        return buildToken(user, appProperties.getJwt().getRefreshTokenExpiration(), TYPE_REFRESH);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Token parsing / Token-Auswertung
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Extracts the user's UUID from a JWT token.
     * Extrahiert die UUID des Benutzers aus einem JWT-Token.
     *
     * @param token JWT string / JWT-String
     * @return user UUID / Benutzer-UUID
     */
    public UUID extractUserId(String token) {
        return UUID.fromString(extractClaims(token).getSubject());
    }

    /**
     * Extracts the role claim from a JWT token.
     * Extrahiert den Rollenanspruch aus einem JWT-Token.
     *
     * @param token JWT string / JWT-String
     * @return role name string (e.g. "USER") / Rollenname-String (z.B. "USER")
     */
    public String extractRole(String token) {
        return extractClaims(token).get(CLAIM_ROLE, String.class);
    }

    /**
     * Extracts the token-version claim used for server-side revocation.
     * Extrahiert den Token-Versions-Anspruch zur serverseitigen Widerrufsprüfung.
     *
     * @param token JWT string / JWT-String
     * @return token version the user had when this token was issued / Token-Version bei Ausstellung
     */
    public int extractTokenVersion(String token) {
        Integer version = extractClaims(token).get(CLAIM_TOKEN_VERSION, Integer.class);
        return version == null ? 0 : version;
    }

    /**
     * Validates that the token is well-formed, not expired, and matches the expected type.
     * Validiert, dass der Token wohlgeformt, nicht abgelaufen ist und dem erwarteten Typ entspricht.
     *
     * @param token        JWT string / JWT-String
     * @param expectedType "access" or "refresh" / "access" oder "refresh"
     * @return true if valid / true wenn gültig
     */
    public boolean isTokenValid(String token, String expectedType) {
        try {
            Claims claims = extractClaims(token);
            boolean notExpired = claims.getExpiration().after(new Date());
            boolean correctType = expectedType.equals(claims.get(CLAIM_TYPE, String.class));
            return notExpired && correctType;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {} / JWT-Validierung fehlgeschlagen: {}", e.getMessage(), e.getMessage());
            return false;
        }
    }

    /**
     * Validates an access token specifically.
     * Validiert speziell einen Zugriffstoken.
     */
    public boolean isAccessTokenValid(String token) {
        return isTokenValid(token, TYPE_ACCESS);
    }

    /**
     * Validates a refresh token specifically.
     * Validiert speziell einen Aktualisierungstoken.
     */
    public boolean isRefreshTokenValid(String token) {
        return isTokenValid(token, TYPE_REFRESH);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers / Private Hilfsmethoden
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Builds and signs a JWT with the given expiration and type.
     * Erstellt und signiert einen JWT mit der angegebenen Ablaufzeit und dem Typ.
     */
    private String buildToken(User user, long expirationMs, String type) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_TYPE, type)
                .claim(CLAIM_TOKEN_VERSION, user.getTokenVersion())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Parses and returns all claims from a JWT token.
     * Parst und gibt alle Ansprüche aus einem JWT-Token zurück.
     *
     * @throws JwtException if token is invalid or expired / wenn Token ungültig oder abgelaufen
     */
    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Derives the HMAC-SHA256 signing key from the configured secret.
     * Leitet den HMAC-SHA256-Signierschlüssel aus dem konfigurierten Secret ab.
     * The secret must be at least 64 characters for HS256 security.
     * Das Secret muss mindestens 64 Zeichen lang sein für HS256-Sicherheit.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = appProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
