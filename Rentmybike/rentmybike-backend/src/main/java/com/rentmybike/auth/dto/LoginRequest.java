package com.rentmybike.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO for user login request.
 * DTO für die Benutzeranmeldeanfrage.
 */
@Data
public class LoginRequest {

    /**
     * User's email address for authentication.
     * E-Mail-Adresse des Benutzers zur Authentifizierung.
     */
    @NotBlank(message = "Email is required / E-Mail ist erforderlich")
    @Email(message = "Invalid email format / Ungültiges E-Mail-Format")
    private String email;

    /**
     * User's password (plain text — will be matched against BCrypt hash).
     * Benutzerpasswort (Klartext — wird mit BCrypt-Hash verglichen).
     */
    @NotBlank(message = "Password is required / Passwort ist erforderlich")
    private String password;
}
