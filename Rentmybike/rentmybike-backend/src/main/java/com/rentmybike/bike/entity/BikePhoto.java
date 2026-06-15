package com.rentmybike.bike.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A single photo attached to a bike listing.
 * Ein einzelnes Foto, das einem Fahrrad-Inserat beigefügt ist.
 *
 * <p>Max 5 photos per bike — enforced in BikeService, not at DB level.
 * <p>Maximal 5 Fotos pro Fahrrad — erzwungen in BikeService, nicht auf DB-Ebene.
 *
 * <p>Does NOT extend BaseEntity — no soft delete / no audit timestamps needed for photos.
 * <p>Erweitert BaseEntity NICHT — keine Soft-Delete / keine Audit-Zeitstempel für Fotos nötig.
 *
 * <p>Maps to PostgreSQL table {@code bike_photos}.
 * <p>Entspricht der PostgreSQL-Tabelle {@code bike_photos}.
 */
@Entity
@Table(name = "bike_photos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BikePhoto {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * The bike this photo belongs to.
     * Das Fahrrad, dem dieses Foto gehört.
     *
     * <p>LAZY loaded — Bike.photos collection controls access.
     * <p>LAZY geladen — Bike.photos-Sammlung kontrolliert den Zugriff.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bike_id", nullable = false)
    private Bike bike;

    /**
     * Cloudinary HTTPS URL of the photo.
     * Cloudinary-HTTPS-URL des Fotos.
     */
    @Column(nullable = false, length = 500)
    private String url;

    /**
     * Display order — 0-based, ascending (0 = primary/cover photo).
     * Anzeigereihenfolge — 0-basiert, aufsteigend (0 = Primär-/Titelbild).
     */
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    /**
     * Whether this is the primary (cover) photo shown in search results.
     * Ob dies das primäre (Titel-) Foto ist, das in Suchergebnissen angezeigt wird.
     *
     * <p>Only one photo per bike should have isPrimary=true.
     * <p>Pro Fahrrad sollte nur ein Foto isPrimary=true haben.
     */
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private boolean primary = false;

    /**
     * Creation timestamp — set by DB default, not by Spring Auditing.
     * Erstellungszeitstempel — wird durch DB-Standard gesetzt, nicht durch Spring Auditing.
     *
     * <p>V1 schema creates this column with DEFAULT NOW().
     * <p>V1-Schema erstellt diese Spalte mit DEFAULT NOW().
     */
    @Column(name = "created_at", nullable = false, updatable = false,
            insertable = false)
    private LocalDateTime createdAt;
}
