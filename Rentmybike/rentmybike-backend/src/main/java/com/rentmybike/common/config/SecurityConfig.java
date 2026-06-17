package com.rentmybike.common.config;

import com.rentmybike.auth.filter.JwtAuthenticationFilter;
import com.rentmybike.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security configuration for the RentMyBike application.
 * Spring Security-Konfiguration für die RentMyBike-Anwendung.
 *
 * <p>Security model / Sicherheitsmodell:
 * <ul>
 *   <li>Stateless JWT via httpOnly cookies / Zustandslos mit JWT via httpOnly-Cookies</li>
 *   <li>Cookie-based double-submit CSRF protection for state-changing requests
 *       (the JWT cookies are SameSite=None in production, since the frontend
 *       and backend are on different origins — SameSite alone does NOT block
 *       cross-site requests in that mode, so CSRF must be handled explicitly)
 *       / Cookie-basierter Double-Submit-CSRF-Schutz für zustandsändernde
 *       Anfragen (die JWT-Cookies sind in der Produktion SameSite=None, da
 *       Frontend und Backend auf unterschiedlichen Origins liegen — SameSite
 *       allein blockiert in diesem Modus KEINE Cross-Site-Anfragen, daher
 *       muss CSRF explizit behandelt werden)</li>
 *   <li>No session (STATELESS) / Keine Session (STATELESS)</li>
 *   <li>Public: auth endpoints, GET bikes/home / Öffentlich: Auth-Endpunkte, GET Bikes/Home</li>
 *   <li>Protected: all other endpoints / Geschützt: alle anderen Endpunkte</li>
 *   <li>Admin: /api/v1/admin/** only for ADMIN role / Admin: /api/v1/admin/** nur für ADMIN-Rolle</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // Enables @PreAuthorize, @Secured on service/controller methods
                               // Aktiviert @PreAuthorize, @Secured auf Service-/Controller-Methoden
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserRepository userRepository;
    private final CorsConfigurationSource corsConfigurationSource;

    // ──────────────────────────────────────────────────────────────────────────
    // Security filter chain / Sicherheits-Filter-Kette
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Configures HTTP security rules, session policy and filter chain.
     * Konfiguriert HTTP-Sicherheitsregeln, Session-Richtlinie und Filter-Kette.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ── Enable CORS using the rules defined in CorsConfig — without this,
            //    Spring Security never adds Access-Control-Allow-Origin headers,
            //    no matter what's configured in CorsConfigurationSource.
            // ── CORS mit den in CorsConfig definierten Regeln aktivieren — ohne dies
            //    fügt Spring Security niemals Access-Control-Allow-Origin-Header hinzu,
            //    unabhängig davon, was in CorsConfigurationSource konfiguriert ist.
            .cors(cors -> cors.configurationSource(corsConfigurationSource))

            // ── Cookie-based double-submit CSRF protection. The JWT auth
            //    cookies are SameSite=None in production (cross-origin
            //    frontend/backend), so they ARE sent on cross-site requests —
            //    the comment that used to justify disabling CSRF here
            //    ("cookies are SameSite=Strict") did not match the actual
            //    cookie code in AuthService, which sets None/Lax depending on
            //    environment. CookieCsrfTokenRepository issues a readable
            //    XSRF-TOKEN cookie; the SPA echoes it back as the
            //    X-XSRF-TOKEN header (axios does this automatically via
            //    withXSRFToken), and CsrfFilter rejects state-changing
            //    requests where the two don't match — a header an attacker's
            //    cross-site form/script cannot read or set.
            //    Pure-credential auth endpoints are exempted: they don't yet
            //    have an authenticated session to forge, and /refresh is
            //    invoked transparently by the axios interceptor.
            //
            // ── Cookie-basierter Double-Submit-CSRF-Schutz. Die JWT-Auth-
            //    Cookies sind in der Produktion SameSite=None (Frontend und
            //    Backend auf unterschiedlichen Origins), werden also bei
            //    Cross-Site-Anfragen MITGESENDET — der Kommentar, der das
            //    Deaktivieren von CSRF früher begründete ("Cookies sind
            //    SameSite=Strict"), stimmte nicht mit dem tatsächlichen
            //    Cookie-Code in AuthService überein, der je nach Umgebung
            //    None/Lax setzt. CookieCsrfTokenRepository stellt ein
            //    lesbares XSRF-TOKEN-Cookie aus; die SPA sendet es als
            //    X-XSRF-TOKEN-Header zurück (axios macht das automatisch via
            //    withXSRFToken), und CsrfFilter lehnt zustandsändernde
            //    Anfragen ab, bei denen beide nicht übereinstimmen — ein
            //    Header, den ein Cross-Site-Formular/Skript eines Angreifers
            //    nicht lesen oder setzen kann.
            //    Reine Credential-Auth-Endpunkte sind ausgenommen: Sie haben
            //    noch keine authentifizierte Sitzung, die gefälscht werden
            //    könnte, und /refresh wird transparent vom Axios-Interceptor
            //    aufgerufen.
            .csrf(csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                    .ignoringRequestMatchers(
                            "/api/v1/auth/login",
                            "/api/v1/auth/register",
                            "/api/v1/auth/refresh",
                            "/api/v1/auth/verify-email",
                            "/api/v1/auth/resend-verification",
                            "/actuator/**"
                    )
            )
            .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)

            // ── Stateless session — no HttpSession will be created
            // ── Zustandslose Session — kein HttpSession wird erstellt
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── Authorization rules / Autorisierungsregeln ──────────────────
            .authorizeHttpRequests(auth -> auth

                // Health check for Railway / Gesundheitsprüfung für Railway
                .requestMatchers("/actuator/**").permitAll()

                // Public auth endpoints / Öffentliche Auth-Endpunkte
                .requestMatchers("/api/v1/auth/**").permitAll()

                // Public bike browsing — read-only / Öffentliches Fahrrad-Browsen — nur lesen
                .requestMatchers(HttpMethod.GET, "/api/v1/bikes").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/bikes/{id}").permitAll()

                // Public booked-date ranges — lets the booking calendar disable
                // taken dates before the user is even logged in / Öffentliche
                // belegte Datumsbereiche — ermöglicht es dem Buchungskalender,
                // vergebene Termine zu deaktivieren, bevor der Benutzer angemeldet ist
                .requestMatchers(HttpMethod.GET, "/api/v1/bookings/bike/*/booked-dates").permitAll()

                // Public reviews — bike and user profiles / Öffentliche Bewertungen — Fahrrad- und Benutzerprofile
                .requestMatchers(HttpMethod.GET, "/api/v1/reviews/bike/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/reviews/user/**").permitAll()

                // Public user profiles / Öffentliche Benutzerprofile
                .requestMatchers(HttpMethod.GET, "/api/v1/users/*/public").permitAll()

                // Admin-only endpoints / Nur-Admin-Endpunkte
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                // Everything else requires authentication
                // Alles andere erfordert Authentifizierung
                .anyRequest().authenticated()
            )

            // ── Custom JWT filter BEFORE the default username/password filter
            // ── Benutzerdefinierter JWT-Filter VOR dem Standard-Benutzername/Passwort-Filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            // ── Use custom AuthenticationProvider / Benutzerdefinierten AuthenticationProvider verwenden
            .authenticationProvider(authenticationProvider());

        return http.build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Authentication beans / Authentifizierungs-Beans
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Loads users from the database by email for Spring Security.
     * Lädt Benutzer anhand der E-Mail aus der Datenbank für Spring Security.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return email -> userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + email + " / Benutzer nicht gefunden: " + email));
    }

    /**
     * DAO-based authentication provider wired with our UserDetailsService and BCrypt encoder.
     * DAO-basierter Authentifizierungsanbieter mit unserem UserDetailsService und BCrypt-Encoder.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * BCryptPasswordEncoder — the industry standard for password hashing.
     * BCryptPasswordEncoder — der Industriestandard für Passwort-Hashing.
     *
     * <p>Strength factor 12 — good balance of security and performance on modern hardware.
     * <p>Stärke-Faktor 12 — gute Balance zwischen Sicherheit und Leistung auf moderner Hardware.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Exposes the AuthenticationManager bean needed by AuthService.authenticate().
     * Stellt den AuthenticationManager-Bean bereit, den AuthService.authenticate() benötigt.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
