package com.rentmybike.bike.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * DTO for a single bike photo in API responses.
 * DTO für ein einzelnes Fahrrad-Foto in API-Antworten.
 */
@Data
@Builder
public class BikePhotoResponse {

    private UUID id;
    private String url;
    private int displayOrder;
    private boolean primary;
}
