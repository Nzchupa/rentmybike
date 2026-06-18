package com.rentmybike.accessory.controller;

import com.rentmybike.accessory.dto.AccessoryResponse;
import com.rentmybike.accessory.dto.CreateAccessoryRequest;
import com.rentmybike.accessory.dto.UpdateAccessoryRequest;
import com.rentmybike.accessory.service.AccessoryService;
import com.rentmybike.common.response.ApiResponse;
import com.rentmybike.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for business accessories (helmets, child seats, locks).
 * REST-Endpunkte für Business-Zubehör (Helme, Kindersitze, Schlösser).
 *
 * <p>Mutations require {@code ROLE_BUSINESS} and are restricted to the
 * caller's own accessories (enforced in {@code AccessoryService}). The
 * by-owner listing is public so renters can see add-on options when
 * browsing a business's bikes, without needing to be logged in.
 * <p>Änderungen erfordern {@code ROLE_BUSINESS} und sind auf die eigenen
 * Zubehörartikel des Aufrufers beschränkt (im {@code AccessoryService}
 * durchgesetzt). Die Auflistung nach Eigentümer ist öffentlich, damit
 * Mieter Add-on-Optionen beim Durchsuchen der Fahrräder eines Unternehmens
 * sehen können, ohne angemeldet zu sein.
 */
@RestController
@RequestMapping("/api/v1/accessories")
@RequiredArgsConstructor
public class AccessoryController {

    private final AccessoryService accessoryService;

    /**
     * Creates a new accessory — BUSINESS accounts only.
     * Erstellt ein neues Zubehör — nur für BUSINESS-Konten.
     *
     * <p>POST /api/v1/accessories → 201 Created
     */
    @PostMapping
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<ApiResponse<AccessoryResponse>> createAccessory(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateAccessoryRequest request) {

        AccessoryResponse created = accessoryService.createAccessory(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Accessory created / Zubehör erstellt"));
    }

    /**
     * Updates an existing accessory — owner only.
     * Aktualisiert ein vorhandenes Zubehör — nur Eigentümer.
     *
     * <p>PUT /api/v1/accessories/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<ApiResponse<AccessoryResponse>> updateAccessory(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateAccessoryRequest request) {

        AccessoryResponse updated = accessoryService.updateAccessory(id, currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Accessory updated / Zubehör aktualisiert"));
    }

    /**
     * Soft-deletes an accessory — owner only.
     * Löscht ein Zubehör soft — nur Eigentümer.
     *
     * <p>DELETE /api/v1/accessories/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<ApiResponse<Void>> deleteAccessory(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {

        accessoryService.deleteAccessory(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Accessory deleted / Zubehör gelöscht"));
    }

    /**
     * Lists the authenticated business's own accessories.
     * Listet die eigenen Zubehörartikel des authentifizierten Unternehmens auf.
     *
     * <p>GET /api/v1/accessories/my
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<ApiResponse<List<AccessoryResponse>>> getMyAccessories(
            @AuthenticationPrincipal User currentUser) {

        List<AccessoryResponse> accessories = accessoryService.getMyAccessories(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(accessories));
    }

    /**
     * Public listing of accessories offered by a given business.
     * Öffentliche Auflistung des von einem bestimmten Unternehmen angebotenen Zubehörs.
     *
     * <p>GET /api/v1/accessories/owner/{ownerId}
     */
    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<ApiResponse<List<AccessoryResponse>>> getAccessoriesByOwner(
            @PathVariable UUID ownerId) {

        List<AccessoryResponse> accessories = accessoryService.getAccessoriesByOwner(ownerId);
        return ResponseEntity.ok(ApiResponse.success(accessories));
    }
}
