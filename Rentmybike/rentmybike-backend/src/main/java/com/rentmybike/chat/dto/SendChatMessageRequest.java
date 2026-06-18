package com.rentmybike.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Inbound STOMP payload for sending a chat message.
 * Eingehendes STOMP-Payload zum Senden einer Chatnachricht.
 */
@Data
public class SendChatMessageRequest {

    @NotBlank
    @Size(max = 2000)
    private String content;
}
