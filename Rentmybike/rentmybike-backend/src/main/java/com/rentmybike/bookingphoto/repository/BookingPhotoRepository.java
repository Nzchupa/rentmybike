package com.rentmybike.bookingphoto.repository;

import com.rentmybike.bookingphoto.entity.BookingPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link BookingPhoto}.
 * Repository für {@link BookingPhoto}.
 */
public interface BookingPhotoRepository extends JpaRepository<BookingPhoto, UUID> {

    /**
     * All photos for a booking, oldest first, with the uploader pre-fetched
     * to avoid N+1 queries when rendering "uploaded by X" in the gallery.
     * Alle Fotos für eine Buchung, älteste zuerst, mit vorab geladenem
     * Uploader, um N+1-Abfragen beim Rendern von "hochgeladen von X" in der
     * Galerie zu vermeiden.
     */
    @Query("SELECT p FROM BookingPhoto p JOIN FETCH p.uploader " +
           "WHERE p.booking.id = :bookingId AND p.deletedAt IS NULL " +
           "ORDER BY p.createdAt ASC")
    List<BookingPhoto> findByBookingIdWithUploader(UUID bookingId);
}
