package com.rentmybike.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for changing user password — PUT /api/v1/users/me/password.
 * DTO zum Ändern des Benutzerpassworts — PUT /api/v1/users/me/password.
 *
 * <p>Requires the current password for verification before allowing the change.
 * <p>Erfordert das aktuelle Passwort zur Verifizierung vor der Änderung.
 */
@Data
public class ChangePasswordRequest {

    /** Current password for identity verification / Aktuelles Passwort zur Identitätsverifizierung */
    @NotBlank(message = "Current password is required / Aktuelles Passwort ist erforderlich")
    private String currentPassword;

    /** New password — minimum 8 characters / Neues Passwort — mindestens 8 Zeichen */
    @NotBlank(message = "New password is required / Neues Passwort ist erforderlich")
    @Size(min = 8, message = "Password must be at least 8 characters / Passwort muss mindestens 8 Zeichen haben")
    private String newPassword;
}
