package com.rentmybike.user.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Public user profile DTO — returned for GET /api/v1/users/{id}/public.
 * Öffentliches Benutzerprofil-DTO — wird für GET /api/v1/users/{id}/public zurückgegeben.
 *
 * <p>Intentionally minimal — no email, no role, no sensitive data.
 * <p>Absichtlich minimal — keine E-Mail, keine Rolle, keine sensiblen Daten.
 * Used by renters viewing a bike owner's profile and vice versa.
 * Wird von Mietern verwendet, die das Profil eines Fahrrad-Eigentümers anzeigen und umgekehrt.
 */
@Data
@Builder
public class PublicUserResponse {

    private UUID id;
    private String fullName;
    private String avatarUrl;

    /** Member since — shown on profile page / Mitglied seit — auf der Profilseite angezeigt */
    private LocalDateTime createdAt;
}
