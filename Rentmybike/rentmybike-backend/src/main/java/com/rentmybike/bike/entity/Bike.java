package com.rentmybike.bike.entity;

import com.rentmybike.common.entity.BaseEntity;
import com.rentmybike.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Bike listing entity — the core domain object of the RentMyBike marketplace.
 * Fahrrad-Inserat-Entity — das zentrale Domänenobjekt des RentMyBike-Marktplatzes.
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>Owner creates a bike → status PENDING, not visible publicly.</li>
 *   <li>Admin approves → status APPROVED, appears in public search.</li>
 *   <li>Admin rejects with reason → status REJECTED, owner can re-submit.</li>
 *   <li>Owner can soft-delete at any time (sets deleted_at).</li>
 * </ol>
 *
 * <p>Maps to PostgreSQL table {@code bikes}.
 * <p>Entspricht der PostgreSQL-Tabelle {@code bikes}.
 */
@Entity
@Table(name = "bikes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bike extends BaseEntity {

    // ──────────────────────────────────────────────────────────────────────────
    // Ownership / Eigentümerschaft
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * The user who listed this bike.
     * Der Benutzer, der dieses Fahrrad inseriert hat.
     *
     * <p>LAZY loaded — use explicit join fetch in queries that need owner data.
     * <p>LAZY geladen — expliziten Join-Fetch in Abfragen verwenden, die Eigentümerdaten benötigen.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    // ──────────────────────────────────────────────────────────────────────────
    // Listing details / Inserat-Details
    // ──────────────────────────────────────────────────────────────────────────

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * Bike category — stored as VARCHAR(50) since V5 (Hibernate 6 cannot write
     * native Postgres ENUM types via JDBC). Must stay in sync with all values
     * of {@link BikeCategory} — adding a new enum constant here requires no
     * migration since the column is a plain VARCHAR.
     * Fahrradkategorie — seit V5 als VARCHAR(50) gespeichert (Hibernate 6 kann
     * native Postgres-ENUM-Typen nicht per JDBC schreiben). Muss mit allen
     * Werten von {@link BikeCategory} übereinstimmen.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50)")
    private BikeCategory category;

    /**
     * Price per day in EUR (2 decimal places).
     * Preis pro Tag in EUR (2 Dezimalstellen).
     */
    @Column(name = "price_per_day", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerDay;

    // ──────────────────────────────────────────────────────────────────────────
    // Location / Standort
    // ──────────────────────────────────────────────────────────────────────────

    @Column(nullable = false, length = 100)
    private String city;

    /** Full address — shown only to confirmed renters / Vollständige Adresse — nur für bestätigte Mieter sichtbar */
    @Column(length = 255)
    private String address;

    /** GPS latitude — optional / GPS-Breitengrad — optional */
    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    /** GPS longitude — optional / GPS-Längengrad — optional */
    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    // ──────────────────────────────────────────────────────────────────────────
    // Availability & approval / Verfügbarkeit & Genehmigung
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Whether the owner has marked this bike as available for booking.
     * Ob der Eigentümer dieses Fahrrad als buchbar markiert hat.
     *
     * <p>Even APPROVED bikes can be temporarily unavailable (e.g. maintenance).
     * <p>Auch APPROVED-Fahrräder können vorübergehend nicht verfügbar sein.
     */
    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private boolean available = true;

    /**
     * Admin approval status — only APPROVED bikes appear in public search.
     * Admin-Genehmigungsstatus — nur APPROVED-Fahrräder erscheinen in öffentlicher Suche.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, columnDefinition = "VARCHAR(20)")
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    /** Reason provided by admin when rejecting a bike / Ablehnungsgrund des Admins */
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    /**
     * Number of times this bike's public detail page has been viewed.
     * Not deduplicated per visitor — see V17 migration comment for why.
     * Anzahl der Aufrufe der öffentlichen Detailseite dieses Fahrrads. Nicht
     * pro Besucher dedupliziert — siehe Kommentar in der V17-Migration.
     */
    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private long viewCount = 0;

    // ──────────────────────────────────────────────────────────────────────────
    // Photos / Fotos
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Ordered list of photos — max 5 enforced in BikeService.
     * Geordnete Liste von Fotos — max. 5, erzwungen in BikeService.
     *
     * <p>CascadeType.ALL + orphanRemoval: deleting a photo entity also removes the DB row.
     * <p>CascadeType.ALL + orphanRemoval: Löschen einer Foto-Entity entfernt auch die DB-Zeile.
     */
    @OneToMany(mappedBy = "bike", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<BikePhoto> photos = new ArrayList<>();

    // ──────────────────────────────────────────────────────────────────────────
    // Helper / Hilfsmethoden
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns true if this bike is visible in public search results.
     * Gibt true zurück, wenn dieses Fahrrad in öffentlichen Suchergebnissen sichtbar ist.
     *
     * <p>Conditions: APPROVED + available + not deleted.
     * <p>Bedingungen: APPROVED + verfügbar + nicht gelöscht.
     */
    public boolean isPubliclyVisible() {
        return ApprovalStatus.APPROVED == approvalStatus
                && available
                && !isDeleted();
    }
}
