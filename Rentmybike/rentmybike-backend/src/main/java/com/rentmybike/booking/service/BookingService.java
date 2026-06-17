package com.rentmybike.booking.service;

import com.rentmybike.bike.entity.Bike;
import com.rentmybike.bike.entity.BikePhoto;
import com.rentmybike.bike.repository.BikeRepository;
import com.rentmybike.booking.dto.BookingResponse;
import com.rentmybike.booking.dto.CreateBookingRequest;
import com.rentmybike.booking.entity.Booking;
import com.rentmybike.booking.entity.BookingStatus;
import com.rentmybike.booking.repository.BookingRepository;
import com.rentmybike.common.exception.AccessDeniedException;
import com.rentmybike.common.exception.BusinessException;
import com.rentmybike.common.exception.ResourceNotFoundException;
import com.rentmybike.common.response.PageResponse;
import com.rentmybike.user.entity.User;
import com.rentmybike.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for bike rental booking lifecycle management.
 * Service für das Lebenszyklusmanagement von Fahrrad-Mietbuchungen.
 *
 * <p>Flow:
 * <ol>
 *   <li>Renter calls createBooking → PENDING, date conflict checked</li>
 *   <li>Owner calls acceptBooking → ACCEPTED</li>
 *   <li>Owner calls rejectBooking → REJECTED</li>
 *   <li>Renter/owner calls cancelBooking → CANCELLED</li>
 *   <li>Admin calls completeBooking → COMPLETED (unlocks reviews)</li>
 *   <li>Stale PENDING (>48h) auto-cancelled on lazy access</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookingService {

    /** Pending bookings older than this are auto-cancelled on next access. */
    private static final int PENDING_EXPIRY_HOURS = 48;

    private final BookingRepository bookingRepository;
    private final BikeRepository bikeRepository;
    private final UserRepository userRepository;

    // ──────────────────────────────────────────────────────────────────────────
    // CREATE / ERSTELLEN
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Renter submits a booking request.
     * Mieter reicht eine Buchungsanfrage ein.
     *
     * <p>Validates: dates, bike availability, no self-booking, no date conflict.
     * <p>Validiert: Termine, Fahrradverfügbarkeit, keine Selbstbuchung, kein Datumskonflikt.
     */
    public BookingResponse createBooking(UUID renterId, CreateBookingRequest request) {
        // Validate dates / Termine validieren
        if (!request.getEndDate().isAfter(request.getStartDate())
                && !request.getEndDate().equals(request.getStartDate())) {
            throw new BusinessException("End date must be on or after start date / Enddatum muss am oder nach dem Startdatum liegen");
        }

        User renter = userRepository.findById(renterId)
                .orElseThrow(() -> new ResourceNotFoundException("User", renterId));

        Bike bike = bikeRepository.findByIdWithDetails(request.getBikeId())
                .orElseThrow(() -> new ResourceNotFoundException("Bike", request.getBikeId()));

        // Bike must be publicly visible / Fahrrad muss öffentlich sichtbar sein
        if (!bike.isPubliclyVisible()) {
            throw new ResourceNotFoundException("Bike", request.getBikeId());
        }

        // No self-booking / Keine Selbstbuchung
        if (bike.getOwner().getId().equals(renterId)) {
            throw new BusinessException("You cannot rent your own bike / Sie können Ihr eigenes Fahrrad nicht mieten");
        }

        // Lock the bike row for the rest of this transaction (SELECT ... FOR UPDATE).
        // This serializes concurrent createBooking calls for the same bike: a second
        // request blocks here until the first one commits or rolls back, so the
        // conflict check below can never race with another insert for the same bike.
        // Without this lock, two requests could both read "no conflict" before either
        // commits, resulting in a double-booking for overlapping dates.
        //
        // Sperrt die Fahrrad-Zeile für den Rest dieser Transaktion (SELECT ... FOR UPDATE).
        // Dies serialisiert gleichzeitige createBooking-Aufrufe für dasselbe Fahrrad: eine
        // zweite Anfrage blockiert hier, bis die erste committet oder zurückgerollt wird,
        // sodass die folgende Konfliktprüfung niemals mit einem anderen Insert für dasselbe
        // Fahrrad in Konflikt geraten kann. Ohne diese Sperre könnten beide Anfragen "kein
        // Konflikt" lesen, bevor eine von beiden committet — das führt zu einer Doppelbuchung.
        bikeRepository.findByIdForUpdate(bike.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bike", request.getBikeId()));

        // Date conflict check / Datumskonflikt-Prüfung
        boolean hasConflict = bookingRepository.existsDateConflict(
                bike.getId(), request.getStartDate(), request.getEndDate());
        if (hasConflict) {
            throw new BusinessException(
                    "Bike is not available for the selected dates / Fahrrad ist für die gewählten Termine nicht verfügbar");
        }

        // Calculate total price / Gesamtpreis berechnen
        long days = java.time.temporal.ChronoUnit.DAYS.between(
                request.getStartDate(), request.getEndDate()) + 1;
        BigDecimal totalPrice = bike.getPricePerDay().multiply(BigDecimal.valueOf(days));

        Booking booking = Booking.builder()
                .bike(bike)
                .renter(renter)
                .owner(bike.getOwner())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalPrice(totalPrice)
                .status(BookingStatus.PENDING)
                .message(request.getMessage())
                .build();

        bookingRepository.save(booking);
        log.info("Booking created: {} by renter: {} for bike: {} / Buchung erstellt: {} von Mieter: {} für Fahrrad: {}",
                booking.getId(), renterId, bike.getId(),
                booking.getId(), renterId, bike.getId());

        return toBookingResponse(booking);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // READ / LESEN
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Get a single booking by ID — accessible by the renter, owner, or admin.
     * Einzelne Buchung nach ID abrufen — zugänglich für Mieter, Eigentümer oder Admin.
     */
    @Transactional(readOnly = true)
    public BookingResponse getBooking(UUID bookingId, UUID requesterId) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        requireParticipant(booking, requesterId);
        return toBookingResponse(booking);
    }

    /**
     * Renter's booking history (all their rentals).
     * Buchungshistorie des Mieters (alle seine Mieten).
     */
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getRenterBookings(UUID renterId, Pageable pageable) {
        expireStaleBookings(); // lazy expiry / lazy Ablauf
        Page<Booking> page = bookingRepository.findByRenterIdOrderByCreatedAtDesc(renterId, pageable);
        return PageResponse.from(page.map(this::toBookingResponse));
    }

    /**
     * Owner's incoming booking requests, optionally filtered by status.
     * Eingehende Buchungsanfragen des Eigentümers, optional nach Status gefiltert.
     */
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getOwnerBookings(UUID ownerId, BookingStatus status, Pageable pageable) {
        expireStaleBookings(); // lazy expiry / lazy Ablauf
        Page<Booking> page = bookingRepository.findByOwnerIdAndStatus(ownerId, status, pageable);
        return PageResponse.from(page.map(this::toBookingResponse));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // OWNER ACTIONS / EIGENTÜMER-AKTIONEN
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Owner accepts a PENDING booking request.
     * Eigentümer akzeptiert eine PENDING-Buchungsanfrage.
     */
    public BookingResponse acceptBooking(UUID bookingId, UUID ownerId) {
        Booking booking = loadBookingForOwner(bookingId, ownerId);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException("Only PENDING bookings can be accepted / Nur PENDING-Buchungen können akzeptiert werden");
        }

        booking.setStatus(BookingStatus.ACCEPTED);
        bookingRepository.save(booking);

        log.info("Booking ACCEPTED: {} by owner: {} / Buchung AKZEPTIERT: {} von Eigentümer: {}",
                bookingId, ownerId, bookingId, ownerId);
        return toBookingResponse(booking);
    }

    /**
     * Owner rejects a PENDING booking request.
     * Eigentümer lehnt eine PENDING-Buchungsanfrage ab.
     */
    public BookingResponse rejectBooking(UUID bookingId, UUID ownerId) {
        Booking booking = loadBookingForOwner(bookingId, ownerId);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException("Only PENDING bookings can be rejected / Nur PENDING-Buchungen können abgelehnt werden");
        }

        booking.setStatus(BookingStatus.REJECTED);
        bookingRepository.save(booking);

        log.info("Booking REJECTED: {} by owner: {} / Buchung ABGELEHNT: {} von Eigentümer: {}",
                bookingId, ownerId, bookingId, ownerId);
        return toBookingResponse(booking);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // RENTER ACTIONS / MIETER-AKTIONEN
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Renter cancels their own booking (PENDING or ACCEPTED).
     * Mieter storniert seine eigene Buchung (PENDING oder ACCEPTED).
     */
    public BookingResponse cancelBookingAsRenter(UUID bookingId, UUID renterId) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        if (!booking.getRenter().getId().equals(renterId)) {
            throw new AccessDeniedException("You are not the renter of this booking / Sie sind nicht der Mieter dieser Buchung");
        }

        if (!booking.isCancellable()) {
            throw new BusinessException(
                    "Booking cannot be cancelled in status: " + booking.getStatus()
                    + " / Buchung kann nicht im Status storniert werden: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        log.info("Booking CANCELLED by renter: {} / Buchung STORNIERT vom Mieter: {}", bookingId, bookingId);
        return toBookingResponse(booking);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // ADMIN ACTIONS / ADMIN-AKTIONEN
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Admin marks an ACCEPTED booking as COMPLETED after the rental period ends.
     * Admin markiert eine ACCEPTED-Buchung als COMPLETED nach Ende der Mietzeit.
     *
     * <p>Only COMPLETED bookings unlock the review flow for both parties.
     * <p>Nur COMPLETED-Buchungen schalten den Bewertungsfluss für beide Parteien frei.
     */
    public BookingResponse completeBooking(UUID bookingId) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        if (booking.getStatus() != BookingStatus.ACCEPTED) {
            throw new BusinessException("Only ACCEPTED bookings can be completed / Nur ACCEPTED-Buchungen können abgeschlossen werden");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);

        log.info("Booking COMPLETED: {} / Buchung ABGESCHLOSSEN: {}", bookingId, bookingId);
        return toBookingResponse(booking);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // LAZY EXPIRY / LAZY-ABLAUF
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Auto-cancels PENDING bookings older than 48 hours.
     * Storniert PENDING-Buchungen älter als 48 Stunden automatisch.
     *
     * <p>Called lazily at the start of list/detail endpoints instead of using a scheduler.
     * Simple and sufficient for dev/MVP; replace with @Scheduled in production.
     * <p>Wird lazy am Anfang von Listen-/Detailendpunkten aufgerufen statt eines Schedulers.
     * Einfach und ausreichend für Entwicklung/MVP; in Produktion durch @Scheduled ersetzen.
     */
    private void expireStaleBookings() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(PENDING_EXPIRY_HOURS);
        int expired = bookingRepository.expireStaleBookings(cutoff);
        if (expired > 0) {
            log.info("Expired {} stale PENDING bookings (>{}h) / {} veraltete PENDING-Buchungen abgelaufen (>{}h)",
                    expired, PENDING_EXPIRY_HOURS, expired, PENDING_EXPIRY_HOURS);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers / Private Hilfsmethoden
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Loads a booking and verifies the caller is the bike owner.
     * Lädt eine Buchung und überprüft, dass der Aufrufer der Fahrrad-Eigentümer ist.
     */
    private Booking loadBookingForOwner(UUID bookingId, UUID ownerId) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        if (!booking.getOwner().getId().equals(ownerId)) {
            throw new AccessDeniedException(
                    "You are not the owner of this booking's bike / Sie sind nicht der Eigentümer des Fahrrads dieser Buchung");
        }
        return booking;
    }

    /**
     * Ensures the requester is either the renter or owner of the booking.
     * Stellt sicher, dass der Anforderer entweder der Mieter oder Eigentümer der Buchung ist.
     */
    private void requireParticipant(Booking booking, UUID userId) {
        boolean isRenter = booking.getRenter().getId().equals(userId);
        boolean isOwner  = booking.getOwner().getId().equals(userId);
        if (!isRenter && !isOwner) {
            throw new AccessDeniedException(
                    "Access denied to this booking / Zugriff auf diese Buchung verweigert");
        }
    }

    /**
     * Maps a Booking entity to a BookingResponse DTO.
     * Mappt eine Booking-Entity auf ein BookingResponse-DTO.
     */
    private BookingResponse toBookingResponse(Booking booking) {
        Bike bike = booking.getBike();

        String primaryPhotoUrl = bike.getPhotos().stream()
                .filter(BikePhoto::isPrimary)
                .map(BikePhoto::getUrl)
                .findFirst()
                .orElse(null);

        return BookingResponse.builder()
                .id(booking.getId())
                // Bike / Fahrrad
                .bikeId(bike.getId())
                .bikeTitle(bike.getTitle())
                .bikeCity(bike.getCity())
                .bikePrimaryPhotoUrl(primaryPhotoUrl)
                // Participants / Teilnehmer
                .renterId(booking.getRenter().getId())
                .renterName(booking.getRenter().getFullName())
                .renterAvatarUrl(booking.getRenter().getAvatarUrl())
                .ownerId(booking.getOwner().getId())
                .ownerName(booking.getOwner().getFullName())
                .ownerAvatarUrl(booking.getOwner().getAvatarUrl())
                // Booking details / Buchungsdetails
                .startDate(booking.getStartDate())
                .endDate(booking.getEndDate())
                .rentalDays(booking.getRentalDays())
                .totalPrice(booking.getTotalPrice())
                .status(booking.getStatus())
                .message(booking.getMessage())
                // Timestamps / Zeitstempel
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                // Computed flags / Berechnete Flags
                .cancellable(booking.isCancellable())
                .reviewable(booking.getStatus() == BookingStatus.COMPLETED)
                .build();
    }
}
