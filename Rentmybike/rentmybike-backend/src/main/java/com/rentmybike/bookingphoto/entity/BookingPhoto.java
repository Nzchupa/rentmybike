package com.rentmybike.bookingphoto.entity;

import com.rentmybike.booking.entity.Booking;
import com.rentmybike.common.entity.BaseEntity;
import com.rentmybike.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * A before/after condition photo attached to a specific booking.
 * Ein Vorher/Nachher-Zustandsfoto, das einer bestimmten Buchung beigefügt ist.
 *
 * <p>Either the renter or the owner of the booking may upload photos —
 * uploader is tracked so the UI can attribute "uploaded by you" / "uploaded
 * by the owner/renter". Not tied to the bike listing itself, since the
 * bike's condition is specific to this one rental period.
 * <p>Entweder der Mieter oder der Eigentümer der Buchung kann Fotos
 * hochladen — der Uploader wird verfolgt, damit die UI "von dir
 * hochgeladen" / "vom Eigentümer/Mieter hochgeladen" zuordnen kann. Nicht
 * an das Fahrrad-Inserat selbst gebunden, da der Zustand des Fahrrads
 * spezifisch für diesen einen Mietzeitraum ist.
 *
 * <p>Maps to PostgreSQL table {@code booking_photos}.
 * <p>Entspricht der PostgreSQL-Tabelle {@code booking_photos}.
 */
@Entity
@Table(name = "booking_photos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingPhoto extends BaseEntity {

    /**
     * The booking this photo documents.
     * Die Buchung, die dieses Foto dokumentiert.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, updatable = false)
    private Booking booking;

    /**
     * The user who uploaded this photo — either the renter or the owner of the booking.
     * Der Benutzer, der dieses Foto hochgeladen hat — entweder Mieter oder Eigentümer der Buchung.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploader_id", nullable = false, updatable = false)
    private User uploader;

    /**
     * Whether this photo was taken before pickup or after return.
     * Ob dieses Foto vor der Abholung oder nach der Rückgabe aufgenommen wurde.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(10)", updatable = false)
    private BookingPhotoPhase phase;

    /**
     * Cloudinary HTTPS URL of the photo.
     * Cloudinary-HTTPS-URL des Fotos.
     */
    @Column(name = "photo_url", nullable = false, length = 500)
    private String photoUrl;
}
