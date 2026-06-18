package com.rentmybike.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS configuration — allows the Next.js frontend (Vercel) to call the backend (Railway).
 * CORS-Konfiguration — ermöglicht dem Next.js Frontend (Vercel) den Aufruf des Backends (Railway).
 *
 * <p>Critical settings for httpOnly cookie flow:
 * <p>Kritische Einstellungen für den httpOnly-Cookie-Ablauf:
 * <ul>
 *   <li>{@code allowCredentials(true)} — required for cookies to be sent cross-origin</li>
 *   <li>{@code allowedOrigins} — must be exact, NOT "*" when credentials=true</li>
 * </ul>
 */
@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private final AppProperties appProperties;

    /**
     * Defines CORS rules applied globally to all API endpoints.
     * Definiert CORS-Regeln, die global für alle API-Endpunkte gelten.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow the configured frontend URL plus all Vercel preview deployments for this
        // project (each push gets a unique *-nzchupas-projects.vercel.app URL).
        // setAllowedOriginPatterns supports wildcards and still works with credentials=true,
        // unlike setAllowedOrigins("*").
        // Erlaubt die konfigurierte Frontend-URL sowie alle Vercel-Preview-Deployments dieses
        // Projekts (jeder Push erhält eine eigene *-nzchupas-projects.vercel.app URL).
        config.setAllowedOriginPatterns(allowedOriginPatterns());

        // Allow standard HTTP methods used by the REST API
        // Standardmäßige HTTP-Methoden für die REST-API erlauben
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Allow all request headers / Alle Anfrage-Header erlauben
        config.setAllowedHeaders(List.of("*"));

        // CRITICAL: must be true for httpOnly cookies to be sent with cross-origin requests
        // KRITISCH: muss true sein, damit httpOnly-Cookies bei Cross-Origin-Anfragen gesendet werden
        config.setAllowCredentials(true);

        // Cache preflight response for 1 hour / Preflight-Antwort 1 Stunde cachen
        config.setMaxAge(3600L);

        // Apply this CORS config to all API routes
        // Diese CORS-Konfiguration auf alle API-Routen anwenden
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    /**
     * The shared allowed-origin pattern list — extracted so other components
     * that need their own origin check (e.g. {@link WebSocketConfig}'s STOMP/
     * SockJS endpoint, which is registered outside the {@code /api/**} CORS
     * mapping above) can reuse the exact same allowlist instead of drifting
     * out of sync with it.
     * Die gemeinsame Liste erlaubter Origin-Muster — ausgelagert, damit
     * andere Komponenten mit eigener Origin-Prüfung (z. B. der STOMP-/SockJS-
     * Endpunkt von {@link WebSocketConfig}, der außerhalb der obigen
     * {@code /api/**}-CORS-Zuordnung registriert wird) dieselbe Allowlist
     * wiederverwenden können, statt davon abzuweichen.
     */
    public List<String> allowedOriginPatterns() {
        return List.of(
                appProperties.getFrontendUrl(),                          // Vercel production / Vercel-Produktion
                "https://rentmybike.xyz",                                 // Custom domain (apex) / Eigene Domain (apex)
                "https://www.rentmybike.xyz",                             // Custom domain (www) / Eigene Domain (www)
                "https://rentmybike-*-nzchupas-projects.vercel.app",     // Vercel previews / Vercel-Vorschauen
                "http://localhost:3000"                                   // Local dev / Lokale Entwicklung
        );
    }
}
