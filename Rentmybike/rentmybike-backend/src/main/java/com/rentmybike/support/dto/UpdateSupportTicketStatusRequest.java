package com.rentmybike.support.dto;

import com.rentmybike.support.entity.SupportTicketStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Admin request body to move a ticket to a new status.
 * Admin-Request-Body, um ein Ticket in einen neuen Status zu versetzen.
 *
 * <p>POST /api/v1/admin/support/tickets/{id}/status
 */
@Data
public class UpdateSupportTicketStatusRequest {

    @NotNull(message = "Status is required / Status ist erforderlich")
    private SupportTicketStatus status;
}
