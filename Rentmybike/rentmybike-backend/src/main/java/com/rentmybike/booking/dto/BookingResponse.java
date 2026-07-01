package com.rentmybike.booking.dto;

import com.rentmybike.booking.entity.BookingStatus;
import com.rentmybike.booking.entity.PaymentMethod;
import com.rentmybike.booking.entity.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for a booking in API responses.
 * DTO für eine Buchung in API-Antworten.
 *
 * <p>Used for both renter and owner views — the service controls
 * which fields are populated based on the viewer's role.
 * <p>Wird für Mieter- und Eigentümeransichten verwendet — der Service steuert,
 * welche Felder basierend auf der Rolle des Betrachters befüllt werden.
 */
@Data
@Builder
public class BookingResponse {

    private UUID id;

    // Bike summary / Fahrrad-Zusammenfassung
    private UUID bikeId;
    private String bikeTitle;
    private String bikeCity;
    private String bikePrimaryPhotoUrl;

    // Participants / Teilnehmer
    private UUID renterId;
    private String renterName;
    private String renterAvatarUrl;

    private UUID ownerId;
    private String ownerName;
    private String ownerAvatarUrl;

    // Booking details / Buchungsdetails
    private LocalDate startDate;
    private LocalDate endDate;
    private long rentalDays;
    private BigDecimal totalPrice;
    private BookingStatus status;
    private String message;

    /** Set once the owner accepts — null while PENDING / Gesetzt nach Annahme durch den Eigentümer — null solange PENDING */
    private PaymentMethod paymentMethod;

    /** Manual PayPal confirmation state — only set for PAYPAL bookings / Manueller PayPal-Bestätigungsstatus — nur bei PAYPAL-Buchungen gesetzt */
    private PaymentStatus paymentStatus;
    private String paymentReceiptUrl;
    private LocalDateTime paymentReceiptSubmittedAt;
    private LocalDateTime paymentConfirmedAt;

    // Accessory add-ons (Stage 3 "Business accounts") / Zubehör-Add-ons (Stage 3 "Business-Konten")
    private List<BookingAccessoryResponse> accessories;

    // Timestamps / Zeitstempel
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Computed flags for frontend / Berechnete Flags für das Frontend
    /** Whether the current user can cancel this booking / Ob der aktuelle Benutzer diese Buchung stornieren kann */
    private boolean cancellable;
    /** Whether reviews can be left for this booking / Ob Bewertungen für diese Buchung hinterlassen werden können */
    private boolean reviewable;
}
