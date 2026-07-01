package com.rentmybike.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for adding a reply to an existing support ticket thread —
 * used by both the filing user and admins.
 * Request-Body zum Hinzufügen einer Antwort zu einem bestehenden
 * Support-Ticket-Thread — wird sowohl vom einreichenden Benutzer als auch
 * von Admins verwendet.
 *
 * <p>POST /api/v1/support/tickets/{id}/messages
 * <p>POST /api/v1/admin/support/tickets/{id}/messages
 */
@Data
public class SendSupportMessageRequest {

    @NotBlank(message = "Message is required / Nachricht ist erforderlich")
    @Size(max = 4000, message = "Message must be 4000 characters or fewer / Nachricht darf maximal 4000 Zeichen lang sein")
    private String body;
}
