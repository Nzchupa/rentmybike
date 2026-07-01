package com.rentmybike.booking.service;

import com.rentmybike.accessory.entity.Accessory;
import com.rentmybike.accessory.repository.AccessoryRepository;
import com.rentmybike.audit.entity.AuditAction;
import com.rentmybike.audit.service.AuditLogService;
import com.rentmybike.bike.entity.Bike;
import com.rentmybike.bike.entity.BikePhoto;
import com.rentmybike.bike.repository.BikeRepository;
import com.rentmybike.booking.dto.AccessorySelectionRequest;
import com.rentmybike.booking.dto.BookingAccessoryResponse;
import com.rentmybike.booking.dto.BookingResponse;
import com.rentmybike.booking.dto.CreateBookingRequest;
import com.rentmybike.booking.entity.Booking;
import com.rentmybike.booking.entity.BookingAccessory;
import com.rentmybike.booking.entity.BookingStatus;
import com.rentmybike.booking.entity.PaymentMethod;
import com.rentmybike.booking.repository.BookingAccessoryRepository;
import com.rentmybike.booking.repository.BookingRepository;
import com.rentmybike.common.exception.AccessDeniedException;
import com.rentmybike.common.exception.BusinessException;
import com.rentmybike.common.exception.ResourceNotFoundException;
import com.rentmybike.common.response.PageResponse;
import com.rentmybike.contract.service.ContractService;
import com.rentmybike.notification.service.NotificationService;
import com.rentmybike.user.entity.User;
import com.rentmybike.user.entity.UserRole;
import com.rentmybike.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    private final NotificationService notificationService;
    private final AccessoryRepository accessoryRepository;
    private final BookingAccessoryRepository bookingAccessoryRepository;
    private final AuditLogService auditLogService;
    private final ContractService contractService;

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

        // Accessory add-ons (Stage 3 "Business accounts") — validate stock and
        // ownership, lock the per-day price, and fold each line into the total.
        // Zubehör-Add-ons (Stage 3 "Business-Konten") — Bestand und Eigentum
        // prüfen, den Tagespreis sperren und jede Position in die Summe einrechnen.
        List<Accessory> selectedAccessories = new ArrayList<>();
        List<Integer> selectedQuantities = new ArrayList<>();
        if (request.getAccessories() != null) {
            for (AccessorySelectionRequest selection : request.getAccessories()) {
                Accessory accessory = accessoryRepository.findActiveById(selection.getAccessoryId())
                        .orElseThrow(() -> new ResourceNotFoundException("Accessory", selection.getAccessoryId()));

                if (!accessory.getOwner().getId().equals(bike.getOwner().getId())) {
                    throw new BusinessException(
                            "Accessory does not belong to this bike's owner / Zubehör gehört nicht dem Eigentümer dieses Fahrrads");
                }
                if (selection.getQuantity() > accessory.getQuantityTotal()) {
                    throw new BusinessException(
                            "Not enough stock for accessory: " + accessory.getName()
                            + " / Nicht genug Bestand für Zubehör: " + accessory.getName());
                }

                BigDecimal lineTotal = accessory.getPricePerDay()
                        .multiply(BigDecimal.valueOf(selection.getQuantity()))
                        .multiply(BigDecimal.valueOf(days));
                totalPrice = totalPrice.add(lineTotal);

                selectedAccessories.add(accessory);
                selectedQuantities.add(selection.getQuantity());
            }
        }

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

        // Persist accessory line items now that the booking has an ID.
        // Zubehör-Positionszeilen jetzt persistieren, da die Buchung eine ID hat.
        for (int i = 0; i < selectedAccessories.size(); i++) {
            Accessory accessory = selectedAccessories.get(i);
            int quantity = selectedQuantities.get(i);
            BookingAccessory bookingAccessory = BookingAccessory.builder()
                    .booking(booking)
                    .accessory(accessory)
                    .quantity(quantity)
                    .pricePerDayAtBooking(accessory.getPricePerDay())
                    .build();
            bookingAccessoryRepository.save(bookingAccessory);
        }

        log.info("Booking created: {} by renter: {} for bike: {} / Buchung erstellt: {} von Mieter: {} für Fahrrad: {}",
                booking.getId(), renterId, bike.getId(),
                booking.getId(), renterId, bike.getId());

        // Bug 5: notify the bike owner (in-app + email) that a renter requested
        // their bike — previously there was no signal at all here, so owners
        // only found out by manually re-checking "As Owner" bookings.
        // Bug 5: den Fahrrad-Eigentümer (In-App + E-Mail) benachrichtigen, dass
        // ein Mieter sein Fahrrad angefragt hat — vorher gab es hier gar kein
        // Signal, sodass Eigentümer es nur durch manuelles erneutes Prüfen der
        // "Als Eigentümer"-Buchungen erfuhren.
        notificationService.notifyOwnerOfNewBookingRequest(booking);

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
     * Occupied date ranges for a bike (PENDING/ACCEPTED bookings only) — public,
     * used by the frontend booking calendar to disable already-taken dates and
     * to validate overlap client-side before submitting a request.
     * Belegte Datumsbereiche für ein Fahrrad (nur PENDING/ACCEPTED-Buchungen) —
     * öffentlich, wird vom Frontend-Buchungskalender verwendet, um bereits
     * vergebene Termine zu deaktivieren und Überschneidungen Client-seitig vor
     * dem Senden einer Anfrage zu validieren.
     *
     * <p>No identity or message data is returned — see
     * BookingRepository.findActiveBookingsByBikeId.
     */
    @Transactional(readOnly = true)
    public java.util.List<com.rentmybike.booking.dto.BookedDateRangeResponse> getBookedDateRanges(UUID bikeId) {
        if (!bikeRepository.existsById(bikeId)) {
            throw new ResourceNotFoundException("Bike", bikeId);
        }
        return bookingRepository.findActiveBookingsByBikeId(bikeId).stream()
                .map(b -> com.rentmybike.booking.dto.BookedDateRangeResponse.builder()
                        .startDate(b.getStartDate())
                        .endDate(b.getEndDate())
                        .build())
                .toList();
    }

    /**
     * Renter's booking history (all their rentals).
     * Buchungshistorie des Mieters (alle seine Mieten).
     *
     * <p>Bug fix: this used to be {@code @Transactional(readOnly = true)}, but it
     * calls {@link #expireStaleBookings()} first, which runs a bulk {@code UPDATE}
     * (see {@code BookingRepository.expireStaleBookings}). Spring sets the actual
     * JDBC connection to read-only for a {@code readOnly = true} transaction, and
     * PostgreSQL rejects any write statement on a read-only connection ("cannot
     * execute UPDATE in a read-only transaction"). That exception aborted the
     * whole request with a 500 — which the frontend's axios error handling turned
     * into a rejected promise that the bookings pages never checked
     * ({@code isError} was never read), so the UI just rendered the "no bookings"
     * empty state. This is why owners (and renters) appeared to have no bookings
     * at all even though bookings existed. Removing {@code readOnly = true} lets
     * the class-level (read-write) {@code @Transactional} apply instead.
     *
     * <p>Bugfix: dies war vorher {@code @Transactional(readOnly = true)}, ruft
     * aber zuerst {@link #expireStaleBookings()} auf, das ein Massen-{@code UPDATE}
     * ausführt (siehe {@code BookingRepository.expireStaleBookings}). Spring setzt
     * die tatsächliche JDBC-Verbindung bei {@code readOnly = true} auf
     * schreibgeschützt, und PostgreSQL lehnt jede Schreibanweisung auf einer
     * schreibgeschützten Verbindung ab ("cannot execute UPDATE in a read-only
     * transaction"). Diese Exception brach die gesamte Anfrage mit einem 500 ab —
     * was die Axios-Fehlerbehandlung im Frontend in ein abgelehntes Promise
     * verwandelte, das die Buchungsseiten nie prüften ({@code isError} wurde nie
     * gelesen), sodass die UI einfach den leeren "keine Buchungen"-Zustand
     * anzeigte. Deshalb wirkte es, als hätten Eigentümer (und Mieter) gar keine
     * Buchungen, obwohl welche existierten. Das Entfernen von
     * {@code readOnly = true} lässt das (lese-schreibende) Klassen-Level-
     * {@code @Transactional} stattdessen greifen.
     */
    public PageResponse<BookingResponse> getRenterBookings(UUID renterId, Pageable pageable) {
        expireStaleBookings(); // lazy expiry / lazy Ablauf
        Page<Booking> page = bookingRepository.findByRenterIdOrderByCreatedAtDesc(renterId, pageable);
        return PageResponse.from(page.map(this::toBookingResponse));
    }

    /**
     * Owner's incoming booking requests, optionally filtered by status.
     * Eingehende Buchungsanfragen des Eigentümers, optional nach Status gefiltert.
     *
     * <p>Same read-only/bulk-update conflict fixed as in {@link #getRenterBookings}
     * above — see that javadoc for the full explanation. This was the direct cause
     * of "owner sees no reservations/bookings even though renters successfully
     * made them": the very first call to this method (which is virtually
     * guaranteed to also be the first call that triggers a stale-booking sweep)
     * threw and the page silently showed an empty list.
     *
     * <p>Derselbe Readonly-/Massen-Update-Konflikt behoben wie in
     * {@link #getRenterBookings} oben — siehe dortiges Javadoc für die vollständige
     * Erklärung. Dies war die direkte Ursache dafür, dass "Eigentümer keine
     * Reservierungen/Buchungen sehen, obwohl Mieter erfolgreich welche
     * vorgenommen haben": der allererste Aufruf dieser Methode (der mit hoher
     * Wahrscheinlichkeit auch der erste Aufruf ist, der einen
     * Abgelaufene-Buchungen-Sweep auslöst) warf eine Exception, und die Seite
     * zeigte stillschweigend eine leere Liste.
     */
    public PageResponse<BookingResponse> getOwnerBookings(UUID ownerId, BookingStatus status, Pageable pageable) {
        expireStaleBookings(); // lazy expiry / lazy Ablauf
        Page<Booking> page = bookingRepository.findByOwnerIdAndStatus(ownerId, status, pageable);
        return PageResponse.from(page.map(this::toBookingResponse));
    }

    /**
     * Bookings for a business owner's bikes overlapping [from, to] — backs the
     * business rental calendar (Stage 3 "Business accounts").
     * Buchungen für die Fahrräder eines Business-Eigentümers, die sich mit
     * [from, to] überschneiden — Grundlage für den Business-Mietkalender
     * (Stage 3 "Business-Konten").
     */
    @Transactional(readOnly = true)
    public List<BookingResponse> getOwnerBookingCalendar(UUID ownerId, LocalDate from, LocalDate to) {
        List<Booking> bookings = bookingRepository.findByOwnerIdAndDateRange(ownerId, from, to);
        return bookings.stream().map(this::toBookingResponse).toList();
    }

    /**
     * The owner's next few upcoming ACCEPTED bookings, soonest first — backs
     * the business overview's "upcoming bookings" panel.
     * Die nächsten anstehenden ACCEPTED-Buchungen des Eigentümers, früheste
     * zuerst — Grundlage für das "anstehende Buchungen"-Panel der
     * Business-Übersicht.
     */
    @Transactional(readOnly = true)
    public List<BookingResponse> getOwnerUpcomingBookings(UUID ownerId, int limit) {
        // findUpcomingByOwnerId already has its own ORDER BY startDate ASC —
        // unsorted Pageable, see the Pageable/Sort pitfall noted elsewhere in
        // this class.
        List<Booking> bookings = bookingRepository.findUpcomingByOwnerId(ownerId, LocalDate.now(), PageRequest.of(0, limit));
        return bookings.stream().map(this::toBookingResponse).toList();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // OWNER ACTIONS / EIGENTÜMER-AKTIONEN
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Owner accepts a PENDING booking request, naming the payment method in
     * the same step (frozen into the auto-generated rental contract — see
     * {@link ContractService#generateForBooking}).
     * Eigentümer akzeptiert eine PENDING-Buchungsanfrage und benennt im
     * selben Schritt die Zahlungsmethode (wird in den automatisch
     * erstellten Mietvertrag eingefroren — siehe
     * {@link ContractService#generateForBooking}).
     */
    public BookingResponse acceptBooking(UUID bookingId, UUID ownerId, PaymentMethod paymentMethod) {
        Booking booking = loadBookingForOwner(bookingId, ownerId);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException("Only PENDING bookings can be accepted / Nur PENDING-Buchungen können akzeptiert werden");
        }

        // CARD_ON_SITE only makes sense for a BUSINESS owner with a physical
        // storefront — a private USER owner has no shop to swipe a card at.
        // CARD_ON_SITE ist nur für einen BUSINESS-Eigentümer mit
        // Ladengeschäft sinnvoll — ein privater USER-Eigentümer hat kein
        // Geschäft, an dem eine Karte gezogen werden könnte.
        if (paymentMethod == PaymentMethod.CARD_ON_SITE && booking.getOwner().getRole() != UserRole.BUSINESS) {
            throw new BusinessException(
                    "Card-on-site payment is only available for business accounts / " +
                    "Kartenzahlung vor Ort ist nur für Geschäftskonten verfügbar");
        }

        // Lock the bike row for the rest of this transaction, same as createBooking.
        // A renter can submit several overlapping PENDING requests for the same bike
        // (createBooking only blocks overlap against PENDING/ACCEPTED at request time,
        // and two different renters can both land in PENDING for the same dates). Without
        // re-checking here, an owner accepting two such requests in quick succession could
        // end up with two ACCEPTED bookings for the same dates — this lock plus the
        // conflict check below closes that gap.
        //
        // Sperrt die Fahrrad-Zeile für den Rest dieser Transaktion, wie bei createBooking.
        // Ein Mieter kann mehrere sich überschneidende PENDING-Anfragen für dasselbe Fahrrad
        // einreichen (createBooking blockiert Überschneidungen nur zum Anfragezeitpunkt gegen
        // PENDING/ACCEPTED, und zwei verschiedene Mieter können beide für dieselben Termine
        // PENDING landen). Ohne erneute Prüfung hier könnte ein Eigentümer, der zwei solche
        // Anfragen kurz nacheinander akzeptiert, am Ende zwei ACCEPTED-Buchungen für dieselben
        // Termine haben — diese Sperre plus die Konfliktprüfung unten schließt diese Lücke.
        bikeRepository.findByIdForUpdate(booking.getBike().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bike", booking.getBike().getId()));

        boolean hasConflict = bookingRepository.existsDateConflictExcluding(
                booking.getBike().getId(), booking.getStartDate(), booking.getEndDate(), booking.getId());
        if (hasConflict) {
            throw new BusinessException(
                    "Another booking for overlapping dates was already accepted / " +
                    "Eine andere Buchung für überlappende Termine wurde bereits akzeptiert");
        }

        booking.setStatus(BookingStatus.ACCEPTED);
        booking.setPaymentMethod(paymentMethod);
        bookingRepository.save(booking);

        // Auto-generate the frozen rental contract snapshot the instant the
        // booking is accepted — see ContractService's class Javadoc for why
        // this data must be denormalized rather than read live off Booking/
        // Bike/User at display time.
        // Automatisch die eingefrorene Mietvertrags-Momentaufnahme erstellen,
        // sobald die Buchung akzeptiert wird — siehe das Klassen-Javadoc von
        // ContractService dafür, warum diese Daten denormalisiert sein
        // müssen statt live von Booking/Bike/User zum Anzeigezeitpunkt
        // gelesen zu werden.
        contractService.generateForBooking(booking, paymentMethod);

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

        auditLogService.record(renterId, booking.getRenter().getFullName(), AuditAction.BOOKING_CANCELLED,
                "BOOKING", bookingId, null);

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

        long rentalDays = booking.getRentalDays();
        List<BookingAccessoryResponse> accessoryResponses = bookingAccessoryRepository
                .findByBookingId(booking.getId()).stream()
                .map(ba -> BookingAccessoryResponse.builder()
                        .accessoryId(ba.getAccessory().getId())
                        .type(ba.getAccessory().getType())
                        .name(ba.getAccessory().getName())
                        .quantity(ba.getQuantity())
                        .pricePerDayAtBooking(ba.getPricePerDayAtBooking())
                        .lineTotal(ba.getLineTotal(rentalDays))
                        .build())
                .toList();

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
                .paymentMethod(booking.getPaymentMethod())
                .accessories(accessoryResponses)
                // Timestamps / Zeitstempel
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                // Computed flags / Berechnete Flags
                .cancellable(booking.isCancellable())
                .reviewable(booking.getStatus() == BookingStatus.COMPLETED)
                .build();
    }
}
