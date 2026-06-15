package com.rentmybike.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for updating user profile — PUT /api/v1/users/me.
 * DTO zum Aktualisieren des Benutzerprofils — PUT /api/v1/users/me.
 *
 * <p>Email and password changes have separate dedicated endpoints.
 * <p>E-Mail- und Passwortänderungen haben separate dedizierte Endpunkte.
 */
@Data
public class UpdateProfileRequest {

    @NotBlank(message = "First name is required / Vorname ist erforderlich")
    @Size(max = 100, message = "First name too long / Vorname zu lang")
    private String firstName;

    @NotBlank(message = "Last name is required / Nachname ist erforderlich")
    @Size(max = 100, message = "Last name too long / Nachname zu lang")
    private String lastName;

    /** Optional phone number / Optionale Telefonnummer */
    @Size(max = 30, message = "Phone number too long / Telefonnummer zu lang")
    private String phone;
}
