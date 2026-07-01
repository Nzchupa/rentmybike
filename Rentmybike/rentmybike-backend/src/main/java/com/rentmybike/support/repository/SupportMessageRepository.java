package com.rentmybike.support.repository;

import com.rentmybike.support.entity.SupportMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SupportMessageRepository extends JpaRepository<SupportMessage, UUID> {

    List<SupportMessage> findByTicketIdAndDeletedAtIsNullOrderByCreatedAtAsc(UUID ticketId);

    long countByTicketIdAndDeletedAtIsNull(UUID ticketId);

    /**
     * The most recent message on a ticket, used to populate the cheap
     * "lastMessagePreview"/"lastMessageAt" list aggregates without loading
     * the whole thread. Spring Data derives {@code findFirstBy...OrderByDesc}
     * as a LIMIT-1 query.
     * Die neueste Nachricht eines Tickets, verwendet, um die günstigen
     * Listen-Aggregate "lastMessagePreview"/"lastMessageAt" zu befüllen, ohne
     * den gesamten Verlauf zu laden. Spring Data leitet
     * {@code findFirstBy...OrderByDesc} als LIMIT-1-Abfrage ab.
     */
    java.util.Optional<SupportMessage> findFirstByTicketIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID ticketId);
}
