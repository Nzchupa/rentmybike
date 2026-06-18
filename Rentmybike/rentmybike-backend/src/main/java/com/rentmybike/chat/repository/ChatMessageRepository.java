package com.rentmybike.chat.repository;

import com.rentmybike.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for chat message persistence/history queries.
 * Repository für Chatnachrichten-Persistenz/Verlaufsabfragen.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /**
     * Full message history for a booking, oldest first, with the sender
     * eagerly fetched to avoid N+1 lookups when rendering names/avatars.
     * Vollständiger Nachrichtenverlauf für eine Buchung, älteste zuerst, mit
     * eifrig geladenem Sender, um N+1-Abfragen beim Rendern von
     * Namen/Avataren zu vermeiden.
     */
    @Query("SELECT m FROM ChatMessage m JOIN FETCH m.sender " +
           "WHERE m.booking.id = :bookingId AND m.deletedAt IS NULL " +
           "ORDER BY m.createdAt ASC")
    List<ChatMessage> findByBookingIdWithSender(@Param("bookingId") UUID bookingId);
}
