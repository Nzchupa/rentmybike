package com.rentmybike.bookingphoto.dto;

import com.rentmybike.bookingphoto.entity.BookingPhotoPhase;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for a single booking condition photo in API responses.
 * DTO für ein einzelnes Buchungs-Zustandsfoto in API-Antworten.
 */
@Data
@Builder
public class BookingPhotoResponse {

    private UUID id;
    private BookingPhotoPhase phase;
    private String photoUrl;
    private UUID uploaderId;
    private String uploaderName;
    private LocalDateTime createdAt;
}
