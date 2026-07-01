package com.rentmybike.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentmybike.common.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory, per-IP rate limiter for the handful of unauthenticated auth
 * endpoints that would otherwise be wide open to brute-force / spam abuse
 * (login credential guessing, mass fake registrations, verification-email
 * bombing). Not applied anywhere else — every other endpoint either requires
 * a valid JWT already or is a low-risk public GET.
 *
 * In-Memory-Ratenbegrenzer pro IP für die wenigen unauthentifizierten
 * Auth-Endpunkte, die sonst offen für Brute-Force-/Spam-Missbrauch wären
 * (Login-Passwortraten, massenhafte Fake-Registrierungen, Verifizierungs-
 * E-Mail-Bombing). Wird sonst nirgends angewendet — jeder andere Endpunkt
 * erfordert entweder bereits ein gültiges JWT oder ist ein risikoarmes
 * öffentliches GET.
 *
 * <p>Fixed-window counter keyed by (client IP, path), held in a plain
 * {@link ConcurrentHashMap}. This is intentionally simple and good enough for
 * a single-instance deployment (e.g. one Railway service) — it does NOT
 * coordinate across multiple app instances. If this app is ever scaled
 * horizontally behind a load balancer, this would need to move to a shared
 * store (e.g. Redis) so all instances see the same counters.
 * <p>Fixed-Window-Zähler, geschlüsselt nach (Client-IP, Pfad), in einer
 * einfachen {@link ConcurrentHashMap}. Bewusst einfach gehalten und für ein
 * Einzelinstanz-Deployment ausreichend — koordiniert NICHT über mehrere
 * App-Instanzen hinweg. Bei horizontaler Skalierung hinter einem Load
 * Balancer müsste dies auf einen gemeinsamen Speicher (z. B. Redis) umziehen.
 */
@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Rate-limited path -> its (max requests, window length) rule. */
    private static final Map<String, Limit> LIMITS = Map.of(
            "/api/v1/auth/login", new Limit(10, 5 * 60_000L),               // 10 tries / 5 min per IP
            "/api/v1/auth/register", new Limit(5, 15 * 60_000L),            // 5 signups / 15 min per IP
            "/api/v1/auth/resend-verification", new Limit(5, 15 * 60_000L)  // 5 resends / 15 min per IP
    );

    /** How long a stale window is kept around before the periodic sweep evicts it. */
    private static final long SWEEP_MAX_AGE_MILLIS = 30 * 60_000L;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        Limit limit = LIMITS.get(request.getRequestURI());
        if (limit == null) {
            // Not a rate-limited path — no counter work, straight through.
            // Kein ratenbegrenzter Pfad — keine Zählerarbeit, direkt durch.
            filterChain.doFilter(request, response);
            return;
        }

        String key = clientIp(request) + '|' + request.getRequestURI();
        long now = System.currentTimeMillis();

        Window window = windows.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStart >= limit.windowMillis()) {
                return new Window(now);
            }
            existing.count.incrementAndGet();
            return existing;
        });

        // Cheap opportunistic cleanup — with ~0.5% probability per rate-limited
        // request, sweep out windows old enough that they can no longer matter,
        // so the map doesn't grow unbounded as new client IPs show up over the
        // app's lifetime. No dedicated @Scheduled job needed for this.
        // Günstige, opportunistische Bereinigung — mit ~0,5% Wahrscheinlichkeit
        // pro ratenbegrenzter Anfrage werden hinreichend alte Fenster entfernt,
        // damit die Map nicht unbegrenzt wächst. Kein eigener @Scheduled-Job
        // dafür nötig.
        if (ThreadLocalRandom.current().nextInt(200) == 0) {
            long cutoff = now - SWEEP_MAX_AGE_MILLIS;
            windows.values().removeIf(w -> w.windowStart < cutoff);
        }

        if (window.count.get() > limit.maxRequests()) {
            log.warn("Rate limit exceeded: {} on {} / Ratenlimit überschritten: {} auf {}",
                    key, request.getRequestURI(), key, request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(MAPPER.writeValueAsString(ApiResponse.error(
                    "Too many requests — please wait a bit and try again / "
                    + "Zu viele Anfragen — bitte warte kurz und versuche es erneut")));
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Same first-hop client IP resolution used by ContractController's
     * click-to-accept audit trail — first entry of X-Forwarded-For when
     * behind a proxy/load balancer (Railway), otherwise the direct remote
     * address.
     * Gleiche First-Hop-Client-IP-Auflösung wie im Click-to-Accept-Audit-Trail
     * von ContractController — erster Eintrag von X-Forwarded-For hinter
     * einem Proxy/Load Balancer (Railway), sonst die direkte Remote-Adresse.
     */
    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record Limit(int maxRequests, long windowMillis) {}

    private static final class Window {
        final long windowStart;
        final AtomicInteger count = new AtomicInteger(1);

        Window(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}
