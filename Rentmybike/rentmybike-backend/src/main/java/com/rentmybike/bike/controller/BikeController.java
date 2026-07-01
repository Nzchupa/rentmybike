package com.rentmybike.bike.controller;

import com.rentmybike.bike.dto.*;
import com.rentmybike.bike.entity.ApprovalStatus;
import com.rentmybike.bike.entity.BikeCategory;
import com.rentmybike.bike.service.BikeService;
import com.rentmybike.common.response.ApiResponse;
import com.rentmybike.common.response.PageResponse;
import com.rentmybike.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for bike listings.
 * REST-Controller für Fahrrad-Inserate.
 *
 * <p>Endpoint groups:
 * <ul>
 *   <li>/api/v1/bikes           — public + owner CRUD</li>
 *   <li>/api/v1/bikes/my        — owner's own bike list</li>
 *   <li>/api/v1/bikes/{id}/photos — photo management</li>
 *   <li>/api/v1/admin/bikes     — admin moderation</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
public class BikeController {

    private final BikeService bikeService;

    // ══════════════════════════════════════════════════════════════════════════
    // PUBLIC ENDPOINTS / ÖFFENTLICHE ENDPUNKTE
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Public bike search with optional filters and pagination.
     * Öffentliche Fahrradsuche mit optionalen Filtern und Seitennavigation.
     *
     * <p>GET /api/v1/bikes?city=Berlin&category=CITY&minPrice=10&maxPrice=50&page=0&size=20
     */
    @GetMapping("/api/v1/bikes")
    public ResponseEntity<ApiResponse<PageResponse<BikeResponse>>> searchBikes(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) BikeCategory category,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        // Cap page size to prevent abuse / Seitengröße begrenzen zur Missbrauchsverhinderung
        size = Math.min(size, 50);
        // searchPublic's @Query already has an explicit "ORDER BY b.createdAt DESC".
        // Passing a Pageable with its own Sort here made Spring Data JPA append a
        // second ORDER BY clause to the generated SQL, which Postgres rejects as a
        // syntax error — every search (with or without filters) failed with a 500,
        // which the bikes search page silently swallowed and rendered as "no bikes
        // found" instead of surfacing the real error. Keep this Pageable unsorted;
        // the query supplies the order.
        //
        // searchPublics @Query hat bereits ein explizites "ORDER BY b.createdAt
        // DESC". Ein Pageable mit eigenem Sort führte dazu, dass Spring Data JPA
        // eine zweite ORDER BY-Klausel an das generierte SQL anhängte, was Postgres
        // als Syntaxfehler ablehnt — jede Suche (mit oder ohne Filter) schlug mit
        // 500 fehl, was die Fahrrad-Suchseite stillschweigend verschluckte und als
        // "keine Fahrräder gefunden" anzeigte statt den eigentlichen Fehler. Dieses
        // Pageable bleibt unsortiert; die Query liefert die Sortierung.
        Pageable pageable = PageRequest.of(page, size);

        PageResponse<BikeResponse> result = bikeService.searchBikes(city, category, model, minPrice, maxPrice, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get a single bike by ID — only returns APPROVED bikes.
     * Einzelnes Fahrrad nach ID abrufen — gibt nur APPROVED-Fahrräder zurück.
     *
     * <p>GET /api/v1/bikes/{id}
     */
    @GetMapping("/api/v1/bikes/{id}")
    public ResponseEntity<ApiResponse<BikeResponse>> getPublicBike(
            @PathVariable UUID id) {

        BikeResponse bike = bikeService.getPublicBike(id);
        return ResponseEntity.ok(ApiResponse.success(bike));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // OWNER ENDPOINTS (AUTH REQUIRED) / EIGENTÜMER-ENDPUNKTE (AUTH ERFORDERLICH)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Create a new bike listing.
     * Neues Fahrrad-Inserat erstellen.
     *
     * <p>POST /api/v1/bikes → 201 Created
     * New bike starts in PENDING status — admin must approve before it appears publicly.
     */
    @PostMapping("/api/v1/bikes")
    public ResponseEntity<ApiResponse<BikeResponse>> createBike(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateBikeRequest request) {

        BikeResponse created = bikeService.createBike(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Bike listing created — pending admin review / Inserat erstellt — wartet auf Admin-Prüfung"));
    }

    /**
     * Bulk-create bike listings — BUSINESS accounts only.
     * Fahrrad-Inserate massenhaft erstellen — nur für BUSINESS-Konten.
     *
     * <p>POST /api/v1/bikes/bulk → 201 Created
     * Each bike starts in PENDING status, same as a single create.
     */
    @PostMapping("/api/v1/bikes/bulk")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<ApiResponse<List<BikeResponse>>> bulkCreateBikes(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody BulkCreateBikeRequest request) {

        List<BikeResponse> created = bikeService.bulkCreateBikes(currentUser.getId(), request.getBikes());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created,
                        created.size() + " bikes created — pending admin review / "
                                + created.size() + " Fahrräder erstellt — warten auf Admin-Prüfung"));
    }

    /**
     * List the authenticated owner's own bikes (all approval statuses).
     * Alle eigenen Fahrräder des authentifizierten Eigentümers auflisten (alle Genehmigungsstatus).
     *
     * <p>GET /api/v1/bikes/my
     */
    @GetMapping("/api/v1/bikes/my")
    public ResponseEntity<ApiResponse<PageResponse<BikeResponse>>> getMyBikes(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        // findByOwnerIdAndDeletedAtIsNull's @Query already orders by createdAt DESC —
        // see the comment on searchBikes() above for why Sort must not be passed here too.
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));

        PageResponse<BikeResponse> result = bikeService.getMyBikes(currentUser.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get owner's own bike by ID (includes private fields like address, rejectionReason).
     * Eigenes Fahrrad nach ID abrufen (inkl. private Felder wie Adresse, Ablehnungsgrund).
     *
     * <p>GET /api/v1/bikes/{id}/owner
     */
    @GetMapping("/api/v1/bikes/{id}/owner")
    public ResponseEntity<ApiResponse<BikeResponse>> getOwnerBike(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {

        BikeResponse bike = bikeService.getOwnerBike(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(bike));
    }

    /**
     * Owner: per-bike stats panel (view count + booking breakdown + revenue).
     * Eigentümer: Pro-Fahrrad-Statistikpanel (Aufrufzähler + Buchungsaufschlüsselung + Umsatz).
     *
     * <p>GET /api/v1/bikes/{id}/stats
     */
    @GetMapping("/api/v1/bikes/{id}/stats")
    public ResponseEntity<ApiResponse<BikeStatsResponse>> getBikeStats(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {

        BikeStatsResponse stats = bikeService.getBikeStats(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Update an existing bike listing (owner only).
     * Vorhandenes Fahrrad-Inserat aktualisieren (nur Eigentümer).
     *
     * <p>PUT /api/v1/bikes/{id}
     * Note: updating content of an APPROVED bike resets it to PENDING.
     */
    @PutMapping("/api/v1/bikes/{id}")
    public ResponseEntity<ApiResponse<BikeResponse>> updateBike(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateBikeRequest request) {

        BikeResponse updated = bikeService.updateBike(id, currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Bike updated / Fahrrad aktualisiert"));
    }

    /**
     * Soft-delete a bike (owner only).
     * Fahrrad soft-löschen (nur Eigentümer).
     *
     * <p>DELETE /api/v1/bikes/{id}
     */
    @DeleteMapping("/api/v1/bikes/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBike(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {

        bikeService.deleteBike(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Bike deleted / Fahrrad gelöscht"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PHOTO ENDPOINTS / FOTO-ENDPUNKTE
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Upload a photo to a bike (max 5).
     * Foto zu einem Fahrrad hochladen (max. 5).
     *
     * <p>POST /api/v1/bikes/{id}/photos (multipart/form-data, field: "file")
     */
    @PostMapping(value = "/api/v1/bikes/{id}/photos",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BikeResponse>> uploadPhoto(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            @RequestParam("file") MultipartFile file) {

        BikeResponse updated = bikeService.uploadPhoto(id, currentUser.getId(), file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(updated, "Photo uploaded / Foto hochgeladen"));
    }

    /**
     * Delete a photo from a bike (owner only).
     * Foto von einem Fahrrad löschen (nur Eigentümer).
     *
     * <p>DELETE /api/v1/bikes/{id}/photos/{photoId}
     */
    @DeleteMapping("/api/v1/bikes/{id}/photos/{photoId}")
    public ResponseEntity<ApiResponse<BikeResponse>> deletePhoto(
            @PathVariable UUID id,
            @PathVariable UUID photoId,
            @AuthenticationPrincipal User currentUser) {

        BikeResponse updated = bikeService.deletePhoto(id, photoId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(updated, "Photo deleted / Foto gelöscht"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ADMIN ENDPOINTS / ADMIN-ENDPUNKTE
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Admin: list all bikes, optionally filter by approval status.
     * Admin: alle Fahrräder auflisten, optional nach Genehmigungsstatus filtern.
     *
     * <p>GET /api/v1/admin/bikes?status=PENDING
     */
    @GetMapping("/api/v1/admin/bikes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<BikeResponse>>> adminListBikes(
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        // findAllForAdmin's @Query already orders by createdAt ASC — see the comment
        // on searchBikes() above for why Sort must not be passed here too.
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));

        PageResponse<BikeResponse> result = bikeService.adminListBikes(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Admin: get a single bike by ID (any approval status).
     * Admin: einzelnes Fahrrad nach ID abrufen (jeder Genehmigungsstatus).
     *
     * <p>GET /api/v1/admin/bikes/{id}
     */
    @GetMapping("/api/v1/admin/bikes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BikeResponse>> adminGetBike(@PathVariable UUID id) {
        BikeResponse bike = bikeService.adminGetBike(id);
        return ResponseEntity.ok(ApiResponse.success(bike));
    }

    /**
     * Admin: per-bike stats panel — same data as the owner endpoint, no
     * ownership check.
     * Admin: Pro-Fahrrad-Statistikpanel — gleiche Daten wie der
     * Eigentümer-Endpunkt, ohne Eigentümerschaftsprüfung.
     *
     * <p>GET /api/v1/admin/bikes/{id}/stats
     */
    @GetMapping("/api/v1/admin/bikes/{id}/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BikeStatsResponse>> adminGetBikeStats(@PathVariable UUID id) {
        BikeStatsResponse stats = bikeService.adminGetBikeStats(id);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Admin: approve a bike listing — makes it visible in public search.
     * Admin: Fahrrad-Inserat genehmigen — macht es in öffentlicher Suche sichtbar.
     *
     * <p>POST /api/v1/admin/bikes/{id}/approve
     */
    @PostMapping("/api/v1/admin/bikes/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BikeResponse>> approveBike(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentAdmin) {
        BikeResponse approved = bikeService.approveBike(id, currentAdmin.getId(), currentAdmin.getFullName());
        return ResponseEntity.ok(ApiResponse.success(approved, "Bike approved / Fahrrad genehmigt"));
    }

    /**
     * Admin: reject a bike listing with a mandatory reason.
     * Admin: Fahrrad-Inserat mit obligatorischem Grund ablehnen.
     *
     * <p>POST /api/v1/admin/bikes/{id}/reject
     */
    @PostMapping("/api/v1/admin/bikes/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BikeResponse>> rejectBike(
            @PathVariable UUID id,
            @Valid @RequestBody RejectBikeRequest request,
            @AuthenticationPrincipal User currentAdmin) {

        BikeResponse rejected = bikeService.rejectBike(id, request, currentAdmin.getId(), currentAdmin.getFullName());
        return ResponseEntity.ok(ApiResponse.success(rejected, "Bike rejected / Fahrrad abgelehnt"));
    }

    /**
     * Admin: request specific changes on a bike listing — softer than a full
     * rejection. The owner sees the feedback and the bike automatically
     * returns to PENDING the next time they save an edit.
     * Admin: konkrete Änderungen an einem Fahrrad-Inserat anfordern — milder
     * als eine vollständige Ablehnung. Der Eigentümer sieht das Feedback und
     * das Fahrrad kehrt bei der nächsten gespeicherten Bearbeitung
     * automatisch zu PENDING zurück.
     *
     * <p>POST /api/v1/admin/bikes/{id}/request-changes
     */
    @PostMapping("/api/v1/admin/bikes/{id}/request-changes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BikeResponse>> requestChanges(
            @PathVariable UUID id,
            @Valid @RequestBody RejectBikeRequest request,
            @AuthenticationPrincipal User currentAdmin) {

        BikeResponse updated = bikeService.requestChanges(id, request, currentAdmin.getId(), currentAdmin.getFullName());
        return ResponseEntity.ok(ApiResponse.success(updated, "Changes requested / Änderungen angefordert"));
    }
}
