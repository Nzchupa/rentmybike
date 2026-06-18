package com.rentmybike.chat.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Outbound DTO for a single chat message — sent both as the REST history
 * payload and as the STOMP broadcast frame.
 * Ausgehendes DTO für eine einzelne Chatnachricht — wird sowohl als
 * REST-Verlaufs-Payload als auch als STOMP-Broadcast-Frame gesendet.
 */
@Data
@Builder
public class ChatMessageResponse {
    private UUID id;
    private UUID bookingId;
    private UUID senderId;
    private String senderName;
    private String senderAvatarUrl;
    private String content;
    private LocalDateTime createdAt;
}
