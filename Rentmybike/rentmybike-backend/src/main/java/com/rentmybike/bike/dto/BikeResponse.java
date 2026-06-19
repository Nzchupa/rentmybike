package com.rentmybike.bike.dto;

import com.rentmybike.bike.entity.ApprovalStatus;
import com.rentmybike.bike.entity.BikeCategory;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for a bike listing in API responses.
 * DTO für ein Fahrrad-Inserat in API-Antworten.
 *
 * <p>Used for both public search results and owner/admin views.
 * Some fields (address, rejectionReason) are omitted from public responses
 * via service-level filtering, not field-level annotations.
 *
 * <p>Wird sowohl für öffentliche Suchergebnisse als auch für Eigentümer-/Admin-Ansichten verwendet.
 * Einige Felder werden durch Service-seitige Filterung ausgeblendet.
 */
@Data
@Builder
public class BikeResponse {

    private UUID id;

    // Owner info / Eigentümer-Info
    private UUID ownerId;
    private String ownerName;
    private String ownerAvatarUrl;

    // Listing / Inserat
    private String title;
    private String description;
    private BikeCategory category;
    private BigDecimal pricePerDay;

    // Location / Standort
    private String city;
    /** Only included for the owner or confirmed renters / Nur für Eigentümer oder bestätigte Mieter */
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;

    // Status / Status
    private boolean available;
    private ApprovalStatus approvalStatus;
    /** Only included in owner/admin view / Nur in Eigentümer-/Admin-Ansicht */
    private String rejectionReason;

    // Photos / Fotos
    private List<BikePhotoResponse> photos;
    /** Convenience: URL of the primary photo / Bequemlichkeit: URL des Primärfotos */
    private String primaryPhotoUrl;

    /** Number of public detail-page views — see Bike.viewCount / Anzahl öffentlicher Detailseiten-Aufrufe — siehe Bike.viewCount */
    private long viewCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
