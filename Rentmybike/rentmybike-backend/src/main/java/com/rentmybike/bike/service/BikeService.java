package com.rentmybike.bike.service;

import com.rentmybike.audit.entity.AuditAction;
import com.rentmybike.audit.service.AuditLogService;
import com.rentmybike.bike.dto.*;
import com.rentmybike.bike.entity.ApprovalStatus;
import com.rentmybike.bike.entity.Bike;
import com.rentmybike.bike.entity.BikeCategory;
import com.rentmybike.bike.entity.BikePhoto;
import com.rentmybike.bike.repository.BikePhotoRepository;
import com.rentmybike.bike.repository.BikeRepository;
import com.rentmybike.booking.entity.BookingStatus;
import com.rentmybike.booking.repository.BookingRepository;
import com.rentmybike.common.exception.AccessDeniedException;
import com.rentmybike.common.exception.BusinessException;
import com.rentmybike.common.exception.ResourceNotFoundException;
import com.rentmybike.common.response.PageResponse;
import com.rentmybike.common.service.CloudinaryService;
import com.rentmybike.user.entity.User;
import com.rentmybike.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Service for bike listing management.
 * Service für die Fahrrad-Inseratverwaltung.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>CRUD for bike listings</li>
 *   <li>Photo upload / delete (max 5 per bike)</li>
 *   <li>Public search with filters + pagination</li>
 *   <li>Admin reactive moderation (approve / reject / request changes) — new
 *       bikes are auto-approved, this is only a post-publication takedown
 *       tool now, not a pre-publication gate</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BikeService {

    private static final int MAX_PHOTOS_PER_BIKE = 5;
    private static final String BIKE_PHOTOS_FOLDER = "rentmybike/bikes";

    private final BikeRepository bikeRepository;
    private final BikePhotoRepository bikePhotoRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final BookingRepository bookingRepository;
    private final AuditLogService auditLogService;

    // ──────────────────────────────────────────────────────────────────────────
    // CREATE / ERSTELLEN
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new bike listing for the authenticated owner.
     * Erstellt ein neues Fahrrad-Inserat für den authentifizierten Eigentümer.
     *
     * <p>New bikes are auto-approved and immediately visible in public search —
     * admin moderation no longer gates individual bike listings, only
     * user/business accounts (see AdminService). An admin can still reject a
     * listing after the fact (e.g. in response to a report).
     * <p>Neue Fahrräder werden automatisch genehmigt und sind sofort in der
     * öffentlichen Suche sichtbar — die Admin-Moderation blockiert nicht mehr
     * einzelne Fahrrad-Inserate, sondern nur noch Benutzer-/Geschäftskonten
     * (siehe AdminService). Ein Admin kann ein Inserat weiterhin nachträglich
     * ablehnen (z. B. nach einer Meldung).
     *
     * @param ownerId owner's UUID / UUID des Eigentümers
     * @param request listing data / Inserat-Daten
     * @return created bike response / erstellte Fahrrad-Antwort
     */
    public BikeResponse createBike(UUID ownerId, CreateBikeRequest request) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", ownerId));

        Bike bike = Bike.builder()
                .owner(owner)
                .title(request.getTitle().strip())
                .description(request.getDescription().strip())
                .model(normalizeModel(request.getModel()))
                .category(request.getCategory())
                .pricePerDay(request.getPricePerDay())
                .depositAmount(request.getDepositAmount())
                .city(request.getCity().strip())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .available(true)
                // Admin moderation now only covers users/business accounts, not
                // individual bike listings — new bikes go straight to APPROVED
                // and are immediately visible in public search. The ApprovalStatus
                // workflow (PENDING/REJECTED/CHANGES_REQUESTED) and the admin
                // approve/reject/request-changes endpoints in BikeController are
                // kept as-is so a bad listing can still be taken down reactively
                // (e.g. after a report), just without a pre-publication gate.
                // Admin-Moderation deckt jetzt nur noch Benutzer-/Geschäftskonten
                // ab, nicht einzelne Fahrrad-Inserate — neue Fahrräder gehen
                // direkt auf APPROVED und sind sofort in der öffentlichen Suche
                // sichtbar. Der ApprovalStatus-Workflow und die Admin-Endpunkte
                // approve/reject/request-changes in BikeController bleiben
                // unverändert bestehen, damit ein schlechtes Inserat weiterhin
                // reaktiv entfernt werden kann (z. B. nach einer Meldung), nur
                // ohne Vorab-Veröffentlichungssperre.
                .approvalStatus(ApprovalStatus.APPROVED)
                .build();

        bikeRepository.save(bike);
        log.info("Bike created (auto-approved): {} by owner: {} / Fahrrad erstellt (automatisch genehmigt): {} von Eigentümer: {}",
                bike.getId(), ownerId, bike.getId(), ownerId);

        return toBikeResponse(bike, true);
    }

    /**
     * Creates multiple bike listings at once for the authenticated BUSINESS
     * owner — lets bike shops onboard their fleet in one request instead of
     * one-by-one. Each entry is auto-approved, same as a single-bike create.
     * Erstellt mehrere Fahrrad-Inserate gleichzeitig für den authentifizierten
     * BUSINESS-Eigentümer — ermöglicht es Fahrradläden, ihre Flotte in einer
     * Anfrage anzulegen, statt einzeln. Jeder Eintrag wird automatisch
     * genehmigt, genauso wie bei einer Einzel-Erstellung.
     *
     * <p>Not wrapped in a single all-or-nothing check beyond the surrounding
     * {@code @Transactional} on the class — a validation failure on any one
     * entry (handled by {@code @Valid} at the controller) rejects the whole
     * batch before any row is written.
     *
     * @param ownerId  owner's UUID / UUID des Eigentümers
     * @param requests list of listing data, 1–50 entries / Liste der Inserat-Daten, 1–50 Einträge
     * @return created bike responses, same order as the request / erstellte Fahrrad-Antworten, gleiche Reihenfolge wie die Anfrage
     */
    public List<BikeResponse> bulkCreateBikes(UUID ownerId, List<CreateBikeRequest> requests) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", ownerId));

        List<Bike> bikes = requests.stream()
                .map(request -> Bike.builder()
                        .owner(owner)
                        .title(request.getTitle().strip())
                        .description(request.getDescription().strip())
                        .model(normalizeModel(request.getModel()))
                        .category(request.getCategory())
                        .pricePerDay(request.getPricePerDay())
                        .depositAmount(request.getDepositAmount())
                        .city(request.getCity().strip())
                        .address(request.getAddress())
                        .latitude(request.getLatitude())
                        .longitude(request.getLongitude())
                        .available(true)
                        // Same auto-approval as the single-bike create above —
                        // see the comment there for why. / Gleiche automatische
                        // Genehmigung wie bei der Einzel-Erstellung oben —
                        // siehe dortigen Kommentar für den Grund.
                        .approvalStatus(ApprovalStatus.APPROVED)
                        .build())
                .toList();

        bikeRepository.saveAll(bikes);
        log.info("{} bikes bulk-created (auto-approved) by owner: {} / {} Fahrräder massenhaft erstellt (automatisch genehmigt) von Eigentümer: {}",
                bikes.size(), ownerId, bikes.size(), ownerId);

        return bikes.stream().map(bike -> toBikeResponse(bike, true)).toList();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // READ / LESEN
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Public search — only APPROVED + available + non-deleted bikes.
     * Öffentliche Suche — nur APPROVED + verfügbare + nicht gelöschte Fahrräder.
     *
     * @param city      optional city filter / optionaler Stadtfilter
     * @param category  optional category filter / optionaler Kategoriefilter
     * @param model     optional brand/model filter (substring match) / optionaler Marke/Modell-Filter (Teilstring)
     * @param minPrice  optional min price filter / optionaler Mindestpreisfilter
     * @param maxPrice  optional max price filter / optionaler Höchstpreisfilter
     * @param ownerId   optional owner filter — powers the "bikes by this owner" list
     *                  on the public profile page / optionaler Eigentümer-Filter —
     *                  versorgt die "Fahrräder dieses Eigentümers"-Liste auf der
     *                  öffentlichen Profilseite
     * @param pageable  pagination / Seitennavigation
     */
    @Transactional(readOnly = true)
    public PageResponse<BikeResponse> searchBikes(
            String city, BikeCategory category, String model,
            BigDecimal minPrice, BigDecimal maxPrice, UUID ownerId,
            Pageable pageable) {

        Page<Bike> page = bikeRepository.searchPublic(city, category, model, minPrice, maxPrice, ownerId, pageable);
        // Public view: no address, no rejection reason / Öffentliche Ansicht: keine Adresse, kein Ablehnungsgrund
        return PageResponse.from(page.map(bike -> toBikeResponse(bike, false)));
    }

    /**
     * Get a single bike by ID — public endpoint, only returns APPROVED bikes.
     * Einzelnes Fahrrad nach ID abrufen — öffentlicher Endpunkt, nur APPROVED-Fahrräder.
     */
    // Not readOnly: this method also increments the view counter via a
    // @Modifying query, which cannot run inside a read-only transaction.
    // The increment happens after the visibility check, in the same
    // transaction as the fetch, so the response below already reflects it.
    //
    // Nicht readOnly: diese Methode erhöht auch den Aufrufzähler über eine
    // @Modifying-Abfrage, die nicht innerhalb einer Read-Only-Transaktion
    // ausgeführt werden kann. Die Erhöhung erfolgt nach der
    // Sichtbarkeitsprüfung, in derselben Transaktion wie der Abruf, sodass
    // die untenstehende Antwort sie bereits berücksichtigt.
    @Transactional
    public BikeResponse getPublicBike(UUID bikeId) {
        Bike bike = bikeRepository.findByIdWithDetails(bikeId)
                .orElseThrow(() -> new ResourceNotFoundException("Bike", bikeId));

        if (!bike.isPubliclyVisible()) {
            throw new ResourceNotFoundException("Bike", bikeId);
        }

        bikeRepository.incrementViewCount(bikeId);
        bike.setViewCount(bike.getViewCount() + 1); // keep in-memory entity in sync for the response below

        return toBikeResponse(bike, false);
    }

    /**
     * Get a bike by ID for its owner (includes all fields, any approval status).
     * Fahrrad nach ID für Eigentümer abrufen (alle Felder, jeder Genehmigungsstatus).
     */
    @Transactional(readOnly = true)
    public BikeResponse getOwnerBike(UUID bikeId, UUID ownerId) {
        Bike bike = bikeRepository.findByIdWithDetails(bikeId)
                .orElseThrow(() -> new ResourceNotFoundException("Bike", bikeId));

        requireOwner(bike, ownerId);
        return toBikeResponse(bike, true); // owner sees address + rejection reason
    }

    /**
     * List all bikes owned by a user (for "My Bikes" page).
     * Alle Fahrräder eines Benutzers auflisten (für "Meine Fahrräder"-Seite).
     */
    @Transactional(readOnly = true)
    public PageResponse<BikeResponse> getMyBikes(UUID ownerId, Pageable pageable) {
        Page<Bike> page = bikeRepository.findByOwnerIdAndDeletedAtIsNull(ownerId, pageable);
        return PageResponse.from(page.map(bike -> toBikeResponse(bike, true)));
    }

    /**
     * Admin: list all bikes, optionally filtered by approval status.
     * Admin: alle Fahrräder auflisten, optional nach Genehmigungsstatus gefiltert.
     */
    @Transactional(readOnly = true)
    public PageResponse<BikeResponse> adminListBikes(ApprovalStatus status, Pageable pageable) {
        Page<Bike> page = bikeRepository.findAllForAdmin(status, pageable);
        return PageResponse.from(page.map(bike -> toBikeResponse(bike, true)));
    }

    /**
     * Admin: get a single bike (any status).
     * Admin: einzelnes Fahrrad abrufen (jeder Status).
     */
    @Transactional(readOnly = true)
    public BikeResponse adminGetBike(UUID bikeId) {
        Bike bike = bikeRepository.findByIdWithDetails(bikeId)
                .orElseThrow(() -> new ResourceNotFoundException("Bike", bikeId));
        return toBikeResponse(bike, true);
    }

    /**
     * Owner: per-bike stats panel (view count + booking breakdown + revenue).
     * Eigentümer: Pro-Fahrrad-Statistikpanel (Aufrufzähler + Buchungsaufschlüsselung + Umsatz).
     */
    @Transactional(readOnly = true)
    public BikeStatsResponse getBikeStats(UUID bikeId, UUID ownerId) {
        Bike bike = bikeRepository.findById(bikeId)
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Bike", bikeId));

        requireOwner(bike, ownerId);
        return buildBikeStats(bike);
    }

    /**
     * Admin: per-bike stats panel — same data as {@link #getBikeStats}, no
     * ownership check.
     * Admin: Pro-Fahrrad-Statistikpanel — gleiche Daten wie {@link
     * #getBikeStats}, ohne Eigentümerschaftsprüfung.
     */
    @Transactional(readOnly = true)
    public BikeStatsResponse adminGetBikeStats(UUID bikeId) {
        Bike bike = bikeRepository.findById(bikeId)
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Bike", bikeId));

        return buildBikeStats(bike);
    }

    /**
     * Shared aggregation logic behind {@link #getBikeStats} / {@link #adminGetBikeStats}.
     * Geteilte Aggregationslogik hinter {@link #getBikeStats} / {@link #adminGetBikeStats}.
     */
    private BikeStatsResponse buildBikeStats(Bike bike) {
        UUID bikeId = bike.getId();

        return BikeStatsResponse.builder()
                .bikeId(bikeId)
                .viewCount(bike.getViewCount())
                .totalBookings(bookingRepository.countByBikeIdAndDeletedAtIsNull(bikeId))
                .pendingBookings(bookingRepository.countByBikeIdAndStatusAndDeletedAtIsNull(bikeId, BookingStatus.PENDING))
                .acceptedBookings(bookingRepository.countByBikeIdAndStatusAndDeletedAtIsNull(bikeId, BookingStatus.ACCEPTED))
                .completedBookings(bookingRepository.countByBikeIdAndStatusAndDeletedAtIsNull(bikeId, BookingStatus.COMPLETED))
                .cancelledBookings(bookingRepository.countByBikeIdAndStatusAndDeletedAtIsNull(bikeId, BookingStatus.CANCELLED))
                .rejectedBookings(bookingRepository.countByBikeIdAndStatusAndDeletedAtIsNull(bikeId, BookingStatus.REJECTED))
                .totalRevenue(bookingRepository.sumTotalPriceOfCompletedByBikeId(bikeId))
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // UPDATE / AKTUALISIEREN
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Owner updates their bike listing.
     * Eigentümer aktualisiert sein Fahrrad-Inserat.
     *
     * <p>Since bikes are no longer gated behind admin approval (see {@link
     * #createBike}), an edit to an already-APPROVED bike no longer bounces it
     * back to PENDING — that used to silently reintroduce a per-edit
     * moderation gate, which defeats the point of removing bike verification.
     * The one exception: a bike an admin explicitly flagged as
     * CHANGES_REQUESTED still returns to PENDING on the owner's next save, so
     * the admin's reactive feedback loop still closes.
     * <p>Da Fahrräder nicht mehr hinter einer Admin-Genehmigung stehen (siehe
     * {@link #createBike}), springt ein bereits genehmigtes Fahrrad bei einer
     * Bearbeitung nicht mehr auf PENDING zurück — das hätte sonst stillschweigend
     * pro Bearbeitung wieder ein Moderations-Gate eingeführt, was den Sinn der
     * entfernten Fahrrad-Verifizierung zunichtemachen würde. Einzige Ausnahme:
     * ein Fahrrad, das ein Admin explizit als CHANGES_REQUESTED markiert hat,
     * kehrt bei der nächsten Speicherung des Eigentümers weiterhin zu PENDING
     * zurück, damit sich die reaktive Feedback-Schleife des Admins schließt.
     */
    public BikeResponse updateBike(UUID bikeId, UUID ownerId, UpdateBikeRequest request) {
        Bike bike = bikeRepository.findByIdWithDetails(bikeId)
                .orElseThrow(() -> new ResourceNotFoundException("Bike", bikeId));

        requireOwner(bike, ownerId);

        String newModel = normalizeModel(request.getModel());

        bike.setTitle(request.getTitle().strip());
        bike.setDescription(request.getDescription().strip());
        bike.setModel(newModel);
        bike.setCategory(request.getCategory());
        bike.setPricePerDay(request.getPricePerDay());
        bike.setDepositAmount(request.getDepositAmount());
        bike.setCity(request.getCity().strip());
        bike.setAddress(request.getAddress());
        bike.setLatitude(request.getLatitude());
        bike.setLongitude(request.getLongitude());
        bike.setAvailable(request.getAvailable());

        // Only CHANGES_REQUESTED (an explicit admin action) still routes back
        // through PENDING on save — plain content edits to an APPROVED bike
        // no longer do.
        // Nur CHANGES_REQUESTED (eine explizite Admin-Aktion) läuft bei der
        // Speicherung weiterhin über PENDING — einfache Inhaltsänderungen an
        // einem APPROVED-Fahrrad tun das nicht mehr.
        if (bike.getApprovalStatus() == ApprovalStatus.CHANGES_REQUESTED) {
            bike.setApprovalStatus(ApprovalStatus.PENDING);
            bike.setRejectionReason(null);
            log.info("Bike {} reset to PENDING after owner addressed requested changes / "
                    + "Fahrrad {} nach Bearbeitung der angeforderten Änderungen auf PENDING zurückgesetzt",
                    bikeId, bikeId);
        }

        bikeRepository.save(bike);
        return toBikeResponse(bike, true);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // DELETE / LÖSCHEN
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Soft-deletes a bike (sets deleted_at timestamp).
     * Löscht ein Fahrrad soft (setzt deleted_at-Zeitstempel).
     *
     * <p>Refuses to delete while the bike has any PENDING/ACCEPTED booking —
     * otherwise a renter could be left with a confirmed rental for a bike that
     * silently disappeared. The owner must wait for those bookings to resolve
     * (complete/cancel/reject) before deleting the listing.
     * <p>Verweigert die Löschung, solange das Fahrrad eine PENDING/ACCEPTED-Buchung
     * hat — sonst könnte ein Mieter mit einer bestätigten Mietbuchung für ein
     * Fahrrad zurückbleiben, das stillschweigend verschwunden ist. Der Eigentümer
     * muss warten, bis diese Buchungen abgeschlossen sind (completed/cancelled/
     * rejected), bevor das Inserat gelöscht werden kann.
     *
     * <p>Photos are NOT removed from Cloudinary immediately — a background cleanup
     * job would handle that in production. For now, only the DB row is soft-deleted.
     * <p>Fotos werden nicht sofort von Cloudinary entfernt — ein Hintergrundbereinigungsjob
     * würde das in der Produktion übernehmen.
     */
    public void deleteBike(UUID bikeId, UUID ownerId) {
        Bike bike = bikeRepository.findById(bikeId)
                .orElseThrow(() -> new ResourceNotFoundException("Bike", bikeId));

        requireOwner(bike, ownerId);

        if (bookingRepository.existsActiveBookingsForBike(bikeId)) {
            throw new BusinessException(
                    "Cannot delete a bike with pending or accepted bookings — resolve them first / "
                    + "Ein Fahrrad mit ausstehenden oder akzeptierten Buchungen kann nicht gelöscht werden — "
                    + "bitte diese zuerst abschließen");
        }

        bike.softDelete(); // from BaseEntity
        bikeRepository.save(bike);

        log.info("Bike soft-deleted: {} by owner: {} / Fahrrad soft-gelöscht: {} von Eigentümer: {}",
                bikeId, ownerId, bikeId, ownerId);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PHOTOS / FOTOS
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Uploads a photo to a bike (max 5 per bike).
     * Lädt ein Foto zu einem Fahrrad hoch (max. 5 pro Fahrrad).
     *
     * <p>First photo uploaded automatically becomes the primary (cover) photo.
     * <p>Das erste hochgeladene Foto wird automatisch zum Primär- (Titel-) Foto.
     *
     * @return updated bike response with new photo list
     */
    public BikeResponse uploadPhoto(UUID bikeId, UUID ownerId, MultipartFile file) {
        Bike bike = bikeRepository.findByIdWithDetails(bikeId)
                .orElseThrow(() -> new ResourceNotFoundException("Bike", bikeId));

        requireOwner(bike, ownerId);

        int currentCount = bikePhotoRepository.countByBikeId(bikeId);
        if (currentCount >= MAX_PHOTOS_PER_BIKE) {
            throw new BusinessException(
                    "Maximum " + MAX_PHOTOS_PER_BIKE + " photos allowed per bike / "
                    + "Maximal " + MAX_PHOTOS_PER_BIKE + " Fotos pro Fahrrad erlaubt");
        }

        String url = cloudinaryService.uploadImage(file, BIKE_PHOTOS_FOLDER);

        int nextOrder = bikePhotoRepository.findMaxDisplayOrderByBikeId(bikeId) + 1;
        boolean isPrimary = (currentCount == 0); // first photo = primary

        BikePhoto photo = BikePhoto.builder()
                .bike(bike)
                .url(url)
                .displayOrder(nextOrder)
                .primary(isPrimary)
                .build();

        bikePhotoRepository.save(photo);
        log.info("Photo uploaded to bike: {} / Foto zu Fahrrad hochgeladen: {}", bikeId, bikeId);

        // Reload to get fresh photo list / Neu laden für aktuelle Fotoliste
        Bike refreshed = bikeRepository.findByIdWithDetails(bikeId).orElseThrow();
        return toBikeResponse(refreshed, true);
    }

    /**
     * Deletes a single photo from a bike.
     * Löscht ein einzelnes Foto von einem Fahrrad.
     *
     * <p>If the deleted photo was primary, the next photo (lowest displayOrder) becomes primary.
     * <p>Wenn das gelöschte Foto das primäre war, wird das nächste Foto (niedrigster displayOrder) primär.
     */
    public BikeResponse deletePhoto(UUID bikeId, UUID photoId, UUID ownerId) {
        Bike bike = bikeRepository.findByIdWithDetails(bikeId)
                .orElseThrow(() -> new ResourceNotFoundException("Bike", bikeId));

        requireOwner(bike, ownerId);

        BikePhoto photo = bikePhotoRepository.findByIdAndBikeId(photoId, bikeId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo", photoId));

        boolean wasPrimary = photo.isPrimary();

        // Delete from Cloudinary / Von Cloudinary löschen
        cloudinaryService.deleteImage(photo.getUrl());
        bikePhotoRepository.delete(photo);

        // If deleted photo was primary, promote the next one / Wenn gelöschtes Foto primär war, nächstes befördern
        if (wasPrimary) {
            List<BikePhoto> remaining = bikePhotoRepository.findByBikeIdOrderByDisplayOrderAsc(bikeId);
            if (!remaining.isEmpty()) {
                BikePhoto newPrimary = remaining.get(0);
                newPrimary.setPrimary(true);
                bikePhotoRepository.save(newPrimary);
            }
        }

        log.info("Photo {} deleted from bike {} / Foto {} von Fahrrad {} gelöscht",
                photoId, bikeId, photoId, bikeId);

        Bike refreshed = bikeRepository.findByIdWithDetails(bikeId).orElseThrow();
        return toBikeResponse(refreshed, true);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // ADMIN APPROVAL / ADMIN-GENEHMIGUNG
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Admin approves a bike listing — makes it visible in public search.
     * Admin genehmigt ein Fahrrad-Inserat — macht es in öffentlicher Suche sichtbar.
     */
    public BikeResponse approveBike(UUID bikeId, UUID adminId, String adminName) {
        Bike bike = bikeRepository.findByIdWithDetails(bikeId)
                .orElseThrow(() -> new ResourceNotFoundException("Bike", bikeId));

        if (bike.getApprovalStatus() == ApprovalStatus.APPROVED) {
            throw new BusinessException("Bike is already approved / Fahrrad ist bereits genehmigt");
        }

        bike.setApprovalStatus(ApprovalStatus.APPROVED);
        bike.setRejectionReason(null);
        bikeRepository.save(bike);

        log.info("Bike APPROVED: {} / Fahrrad GENEHMIGT: {}", bikeId, bikeId);

        auditLogService.record(adminId, adminName, AuditAction.BIKE_APPROVED,
                "BIKE", bikeId, null);

        return toBikeResponse(bike, true);
    }

    /**
     * Admin rejects a bike listing with a mandatory reason.
     * Admin lehnt ein Fahrrad-Inserat mit einem obligatorischen Grund ab.
     *
     * <p>The owner will see the rejection reason so they can fix and resubmit.
     * <p>Der Eigentümer sieht den Ablehnungsgrund, damit er Probleme beheben und erneut einreichen kann.
     */
    public BikeResponse rejectBike(UUID bikeId, RejectBikeRequest request, UUID adminId, String adminName) {
        Bike bike = bikeRepository.findByIdWithDetails(bikeId)
                .orElseThrow(() -> new ResourceNotFoundException("Bike", bikeId));

        // Guard against re-rejecting an already-rejected bike — symmetric
        // with the already-approved guard in approveBike() above.
        // Schutz vor erneuter Ablehnung eines bereits abgelehnten Fahrrads —
        // symmetrisch zum "bereits genehmigt"-Schutz in approveBike() oben.
        if (bike.getApprovalStatus() == ApprovalStatus.REJECTED) {
            throw new BusinessException("Bike is already rejected / Fahrrad ist bereits abgelehnt");
        }

        bike.setApprovalStatus(ApprovalStatus.REJECTED);
        bike.setRejectionReason(request.getReason().strip());
        bikeRepository.save(bike);

        log.info("Bike REJECTED: {} / Fahrrad ABGELEHNT: {}", bikeId, bikeId);

        auditLogService.record(adminId, adminName, AuditAction.BIKE_REJECTED,
                "BIKE", bikeId, bike.getRejectionReason());

        return toBikeResponse(bike, true);
    }

    /**
     * Admin requests changes on a bike listing — a softer alternative to
     * {@link #rejectBike} for when the listing is close but needs specific
     * fixes (e.g. better photos, clearer description) rather than an outright
     * rejection. The owner sees the feedback via the same {@code
     * rejectionReason} field, and {@link #updateBike} automatically returns
     * the bike to PENDING as soon as the owner saves an edit, so the admin
     * sees it again in the moderation queue without a separate "resubmit"
     * action.
     * Admin fordert Änderungen an einem Fahrrad-Inserat an — eine mildere
     * Alternative zu {@link #rejectBike}, wenn das Inserat fast fertig ist,
     * aber konkrete Korrekturen benötigt (z. B. bessere Fotos, klarere
     * Beschreibung), statt einer vollständigen Ablehnung. Der Eigentümer
     * sieht das Feedback über dasselbe Feld {@code rejectionReason}, und
     * {@link #updateBike} setzt das Fahrrad automatisch auf PENDING zurück,
     * sobald der Eigentümer eine Bearbeitung speichert — ohne separate
     * "Erneut einreichen"-Aktion.
     */
    public BikeResponse requestChanges(UUID bikeId, RejectBikeRequest request, UUID adminId, String adminName) {
        Bike bike = bikeRepository.findByIdWithDetails(bikeId)
                .orElseThrow(() -> new ResourceNotFoundException("Bike", bikeId));

        if (bike.getApprovalStatus() == ApprovalStatus.APPROVED) {
            throw new BusinessException(
                    "Cannot request changes on an already-approved bike — reject it instead if needed "
                            + "/ Änderungen können bei einem bereits genehmigten Fahrrad nicht angefordert werden — bei Bedarf stattdessen ablehnen");
        }
        if (bike.getApprovalStatus() == ApprovalStatus.CHANGES_REQUESTED) {
            throw new BusinessException(
                    "Changes have already been requested for this bike / Für dieses Fahrrad wurden bereits Änderungen angefordert");
        }

        bike.setApprovalStatus(ApprovalStatus.CHANGES_REQUESTED);
        bike.setRejectionReason(request.getReason().strip());
        bikeRepository.save(bike);

        log.info("Bike CHANGES_REQUESTED: {} / Fahrrad ÄNDERUNGEN_ANGEFORDERT: {}", bikeId, bikeId);

        auditLogService.record(adminId, adminName, AuditAction.BIKE_CHANGES_REQUESTED,
                "BIKE", bikeId, bike.getRejectionReason());

        return toBikeResponse(bike, true);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers / Private Hilfsmethoden
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Normalizes an optional model input: strips whitespace, treats a
     * blank result as "not provided" (null) rather than storing an empty
     * string.
     * Normalisiert eine optionale Modell-Eingabe: entfernt Leerzeichen,
     * behandelt ein leeres Ergebnis als "nicht angegeben" (null) statt
     * einen leeren String zu speichern.
     */
    private String normalizeModel(String model) {
        if (model == null) return null;
        String stripped = model.strip();
        return stripped.isEmpty() ? null : stripped;
    }

    /**
     * Verifies that the given user is the owner of the bike.
     * Überprüft, dass der gegebene Benutzer der Eigentümer des Fahrrads ist.
     *
     * @throws AccessDeniedException if not the owner / wenn nicht der Eigentümer
     */
    private void requireOwner(Bike bike, UUID userId) {
        if (!bike.getOwner().getId().equals(userId)) {
            throw new AccessDeniedException(
                    "You are not the owner of this bike / Sie sind nicht der Eigentümer dieses Fahrrads");
        }
    }

    /**
     * Maps a Bike entity to a BikeResponse DTO.
     * Mappt eine Bike-Entity auf ein BikeResponse-DTO.
     *
     * @param includePrivate if true, includes address and rejectionReason
     *                       wenn true, werden Adresse und Ablehnungsgrund einbezogen
     */
    private BikeResponse toBikeResponse(Bike bike, boolean includePrivate) {
        List<BikePhotoResponse> photoResponses = bike.getPhotos().stream()
                .map(p -> BikePhotoResponse.builder()
                        .id(p.getId())
                        .url(p.getUrl())
                        .displayOrder(p.getDisplayOrder())
                        .primary(p.isPrimary())
                        .build())
                .toList();

        String primaryUrl = bike.getPhotos().stream()
                .filter(BikePhoto::isPrimary)
                .map(BikePhoto::getUrl)
                .findFirst()
                .orElse(null);

        return BikeResponse.builder()
                .id(bike.getId())
                .ownerId(bike.getOwner().getId())
                .ownerName(bike.getOwner().getFullName())
                .ownerAvatarUrl(bike.getOwner().getAvatarUrl())
                .title(bike.getTitle())
                .description(bike.getDescription())
                .model(bike.getModel())
                .category(bike.getCategory())
                .pricePerDay(bike.getPricePerDay())
                // Always visible, even in the public view — a renter must know about a
                // deposit requirement before booking, unlike address/rejectionReason.
                // Immer sichtbar, auch in der öffentlichen Ansicht — ein Mieter muss vor
                // der Buchung von einer Kautionspflicht wissen, anders als Adresse/Ablehnungsgrund.
                .depositAmount(bike.getDepositAmount())
                .city(bike.getCity())
                .address(includePrivate ? bike.getAddress() : null)
                .latitude(bike.getLatitude())
                .longitude(bike.getLongitude())
                .available(bike.isAvailable())
                .approvalStatus(bike.getApprovalStatus())
                .rejectionReason(includePrivate ? bike.getRejectionReason() : null)
                .photos(photoResponses)
                .primaryPhotoUrl(primaryUrl)
                .viewCount(bike.getViewCount())
                .createdAt(bike.getCreatedAt())
                .updatedAt(bike.getUpdatedAt())
                .build();
    }
}
