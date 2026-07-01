package com.rentmybike.support.repository;

import com.rentmybike.support.entity.SupportCategory;
import com.rentmybike.support.entity.SupportTicket;
import com.rentmybike.support.entity.SupportTicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {

    /**
     * A user's own tickets, newest first — backs the "my tickets" list.
     * Die eigenen Tickets eines Benutzers, neueste zuerst — versorgt die
     * "Meine Tickets"-Liste.
     */
    @Query("""
            SELECT t FROM SupportTicket t
            WHERE t.deletedAt IS NULL AND t.userId = :userId
            ORDER BY t.updatedAt DESC
            """)
    Page<SupportTicket> findAllForUser(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Admin list/filter query — null-coalescing filters, same pattern as
     * ReportRepository.findAllForAdmin. Search matches subject and the
     * filing user's name/email.
     * Admin-Listen-/Filterabfrage — null-koaleszierende Filter, gleiches
     * Muster wie ReportRepository.findAllForAdmin. Die Suche durchsucht
     * Betreff sowie Name/E-Mail des einreichenden Benutzers.
     *
     * <p>{@code CAST(:search AS string)} is required — see the detailed
     * explanation on {@code UserRepository.findAllForAdmin}: without it, a
     * null search bound only inside CONCAT/LIKE is sent as bytea and the
     * query fails.
     */
    @Query("""
            SELECT t FROM SupportTicket t
            WHERE t.deletedAt IS NULL
              AND (:status IS NULL OR t.status = :status)
              AND (:category IS NULL OR t.category = :category)
              AND (:search IS NULL
                   OR LOWER(t.subject)   LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(t.userName)  LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(t.userEmail) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            ORDER BY t.updatedAt DESC
            """)
    Page<SupportTicket> findAllForAdmin(
            @Param("status") SupportTicketStatus status,
            @Param("category") SupportCategory category,
            @Param("search") String search,
            Pageable pageable);

    long countByStatusAndDeletedAtIsNull(SupportTicketStatus status);
}
