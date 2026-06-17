package com.rentmybike.notification.service;

import com.rentmybike.auth.service.EmailService;
import com.rentmybike.booking.entity.Booking;
import com.rentmybike.common.exception.AccessDeniedException;
import com.rentmybike.common.exception.ResourceNotFoundException;
import com.rentmybike.common.response.PageResponse;
import com.rentmybike.notification.dto.NotificationResponse;
import com.rentmybike.notification.entity.Notification;
import com.rentmybike.notification.entity.NotificationType;
import com.rentmybike.notification.repository.NotificationRepository;
import com.rentmybike.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Service for in-app notifications and the transactional emails that
 * accompany them.
 * Service für In-App-Benachrichtigungen und die dazugehörigen
 * Transaktions-E-Mails.
 *
 * <p>Bug 5: bike owners previously had no signal at all — no email, no
 * in-app indicator — when a renter submitted a booking request. This
 * service is called from {@code BookingService.createBooking} right after
 * the booking is persisted, and does two things: (1) writes a
 * {@link Notification} row for the owner so the bell icon / notification
 * feed picks it up, and (2) sends a best-effort email via
 * {@link EmailService}.
 * <p>Bug 5: Fahrrad-Eigentümer hatten vorher überhaupt kein Signal — keine
 * E-Mail, kein In-App-Hinweis — wenn ein Mieter eine Buchungsanfrage stellte.
 * Dieser Service wird von {@code BookingService.createBooking} direkt nach
 * dem Speichern der Buchung aufgerufen und macht zwei Dinge: (1) schreibt
 * eine {@link Notification}-Zeile für den Eigentümer, damit das Glocken-Icon
 * / der Benachrichtigungs-Feed sie erfasst, und (2) sendet eine
 * Best-Effort-E-Mail über {@link EmailService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    // ──────────────────────────────────────────────────────────────────────────
    // Create (triggered by BookingService) / Erstellen (ausgelöst von BookingService)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Creates an in-app notification for the bike owner and emails them,
     * for a newly-created booking request.
     * Erstellt eine In-App-Benachrichtigung für den Fahrrad-Eigentümer und
     * benachrichtigt ihn per E-Mail über eine neu erstellte Buchungsanfrage.
     *
     * <p>Deliberately swallows email failures (logs only) — same philosophy
     * as the rest of the codebase: a transient Resend outage must not roll
     * back or fail the booking itself. The in-app {@link Notification} row
     * is still written either way, so the owner has at least one durable
     * signal even if the email never went out.
     * <p>Verschluckt E-Mail-Fehler absichtlich (nur Protokollierung) — gleiche
     * Philosophie wie im Rest der Codebase: ein vorübergehender Resend-Ausfall
     * darf die Buchung selbst nicht zurückrollen oder fehlschlagen lassen. Die
     * In-App-{@link Notification}-Zeile wird in jedem Fall geschrieben, sodass
     * der Eigentümer mindestens ein dauerhaftes Signal hat, auch wenn die
     * E-Mail nie verschickt wurde.
     *
     * @param booking the just-created booking / die soeben erstellte Buchung
     */
    public void notifyOwnerOfNewBookingRequest(Booking booking) {
        User owner = booking.getOwner();
        String renterName = booking.getRenter().getFullName();
        String bikeTitle = booking.getBike().getTitle();
        String start = booking.getStartDate().format(DATE_FMT);
        String end = booking.getEndDate().format(DATE_FMT);

        String title = "New rental request / Neue Mietanfrage";
        String message = renterName + " wants to rent \"" + bikeTitle + "\" from " + start + " to " + end
                + " / " + renterName + " möchte \"" + bikeTitle + "\" vom " + start + " bis " + end + " mieten.";

        Notification notification = Notification.builder()
                .user(owner)
                .booking(booking)
                .type(NotificationType.NEW_BOOKING_REQUEST)
                .title(title)
                .message(message)
                .build();
        notificationRepository.save(notification);

        try {
            boolean sent = emailService.sendNewBookingRequestEmail(owner, renterName, bikeTitle, start, end);
            if (!sent) {
                log.warn("New-booking-request email not sent to owner {} (booking {}) / "
                        + "Neue-Buchungsanfrage-E-Mail nicht an Eigentümer {} gesendet (Buchung {})",
                        owner.getId(), booking.getId(), owner.getId(), booking.getId());
            }
        } catch (Exception e) {
            // Never let an email failure affect the booking transaction that's
            // already committed by the time this runs from createBooking.
            // Nie zulassen, dass ein E-Mail-Fehler die Buchungstransaktion
            // beeinträchtigt, die zu diesem Zeitpunkt bereits committet ist.
            log.error("Failed to send new-booking-request email for booking {}: {} / "
                    + "Neue-Buchungsanfrage-E-Mail für Buchung {} fehlgeschlagen: {}",
                    booking.getId(), e.getMessage(), booking.getId(), e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Read / Lesen
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Paginated notification feed for the current user, newest first.
     * Paginierter Benachrichtigungs-Feed für den aktuellen Benutzer, neueste zuerst.
     */
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getMyNotifications(UUID userId, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByUserId(userId, pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    /**
     * Unread notification count for the current user — backs the bell badge.
     * Ungelesen-Zähler für den aktuellen Benutzer — liefert das Glocken-Badge.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadAtIsNullAndDeletedAtIsNull(userId);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Mark as read / Als gelesen markieren
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Marks a single notification as read. No-op (not an error) if it was
     * already read; throws if it doesn't belong to the caller.
     * Markiert eine einzelne Benachrichtigung als gelesen. Kein Effekt (kein
     * Fehler), falls bereits gelesen; wirft eine Exception, falls sie nicht
     * dem Aufrufer gehört.
     */
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
        if (!notification.getUser().getId().equals(userId)) {
            throw new AccessDeniedException(
                    "This notification does not belong to you / Diese Benachrichtigung gehört Ihnen nicht");
        }
        notificationRepository.markAsRead(notificationId, userId, LocalDateTime.now());
    }

    /**
     * Marks every unread notification for the current user as read.
     * Markiert alle ungelesenen Benachrichtigungen des aktuellen Benutzers als gelesen.
     */
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId, LocalDateTime.now());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers / Private Hilfsmethoden
    // ──────────────────────────────────────────────────────────────────────────

    private NotificationResponse toResponse(Notification n) {
        Booking booking = n.getBooking();
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .bookingId(booking != null ? booking.getId() : null)
                .bikeId(booking != null ? booking.getBike().getId() : null)
                .bikeTitle(booking != null ? booking.getBike().getTitle() : null)
                .build();
    }
}
