package com.rentmybike.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Forces resolution of the lazily-supplied CSRF token on every request so
 * that {@link org.springframework.security.web.csrf.CookieCsrfTokenRepository}
 * actually writes the XSRF-TOKEN cookie. Without this, Spring Security only
 * generates the token (and the response cookie) the first time something
 * calls {@code CsrfToken#getToken()} — which, for a pure-JSON API with no
 * server-rendered form, never happens on its own, so the frontend would
 * never receive a token to echo back.
 *
 * Erzwingt die Auflösung des verzögert bereitgestellten CSRF-Tokens bei jeder
 * Anfrage, damit {@link org.springframework.security.web.csrf.CookieCsrfTokenRepository}
 * tatsächlich das XSRF-TOKEN-Cookie schreibt. Ohne dies generiert Spring
 * Security das Token (und das Antwort-Cookie) nur, wenn etwas
 * {@code CsrfToken#getToken()} aufruft — was bei einer reinen JSON-API ohne
 * serverseitig gerenderte Formulare nie von selbst passiert, sodass das
 * Frontend nie ein Token zum Zurücksenden erhalten würde.
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            // Triggers the deferred supplier, which is what actually causes
            // CookieCsrfTokenRepository to write the Set-Cookie header.
            // Löst den verzögerten Supplier aus, was CookieCsrfTokenRepository
            // tatsächlich veranlasst, den Set-Cookie-Header zu schreiben.
            csrfToken.getToken();
        }
        filterChain.doFilter(request, response);
    }
}
