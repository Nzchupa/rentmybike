package com.rentmybike.booking.repository;

import com.rentmybike.booking.entity.BookingAccessory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for accessory line items attached to bookings.
 * Repository für Zubehör-Positionszeilen, die an Buchungen angehängt sind.
 */
@Repository
public interface BookingAccessoryRepository extends JpaRepository<BookingAccessory, UUID> {

    /**
     * Accessory selections for a booking, with the accessory fetched in the
     * same query to avoid N+1 lazy-load hits when building the response.
     * Zubehörauswahlen für eine Buchung, mit dem Zubehör in derselben Abfrage
     * geladen, um N+1-Lazy-Load-Zugriffe beim Erstellen der Antwort zu vermeiden.
     */
    @Query("""
            SELECT ba FROM BookingAccessory ba
            JOIN FETCH ba.accessory a
            WHERE ba.booking.id = :bookingId
            ORDER BY ba.createdAt ASC
            """)
    List<BookingAccessory> findByBookingId(@Param("bookingId") UUID bookingId);
}
