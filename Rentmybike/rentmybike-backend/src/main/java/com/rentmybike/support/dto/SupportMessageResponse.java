package com.rentmybike.support.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class SupportMessageResponse {

    private UUID id;
    private UUID senderId;
    private String senderName;
    private boolean fromAdmin;
    private String body;
    private LocalDateTime createdAt;
}
