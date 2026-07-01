package com.rentmybike.support.dto;

import com.rentmybike.support.entity.SupportCategory;
import com.rentmybike.support.entity.SupportTicketStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class SupportTicketResponse {

    private UUID id;
    private UUID userId;
    private String userName;
    private String userEmail;
    private String subject;
    private SupportCategory category;
    private SupportTicketStatus status;

    // Cheap aggregates for list views — avoids shipping the full thread on
    // every row of a paginated list. / Günstige Aggregate für Listenansichten
    // — vermeidet, den vollständigen Verlauf in jeder Zeile einer paginierten
    // Liste mitzuschicken.
    private int messageCount;
    private String lastMessagePreview;
    private LocalDateTime lastMessageAt;

    /**
     * Full message thread — populated only by the single-ticket "get" calls
     * (getMyTicket / adminGetTicket), left null on paginated list responses.
     * Vollständiger Nachrichtenverlauf — nur bei den Einzelticket-"Get"-
     * Aufrufen (getMyTicket / adminGetTicket) befüllt, bei paginierten
     * Listenantworten null.
     */
    private List<SupportMessageResponse> messages;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
