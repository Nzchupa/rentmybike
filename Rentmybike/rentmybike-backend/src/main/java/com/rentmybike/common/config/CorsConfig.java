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

        // Allow only the configured frontend URL (no wildcards when credentials=true)
        // Nur die konfigurierte Frontend-URL erlauben (keine Wildcards wenn credentials=true)
        config.setAllowedOrigins(List.of(
                appProperties.getFrontendUrl(),   // Vercel production / Vercel-Produktion
                "http://localhost:3000"            // Local dev / Lokale Entwicklung
        ));

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
}
