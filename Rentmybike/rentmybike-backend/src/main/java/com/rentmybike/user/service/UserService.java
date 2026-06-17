package com.rentmybike.user.service;

import com.rentmybike.common.exception.AccessDeniedException;
import com.rentmybike.common.exception.BusinessException;
import com.rentmybike.common.exception.ResourceNotFoundException;
import com.rentmybike.common.service.CloudinaryService;
import com.rentmybike.user.dto.ChangePasswordRequest;
import com.rentmybike.user.dto.PublicUserResponse;
import com.rentmybike.user.dto.UpdateProfileRequest;
import com.rentmybike.user.dto.UserProfileResponse;
import com.rentmybike.user.entity.User;
import com.rentmybike.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Service for user profile management.
 * Service für die Benutzerprofilverwaltung.
 *
 * <p>Handles: own profile, public profiles, profile updates, password changes, avatar uploads.
 * <p>Verarbeitet: eigenes Profil, öffentliche Profile, Profilaktualisierungen, Passwortänderungen, Avatar-Uploads.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private static final String AVATAR_FOLDER = "rentmybike/avatars";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;

    // ──────────────────────────────────────────────────────────────────────────
    // Read operations / Leseoperationen
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns the full profile of the currently authenticated user.
     * Gibt das vollständige Profil des aktuell authentifizierten Benutzers zurück.
     *
     * @param userId the authenticated user's ID / ID des authentifizierten Benutzers
     * @return full profile response / vollständige Profilantwort
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(UUID userId) {
        User user = findActiveUser(userId);
        return toUserProfileResponse(user);
    }

    /**
     * Returns the public profile of any user by ID.
     * Gibt das öffentliche Profil eines beliebigen Benutzers anhand der ID zurück.
     *
     * <p>Used by renters viewing a bike owner's profile.
     * <p>Wird von Mietern verwendet, die das Profil eines Fahrrad-Eigentümers anzeigen.
     *
     * @param userId the target user's ID / ID des Zielbenutzers
     * @return minimal public profile / minimales öffentliches Profil
     */
    @Transactional(readOnly = true)
    public PublicUserResponse getPublicProfile(UUID userId) {
        User user = findActiveUser(userId);
        return toPublicUserResponse(user);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Update operations / Aktualisierungsoperationen
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Updates the authenticated user's first name, last name, and phone number.
     * Aktualisiert Vorname, Nachname und Telefonnummer des authentifizierten Benutzers.
     *
     * @param userId  the authenticated user's ID / ID des authentifizierten Benutzers
     * @param request update data / Aktualisierungsdaten
     * @return updated profile / aktualisiertes Profil
     */
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findActiveUser(userId);

        user.setFirstName(request.getFirstName().strip());
        user.setLastName(request.getLastName().strip());
        // Allow clearing phone by passing null or blank / Telefon durch null oder leer löschen erlauben
        user.setPhone(request.getPhone() != null && !request.getPhone().isBlank()
                ? request.getPhone().strip()
                : null);

        userRepository.save(user);
        log.info("Profile updated for user: {} / Profil aktualisiert für Benutzer: {}", userId, userId);

        return toUserProfileResponse(user);
    }

    /**
     * Changes the user's password after verifying the current one.
     * Ändert das Passwort des Benutzers nach Verifizierung des aktuellen Passworts.
     *
     * @param userId  the authenticated user's ID / ID des authentifizierten Benutzers
     * @param request current + new password / aktuelles + neues Passwort
     * @throws BusinessException if current password is wrong or new == current
     */
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = findActiveUser(userId);

        // Verify current password / Aktuelles Passwort verifizieren
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Current password is incorrect / Aktuelles Passwort ist falsch");
        }

        // Prevent setting the same password / Gleiches Passwort verhindern
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException(
                    "New password must differ from current / Neues Passwort muss sich vom aktuellen unterscheiden");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        // Bump tokenVersion so every refresh token issued before this point
        // is rejected (see AuthService.refresh()'s tokenVersion check) —
        // otherwise a stolen/leaked refresh token would keep working after
        // the user changes their password specifically to invalidate it.
        // tokenVersion erhöhen, damit jeder zuvor ausgestellte Refresh-Token
        // abgelehnt wird (siehe tokenVersion-Prüfung in AuthService.refresh())
        // — sonst würde ein gestohlener/geleakter Refresh-Token weiterhin
        // funktionieren, nachdem der Benutzer sein Passwort genau deshalb
        // geändert hat, um ihn zu invalidieren.
        user.setTokenVersion(user.getTokenVersion() + 1);

        userRepository.save(user);

        log.info("Password changed for user: {} / Passwort geändert für Benutzer: {}", userId, userId);
    }

    /**
     * Uploads a new avatar image to Cloudinary and updates the user's avatarUrl.
     * Lädt ein neues Avatar-Bild auf Cloudinary hoch und aktualisiert die avatarUrl des Benutzers.
     *
     * <p>Deletes the old avatar from Cloudinary if one exists.
     * <p>Löscht den alten Avatar von Cloudinary, falls vorhanden.
     *
     * @param userId the authenticated user's ID / ID des authentifizierten Benutzers
     * @param file   the image file (JPEG/PNG/WebP, max 5MB) / die Bilddatei
     * @return updated profile with new avatar URL / aktualisiertes Profil mit neuer Avatar-URL
     */
    public UserProfileResponse uploadAvatar(UUID userId, MultipartFile file) {
        User user = findActiveUser(userId);

        // Delete old avatar if exists / Alten Avatar löschen falls vorhanden
        if (user.getAvatarUrl() != null) {
            cloudinaryService.deleteImage(user.getAvatarUrl());
        }

        // Upload new avatar / Neuen Avatar hochladen
        String newAvatarUrl = cloudinaryService.uploadImage(file, AVATAR_FOLDER);

        user.setAvatarUrl(newAvatarUrl);
        userRepository.save(user);

        log.info("Avatar uploaded for user: {} / Avatar hochgeladen für Benutzer: {}", userId, userId);

        return toUserProfileResponse(user);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers / Private Hilfsmethoden
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Loads a non-deleted, non-banned user by ID.
     * Lädt einen nicht gelöschten, nicht gesperrten Benutzer anhand der ID.
     *
     * @throws ResourceNotFoundException if user doesn't exist / wenn Benutzer nicht existiert
     * @throws AccessDeniedException     if user is banned / wenn Benutzer gesperrt ist
     */
    private User findActiveUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (user.isDeleted()) {
            throw new ResourceNotFoundException("User", userId);
        }
        if (user.isBanned()) {
            throw new AccessDeniedException("Account is banned / Konto ist gesperrt");
        }

        return user;
    }

    /**
     * Maps a User entity to a full UserProfileResponse.
     * Mappt eine User-Entity auf eine vollständige UserProfileResponse.
     */
    private UserProfileResponse toUserProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .emailVerified(user.isEmailVerified())
                .banned(user.isBanned())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * Maps a User entity to a minimal PublicUserResponse.
     * Mappt eine User-Entity auf eine minimale PublicUserResponse.
     */
    private PublicUserResponse toPublicUserResponse(User user) {
        return PublicUserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
