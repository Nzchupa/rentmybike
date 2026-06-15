package com.rentmybike.user.entity;

import com.rentmybike.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * User entity — represents both bike owners and renters on the platform.
 * Benutzer-Entität — repräsentiert sowohl Fahrrad-Eigentümer als auch Mieter auf der Plattform.
 *
 * <p>One account can act as both owner and renter simultaneously.
 * <p>Ein Konto kann gleichzeitig als Eigentümer und Mieter agieren.
 *
 * <p>Implements {@link UserDetails} for Spring Security integration.
 * <p>Implementiert {@link UserDetails} für Spring Security Integration.
 */
@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_users_email", columnList = "email"),
        @Index(name = "idx_users_email_verification_token", columnList = "email_verification_token")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity implements UserDetails {

    // ──────────────────────────────────────────────────────────────────────────
    // Authentication fields / Authentifizierungsfelder
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Unique email address used for login.
     * Eindeutige E-Mail-Adresse für die Anmeldung.
     */
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /**
     * BCrypt-hashed password (never store plain text!).
     * BCrypt-gehashtes Passwort (niemals Klartext speichern!).
     */
    @Column(nullable = false)
    private String password;

    // ──────────────────────────────────────────────────────────────────────────
    // Profile fields / Profilfelder
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * User's first name.
     * Vorname des Benutzers.
     */
    @Column(nullable = false, length = 100)
    private String firstName;

    /**
     * User's last name.
     * Nachname des Benutzers.
     */
    @Column(nullable = false, length = 100)
    private String lastName;

    /**
     * Optional phone number for contact.
     * Optionale Telefonnummer für Kontakt.
     */
    @Column(length = 30)
    private String phone;

    /**
     * Cloudinary URL of user's avatar image.
     * Cloudinary-URL des Benutzer-Avatarbilds.
     */
    @Column(length = 500)
    private String avatarUrl;

    // ──────────────────────────────────────────────────────────────────────────
    // Role & status / Rolle und Status
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * User's role on the platform (USER or ADMIN).
     * Rolle des Benutzers auf der Plattform (USER oder ADMIN).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.USER;

    /**
     * Whether the user's email has been verified.
     * Ob die E-Mail des Benutzers verifiziert wurde.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    /**
     * Token sent via email for verification (UUID string).
     * Token, das per E-Mail zur Verifizierung gesendet wird (UUID-String).
     */
    @Column(length = 100)
    private String emailVerificationToken;

    /**
     * Expiry timestamp of the verification token (24 hours from creation).
     * Ablaufzeitpunkt des Verifizierungstokens (24 Stunden ab Erstellung).
     */
    @Column
    private LocalDateTime emailVerificationTokenExpiry;

    /**
     * Timestamp when the user was banned by an admin (null = not banned).
     * Zeitpunkt, wann der Benutzer von einem Admin gesperrt wurde (null = nicht gesperrt).
     */
    @Column
    private LocalDateTime bannedAt;

    // ──────────────────────────────────────────────────────────────────────────
    // Convenience methods / Hilfsmethoden
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns the user's full name (firstName + lastName).
     * Gibt den vollständigen Namen zurück (Vorname + Nachname).
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Checks whether the user is currently banned.
     * Prüft, ob der Benutzer aktuell gesperrt ist.
     */
    public boolean isBanned() {
        return bannedAt != null;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // UserDetails implementation (Spring Security)
    // UserDetails-Implementierung (Spring Security)
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Grant authority based on role prefix ROLE_
        // Berechtigung basierend auf Rollen-Präfix ROLE_ vergeben
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /**
     * Email is the username in Spring Security context.
     * E-Mail ist der Benutzername im Spring Security-Kontext.
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * Account is considered non-expired always (no expiry logic in MVP).
     * Konto gilt immer als nicht abgelaufen (keine Ablauflogik im MVP).
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Account is locked if the user is banned.
     * Konto ist gesperrt, wenn der Benutzer gebannt wurde.
     */
    @Override
    public boolean isAccountNonLocked() {
        return !isBanned();
    }

    /**
     * Credentials never expire in MVP.
     * Anmeldedaten laufen im MVP nie ab.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Account is enabled only after email verification.
     * Konto ist nur nach E-Mail-Verifizierung aktiviert.
     */
    @Override
    public boolean isEnabled() {
        return emailVerified && getDeletedAt() == null;
    }
}
