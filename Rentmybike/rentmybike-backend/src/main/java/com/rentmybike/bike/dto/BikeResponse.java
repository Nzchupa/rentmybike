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
    /**
     * Set when the owner is a BUSINESS account verified by an admin. The
     * frontend bike detail page (and BikeCard) already expected this field
     * (and ownerBusinessName below) to show a "verified shop" badge, but it
     * was never populated here — the badge silently never rendered.
     * Gesetzt, wenn der Eigentümer ein von einem Admin verifiziertes
     * BUSINESS-Konto ist. Die Frontend-Fahrrad-Detailseite (und BikeCard)
     * erwarteten dieses Feld (und ownerBusinessName unten) bereits für ein
     * "Verifizierter Shop"-Abzeichen, es wurde hier aber nie befüllt — das
     * Abzeichen wurde also nie angezeigt.
     */
    private boolean ownerBusinessVerified;
    /** Business display name — present when ownerBusinessVerified is true / Geschäftsname — vorhanden, wenn ownerBusinessVerified true ist */
    private String ownerBusinessName;

    // Listing / Inserat
    private String title;
    private String description;
    /** Optional brand/model, e.g. "Trek FX2 Disc" / Optionale Marke/Modell */
    private String model;
    private BikeCategory category;
    private BigDecimal pricePerDay;
    /** Optional refundable security deposit — null means none required / Optionale Kaution — null bedeutet keine erforderlich */
    private BigDecimal depositAmount;

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
