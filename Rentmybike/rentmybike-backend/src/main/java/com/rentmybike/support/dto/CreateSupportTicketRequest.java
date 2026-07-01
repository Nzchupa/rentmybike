package com.rentmybike.support.dto;

import com.rentmybike.support.entity.SupportCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for a user opening a new support ticket. The ticket and its
 * first {@code SupportMessage} are created together from this one call.
 * Request-Body zum Eröffnen eines neuen Support-Tickets durch einen
 * Benutzer. Ticket und erste {@code SupportMessage} werden gemeinsam aus
 * diesem einen Aufruf erstellt.
 *
 * <p>POST /api/v1/support/tickets
 */
@Data
public class CreateSupportTicketRequest {

    @NotBlank(message = "Subject is required / Betreff ist erforderlich")
    @Size(max = 200, message = "Subject must be 200 characters or fewer / Betreff darf maximal 200 Zeichen lang sein")
    private String subject;

    @NotNull(message = "Category is required / Kategorie ist erforderlich")
    private SupportCategory category;

    @NotBlank(message = "Message is required / Nachricht ist erforderlich")
    @Size(max = 4000, message = "Message must be 4000 characters or fewer / Nachricht darf maximal 4000 Zeichen lang sein")
    private String message;
}
