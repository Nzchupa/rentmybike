package com.rentmybike.support.service;

import com.rentmybike.audit.entity.AuditAction;
import com.rentmybike.audit.service.AuditLogService;
import com.rentmybike.common.exception.AccessDeniedException;
import com.rentmybike.common.exception.BusinessException;
import com.rentmybike.common.exception.ResourceNotFoundException;
import com.rentmybike.common.response.PageResponse;
import com.rentmybike.notification.service.NotificationService;
import com.rentmybike.support.dto.CreateSupportTicketRequest;
import com.rentmybike.support.dto.SendSupportMessageRequest;
import com.rentmybike.support.dto.SupportMessageResponse;
import com.rentmybike.support.dto.SupportTicketResponse;
import com.rentmybike.support.entity.SupportCategory;
import com.rentmybike.support.entity.SupportMessage;
import com.rentmybike.support.entity.SupportTicket;
import com.rentmybike.support.entity.SupportTicketStatus;
import com.rentmybike.support.repository.SupportMessageRepository;
import com.rentmybike.support.repository.SupportTicketRepository;
import com.rentmybike.user.entity.User;
import com.rentmybike.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Support ticket service — a user-facing help desk (open ticket, reply,
 * track status) that replaces "email the single developer" as the tracked
 * support channel, plus the admin side that triages and answers tickets.
 * Support-Ticket-Service — ein nutzerseitiges Support-System (Ticket
 * eröffnen, antworten, Status verfolgen), das "E-Mail an den einzelnen
 * Entwickler" als nachverfolgbaren Support-Kanal ablöst, sowie die
 * Admin-Seite, die Tickets triagiert und beantwortet.
 *
 * <p>Mirrors {@code ReportService}'s structure (denormalized snapshots,
 * admin list/filter query, audit log on terminal actions) but adds a real
 * message thread since support conversations are back-and-forth rather than
 * a single resolution note.
 * <p>Folgt der Struktur von {@code ReportService} (denormalisierte
 * Momentaufnahmen, Admin-Listen-/Filterabfrage, Audit-Log bei
 * Endzustands-Aktionen), fügt aber einen echten Nachrichtenverlauf hinzu, da
 * Support-Gespräche hin- und hergehen statt einer einzelnen Auflösungsnotiz.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SupportService {

    private final SupportTicketRepository ticketRepository;
    private final SupportMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    // ──────────────────────────────────────────────────────────────────────────
    // User-facing: create / list / get / reply
    // Benutzerseitig: erstellen / auflisten / abrufen / antworten
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Opens a new ticket and its first message in one call.
     * Eröffnet ein neues Ticket und dessen erste Nachricht in einem Aufruf.
     */
    public SupportTicketResponse createTicket(UUID userId, CreateSupportTicketRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        SupportTicket ticket = SupportTicket.builder()
                .userId(userId)
                .userName(user.getFullName())
                .userEmail(user.getEmail())
                .subject(request.getSubject())
                .category(request.getCategory())
                .status(SupportTicketStatus.OPEN)
                .build();
        ticket = ticketRepository.save(ticket);

        SupportMessage message = SupportMessage.builder()
                .ticket(ticket)
                .senderId(userId)
                .senderName(user.getFullName())
                .fromAdmin(false)
                .body(request.getMessage())
                .build();
        messageRepository.save(message);

        log.info("User {} opened support ticket {} ({}) / "
                + "Benutzer {} hat Support-Ticket {} eröffnet ({})",
                userId, ticket.getId(), request.getCategory(),
                userId, ticket.getId(), request.getCategory());

        notificationService.notifyAdminsOfNewSupportTicket(ticket);

        return toResponse(ticket, List.of(message), 1, message);
    }

    /**
     * Paginated list of the current user's own tickets.
     * Paginierte Liste der eigenen Tickets des aktuellen Benutzers.
     */
    @Transactional(readOnly = true)
    public PageResponse<SupportTicketResponse> listMyTickets(UUID userId, Pageable pageable) {
        Page<SupportTicket> page = ticketRepository.findAllForUser(userId, pageable);
        return PageResponse.from(page.map(this::toListResponse));
    }

    /**
     * A single ticket with its full message thread — owner-only.
     * Ein einzelnes Ticket mit vollständigem Nachrichtenverlauf — nur für den Eigentümer.
     */
    @Transactional(readOnly = true)
    public SupportTicketResponse getMyTicket(UUID userId, UUID ticketId) {
        SupportTicket ticket = findActiveTicket(ticketId);
        requireOwner(ticket, userId);
        List<SupportMessage> messages = messageRepository.findByTicketIdAndDeletedAtIsNullOrderByCreatedAtAsc(ticketId);
        return toResponse(ticket, messages, messages.size(), messages.isEmpty() ? null : messages.get(messages.size() - 1));
    }

    /**
     * Adds a reply from the ticket's owner. A CLOSED ticket is a hard
     * terminal state — the user must open a new ticket instead of replying
     * to a closed one.
     * Fügt eine Antwort des Ticket-Eigentümers hinzu. Ein CLOSED-Ticket ist
     * ein harter Endzustand — der Benutzer muss ein neues Ticket eröffnen,
     * statt auf ein geschlossenes zu antworten.
     */
    public SupportTicketResponse addMyMessage(UUID userId, UUID ticketId, SendSupportMessageRequest request) {
        SupportTicket ticket = findActiveTicket(ticketId);
        requireOwner(ticket, userId);
        requireNotClosed(ticket);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        SupportMessage message = SupportMessage.builder()
                .ticket(ticket)
                .senderId(userId)
                .senderName(user.getFullName())
                .fromAdmin(false)
                .body(request.getBody())
                .build();
        messageRepository.save(message);

        // A user replying to a RESOLVED ticket ("actually, still broken")
        // reopens it to IN_PROGRESS so it doesn't silently sit resolved.
        // Antwortet ein Benutzer auf ein RESOLVED-Ticket ("eigentlich doch
        // nicht behoben"), wird es auf IN_PROGRESS zurückgesetzt, damit es
        // nicht stillschweigend als gelöst gilt.
        if (ticket.getStatus() == SupportTicketStatus.RESOLVED) {
            ticket.setStatus(SupportTicketStatus.IN_PROGRESS);
        }

        List<SupportMessage> messages = messageRepository.findByTicketIdAndDeletedAtIsNullOrderByCreatedAtAsc(ticketId);
        return toResponse(ticket, messages, messages.size(), message);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Admin: list / get / reply / status
    // Admin: auflisten / abrufen / antworten / Status
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<SupportTicketResponse> adminList(SupportTicketStatus status, SupportCategory category, String search, Pageable pageable) {
        String effectiveSearch = (search != null && search.isBlank()) ? null : search;
        Page<SupportTicket> page = ticketRepository.findAllForAdmin(status, category, effectiveSearch, pageable);
        return PageResponse.from(page.map(this::toListResponse));
    }

    @Transactional(readOnly = true)
    public SupportTicketResponse adminGetTicket(UUID ticketId) {
        SupportTicket ticket = findActiveTicket(ticketId);
        List<SupportMessage> messages = messageRepository.findByTicketIdAndDeletedAtIsNullOrderByCreatedAtAsc(ticketId);
        return toResponse(ticket, messages, messages.size(), messages.isEmpty() ? null : messages.get(messages.size() - 1));
    }

    /**
     * Admin reply. Moves an OPEN ticket to IN_PROGRESS automatically (first
     * response = work has started) — mirrors the PENDING -&gt; UNDER_REVIEW
     * step in ReportService, but happens implicitly here since replying IS
     * the "start working on it" action for a ticket.
     * Admin-Antwort. Versetzt ein OPEN-Ticket automatisch in IN_PROGRESS
     * (erste Antwort = Bearbeitung hat begonnen) — analog zum
     * PENDING -&gt; UNDER_REVIEW-Schritt in ReportService, hier aber implizit,
     * da das Antworten selbst die "Bearbeitung starten"-Aktion eines Tickets ist.
     */
    public SupportTicketResponse adminAddMessage(UUID adminId, UUID ticketId, SendSupportMessageRequest request) {
        SupportTicket ticket = findActiveTicket(ticketId);
        requireNotClosed(ticket);

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("User", adminId));

        SupportMessage message = SupportMessage.builder()
                .ticket(ticket)
                .senderId(adminId)
                .senderName(admin.getFullName())
                .fromAdmin(true)
                .body(request.getBody())
                .build();
        messageRepository.save(message);

        if (ticket.getStatus() == SupportTicketStatus.OPEN) {
            ticket.setStatus(SupportTicketStatus.IN_PROGRESS);
        }

        userRepository.findById(ticket.getUserId())
                .ifPresent(recipient -> notificationService.notifySupportTicketReply(ticket, recipient, request.getBody()));

        log.info("Admin {} replied to support ticket {} / Admin {} hat auf Support-Ticket {} geantwortet",
                adminId, ticketId, adminId, ticketId);

        List<SupportMessage> messages = messageRepository.findByTicketIdAndDeletedAtIsNullOrderByCreatedAtAsc(ticketId);
        return toResponse(ticket, messages, messages.size(), message);
    }

    /**
     * Admin status change. RESOLVED and CLOSED are recorded in the audit
     * log (terminal-ish states, same treatment as report resolve/dismiss);
     * OPEN/IN_PROGRESS are just in-thread triage, not audit-worthy.
     * Admin-Statusänderung. RESOLVED und CLOSED werden im Audit-Log erfasst
     * (End-artige Zustände, gleiche Behandlung wie beim Lösen/Ablehnen eines
     * Reports); OPEN/IN_PROGRESS sind reine Thread-interne Triage, nicht
     * audit-würdig.
     */
    public SupportTicketResponse adminUpdateStatus(UUID adminId, UUID ticketId, SupportTicketStatus newStatus) {
        SupportTicket ticket = findActiveTicket(ticketId);
        ticket.setStatus(newStatus);

        if (newStatus == SupportTicketStatus.RESOLVED || newStatus == SupportTicketStatus.CLOSED) {
            AuditAction action = newStatus == SupportTicketStatus.RESOLVED
                    ? AuditAction.SUPPORT_TICKET_RESOLVED
                    : AuditAction.SUPPORT_TICKET_CLOSED;
            String adminName = userRepository.findById(adminId).map(User::getFullName).orElse(null);
            auditLogService.record(adminId, adminName, action, "SUPPORT_TICKET", ticket.getId(), ticket.getSubject());
        }

        log.info("Admin {} set support ticket {} to {} / Admin {} hat Support-Ticket {} auf {} gesetzt",
                adminId, ticketId, newStatus, adminId, ticketId, newStatus);

        List<SupportMessage> messages = messageRepository.findByTicketIdAndDeletedAtIsNullOrderByCreatedAtAsc(ticketId);
        return toResponse(ticket, messages, messages.size(), messages.isEmpty() ? null : messages.get(messages.size() - 1));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers / Private Hilfsmethoden
    // ──────────────────────────────────────────────────────────────────────────

    private SupportTicket findActiveTicket(UUID id) {
        return ticketRepository.findById(id)
                .filter(t -> t.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("SupportTicket", id));
    }

    private void requireOwner(SupportTicket ticket, UUID userId) {
        if (!ticket.getUserId().equals(userId)) {
            throw new AccessDeniedException(
                    "This ticket does not belong to you / Dieses Ticket gehört Ihnen nicht");
        }
    }

    private void requireNotClosed(SupportTicket ticket) {
        if (ticket.isClosed()) {
            throw new BusinessException(
                    "This ticket is closed — open a new ticket instead / "
                            + "Dieses Ticket ist geschlossen — bitte ein neues Ticket eröffnen");
        }
    }

    private SupportTicketResponse toListResponse(SupportTicket t) {
        return toResponse(t, null, (int) messageRepository.countByTicketIdAndDeletedAtIsNull(t.getId()),
                messageRepository.findFirstByTicketIdAndDeletedAtIsNullOrderByCreatedAtDesc(t.getId()).orElse(null));
    }

    private SupportTicketResponse toResponse(SupportTicket t, List<SupportMessage> messages, int messageCount, SupportMessage lastMessage) {
        return SupportTicketResponse.builder()
                .id(t.getId())
                .userId(t.getUserId())
                .userName(t.getUserName())
                .userEmail(t.getUserEmail())
                .subject(t.getSubject())
                .category(t.getCategory())
                .status(t.getStatus())
                .messageCount(messageCount)
                .lastMessagePreview(lastMessage != null ? truncate(lastMessage.getBody(), 140) : null)
                .lastMessageAt(lastMessage != null ? lastMessage.getCreatedAt() : null)
                .messages(messages == null ? null : messages.stream().map(this::toMessageResponse).toList())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private SupportMessageResponse toMessageResponse(SupportMessage m) {
        return SupportMessageResponse.builder()
                .id(m.getId())
                .senderId(m.getSenderId())
                .senderName(m.getSenderName())
                .fromAdmin(m.isFromAdmin())
                .body(m.getBody())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}
