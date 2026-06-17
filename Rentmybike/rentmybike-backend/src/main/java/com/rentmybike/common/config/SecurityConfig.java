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
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security configuration for the RentMyBike application.
 * Spring Security-Konfiguration für die RentMyBike-Anwendung.
 *
 * <p>Security model / Sicherheitsmodell:
 * <ul>
 *   <li>Stateless JWT via httpOnly cookies / Zustandslos mit JWT via httpOnly-Cookies</li>
 *   <li>No CSRF (cookies are SameSite=Strict) / Kein CSRF (Cookies sind SameSite=Strict)</li>
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

            // ── Disable CSRF — we use SameSite=Strict cookies instead
            // ── CSRF deaktivieren — wir verwenden stattdessen SameSite=Strict-Cookies
            .csrf(AbstractHttpConfigurer::disable)

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
