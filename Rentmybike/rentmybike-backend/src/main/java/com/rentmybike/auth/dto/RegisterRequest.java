package com.rentmybike.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for user registration request.
 * DTO für die Benutzerregistrierungsanfrage.
 */
@Data
public class RegisterRequest {

    /**
     * User's email — must be a valid email format and not blank.
     * E-Mail des Benutzers — muss ein gültiges E-Mail-Format haben und darf nicht leer sein.
     */
    @NotBlank(message = "Email is required / E-Mail ist erforderlich")
    @Email(message = "Invalid email format / Ungültiges E-Mail-Format")
    private String email;

    /**
     * Password — minimum 8 characters.
     * Passwort — mindestens 8 Zeichen.
     */
    @NotBlank(message = "Password is required / Passwort ist erforderlich")
    @Size(min = 8, message = "Password must be at least 8 characters / Passwort muss mindestens 8 Zeichen haben")
    private String password;

    /**
     * First name — required, max 100 chars.
     * Vorname — erforderlich, max 100 Zeichen.
     */
    @NotBlank(message = "First name is required / Vorname ist erforderlich")
    @Size(max = 100, message = "First name too long / Vorname zu lang")
    private String firstName;

    /**
     * Last name — required, max 100 chars.
     * Nachname — erforderlich, max 100 Zeichen.
     */
    @NotBlank(message = "Last name is required / Nachname ist erforderlich")
    @Size(max = 100, message = "Last name too long / Nachname zu lang")
    private String lastName;
}
