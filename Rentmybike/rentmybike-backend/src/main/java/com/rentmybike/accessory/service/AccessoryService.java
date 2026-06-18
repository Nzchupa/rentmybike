package com.rentmybike.accessory.service;

import com.rentmybike.accessory.dto.AccessoryResponse;
import com.rentmybike.accessory.dto.CreateAccessoryRequest;
import com.rentmybike.accessory.dto.UpdateAccessoryRequest;
import com.rentmybike.accessory.entity.Accessory;
import com.rentmybike.accessory.repository.AccessoryRepository;
import com.rentmybike.common.exception.AccessDeniedException;
import com.rentmybike.common.exception.ResourceNotFoundException;
import com.rentmybike.user.entity.User;
import com.rentmybike.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for business accessories (helmets, child seats, locks).
 * Service für Business-Zubehör (Helme, Kindersitze, Schlösser).
 *
 * <p>Stage 3 ("Business accounts") feature. Mutations are restricted to the
 * owning BUSINESS account at the controller level via {@code @PreAuthorize};
 * this service additionally checks resource ownership so one business can't
 * edit/delete another's accessories.
 * <p>Stage-3-Feature ("Business-Konten"). Änderungen sind auf der
 * Controller-Ebene über {@code @PreAuthorize} auf das besitzende
 * BUSINESS-Konto beschränkt; dieser Service prüft zusätzlich das Eigentum
 * an der Ressource, damit ein Unternehmen nicht das Zubehör eines anderen
 * bearbeiten/löschen kann.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AccessoryService {

    private final AccessoryRepository accessoryRepository;
    private final UserRepository userRepository;

    // ──────────────────────────────────────────────────────────────────────────
    // Mutations / Änderungen
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new accessory for the given owner.
     * Erstellt ein neues Zubehör für den angegebenen Eigentümer.
     */
    public AccessoryResponse createAccessory(UUID ownerId, CreateAccessoryRequest request) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", ownerId));

        Accessory accessory = Accessory.builder()
                .owner(owner)
                .type(request.getType())
                .name(request.getName())
                .quantityTotal(request.getQuantityTotal())
                .pricePerDay(request.getPricePerDay())
                .build();

        accessory = accessoryRepository.save(accessory);
        log.info("Accessory {} created by business {} / Zubehör {} von Unternehmen {} erstellt",
                accessory.getId(), ownerId, accessory.getId(), ownerId);
        return toResponse(accessory);
    }

    /**
     * Updates an existing accessory (full replace). Owner-only.
     * Aktualisiert ein vorhandenes Zubehör (vollständiger Ersatz). Nur Eigentümer.
     */
    public AccessoryResponse updateAccessory(UUID accessoryId, UUID ownerId, UpdateAccessoryRequest request) {
        Accessory accessory = loadOwned(accessoryId, ownerId);

        accessory.setType(request.getType());
        accessory.setName(request.getName());
        accessory.setQuantityTotal(request.getQuantityTotal());
        accessory.setPricePerDay(request.getPricePerDay());

        accessory = accessoryRepository.save(accessory);
        log.info("Accessory {} updated by business {} / Zubehör {} von Unternehmen {} aktualisiert",
                accessoryId, ownerId, accessoryId, ownerId);
        return toResponse(accessory);
    }

    /**
     * Soft-deletes an accessory. Owner-only.
     * Löscht ein Zubehör soft. Nur Eigentümer.
     */
    public void deleteAccessory(UUID accessoryId, UUID ownerId) {
        Accessory accessory = loadOwned(accessoryId, ownerId);
        accessory.softDelete();
        accessoryRepository.save(accessory);
        log.info("Accessory {} deleted by business {} / Zubehör {} von Unternehmen {} gelöscht",
                accessoryId, ownerId, accessoryId, ownerId);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Read / Lesen
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Lists the authenticated business's own accessories.
     * Listet die eigenen Zubehörartikel des authentifizierten Unternehmens auf.
     */
    @Transactional(readOnly = true)
    public List<AccessoryResponse> getMyAccessories(UUID ownerId) {
        return accessoryRepository.findActiveByOwnerId(ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Public listing of accessories offered by a given business — used when
     * a renter is browsing a bike and choosing add-on accessories.
     * Öffentliche Auflistung der von einem Unternehmen angebotenen
     * Zubehörartikel — wird verwendet, wenn ein Mieter ein Fahrrad ansieht
     * und Zubehör-Add-ons auswählt.
     */
    @Transactional(readOnly = true)
    public List<AccessoryResponse> getAccessoriesByOwner(UUID ownerId) {
        return accessoryRepository.findActiveByOwnerId(ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers / Private Hilfsmethoden
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Loads an active accessory and verifies the caller owns it, throwing
     * 404 if missing and 403 if owned by someone else.
     * Lädt ein aktives Zubehör und prüft, ob der Aufrufer es besitzt; wirft
     * 404, falls nicht vorhanden, und 403, falls einem anderen gehörend.
     */
    private Accessory loadOwned(UUID accessoryId, UUID ownerId) {
        Accessory accessory = accessoryRepository.findActiveById(accessoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Accessory", accessoryId));

        if (!accessory.getOwner().getId().equals(ownerId)) {
            throw new AccessDeniedException(
                    "You can only manage your own accessories / Du kannst nur dein eigenes Zubehör verwalten");
        }
        return accessory;
    }

    private AccessoryResponse toResponse(Accessory accessory) {
        return AccessoryResponse.builder()
                .id(accessory.getId())
                .ownerId(accessory.getOwner().getId())
                .ownerName(accessory.getOwner().getFullName())
                .type(accessory.getType())
                .name(accessory.getName())
                .quantityTotal(accessory.getQuantityTotal())
                .pricePerDay(accessory.getPricePerDay())
                .createdAt(accessory.getCreatedAt())
                .updatedAt(accessory.getUpdatedAt())
                .build();
    }
}
