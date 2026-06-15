package com.rentmybike.auth.filter;

import com.rentmybike.auth.service.AuthService;
import com.rentmybike.auth.service.JwtService;
import com.rentmybike.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

/**
 * Spring Security filter that reads the JWT access token from the httpOnly cookie
 * and populates the SecurityContext for each incoming request.
 *
 * Spring Security Filter, der den JWT-Zugriffstoken aus dem httpOnly-Cookie liest
 * und den SecurityContext für jede eingehende Anfrage befüllt.
 *
 * <p>This filter runs ONCE PER REQUEST before the UsernamePasswordAuthenticationFilter.
 * <p>Dieser Filter wird EINMAL PRO ANFRAGE vor dem UsernamePasswordAuthenticationFilter ausgeführt.
 *
 * <p>Flow / Ablauf:
 * <ol>
 *   <li>Extract "access_token" cookie / "access_token"-Cookie extrahieren</li>
 *   <li>Validate JWT signature and expiry / JWT-Signatur und Ablauf validieren</li>
 *   <li>Load user from DB by UUID in subject claim / Benutzer aus DB via UUID im Subject laden</li>
 *   <li>Set authentication in SecurityContext / Authentifizierung im SecurityContext setzen</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Skip if user is already authenticated in this request cycle
        // Überspringen, wenn der Benutzer in diesem Anfragezyklus bereits authentifiziert ist
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Try to extract the access token cookie / Versuche, das Access-Token-Cookie zu extrahieren
        String accessToken = extractCookieValue(request, AuthService.ACCESS_TOKEN_COOKIE);

        // No cookie present — continue as anonymous / Kein Cookie vorhanden — als anonym fortfahren
        if (accessToken == null || accessToken.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Validate token structure, signature and expiry
        // Token-Struktur, Signatur und Ablauf validieren
        if (!jwtService.isAccessTokenValid(accessToken)) {
            log.debug("Invalid or expired access token on {} {} / Ungültiger oder abgelaufener Zugriffstoken bei {} {}",
                    request.getMethod(), request.getRequestURI(),
                    request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extract user ID from token subject / Benutzer-ID aus Token-Subject extrahieren
            UUID userId = jwtService.extractUserId(accessToken);

            // Load full user entity from database / Vollständige Benutzer-Entity aus Datenbank laden
            UserDetails userDetails = userRepository.findById(userId).orElse(null);

            if (userDetails == null || !userDetails.isEnabled()) {
                // User deleted or banned since token was issued
                // Benutzer gelöscht oder gesperrt seit Token-Ausstellung
                filterChain.doFilter(request, response);
                return;
            }

            // Build authentication token and set in SecurityContext
            // Authentifizierungstoken erstellen und im SecurityContext setzen
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            // Any parsing error — proceed as unauthenticated
            // Jeder Parsing-Fehler — als nicht authentifiziert fortfahren
            log.warn("JWT processing error: {} / JWT-Verarbeitungsfehler: {}", e.getMessage(), e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts a cookie value by name from the request.
     * Extrahiert einen Cookie-Wert anhand des Namens aus der Anfrage.
     *
     * @param request    the HTTP request / die HTTP-Anfrage
     * @param cookieName the name of the cookie / der Name des Cookies
     * @return cookie value or null if not present / Cookie-Wert oder null wenn nicht vorhanden
     */
    private String extractCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        return Arrays.stream(cookies)
                .filter(c -> cookieName.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
