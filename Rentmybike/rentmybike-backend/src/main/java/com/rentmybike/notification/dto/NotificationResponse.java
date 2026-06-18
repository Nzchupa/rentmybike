package com.rentmybike.notification.dto;

import com.rentmybike.notification.entity.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Flat response DTO for a single notification — same denormalized-fields
 * convention as {@code ReviewResponse} (e.g. {@code bikeTitle} instead of a
 * nested booking/bike object) so the frontend doesn't need extra round-trips
 * to render a notification row.
 * Flaches Antwort-DTO für eine einzelne Benachrichtigung — gleiche
 * Konvention denormalisierter Felder wie {@code ReviewResponse}, damit das
 * Frontend keine zusätzlichen Roundtrips braucht, um eine Benachrichtigungszeile
 * darzustellen.
 */
@Getter
@Builder
public class NotificationResponse {
    private UUID id;
    private NotificationType type;
    private String title;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;

    // Denormalized booking/bike context, null if not booking-related / null, falls nicht buchungsbezogen
    private UUID bookingId;
    private UUID bikeId;
    private String bikeTitle;

    /**
     * Whether the recipient should be deep-linked to the "as owner" or
     * "as renter" bookings list to view this notification's booking — null
     * if not booking-related. Needed because, unlike {@code NEW_BOOKING_REQUEST}
     * (recipient is always the owner), {@code NEW_CHAT_MESSAGE} can be sent to
     * either side of a booking, so the frontend can no longer hardcode one link.
     * Ob der Empfänger zur "als Eigentümer"- oder "als Mieter"-Buchungsliste
     * verlinkt werden soll, um die Buchung dieser Benachrichtigung anzusehen —
     * null, falls nicht buchungsbezogen. Nötig, weil im Gegensatz zu
     * {@code NEW_BOOKING_REQUEST} (Empfänger ist immer der Eigentümer)
     * {@code NEW_CHAT_MESSAGE} an beide Seiten einer Buchung gesendet werden
     * kann, sodass das Frontend nicht mehr einen einzigen Link fest verdrahten kann.
     */
    private Boolean viewAsOwner;
}
