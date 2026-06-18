package com.rentmybike.accessory.entity;

import com.rentmybike.common.entity.BaseEntity;
import com.rentmybike.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * A rentable accessory (helmet, child seat, lock) offered by a BUSINESS account.
 * Ein vermietbares Zubehör (Helm, Kindersitz, Schloss), das von einem
 * BUSINESS-Konto angeboten wird.
 *
 * <p>Stage 3 ("Business accounts") feature. One row per accessory type the
 * business stocks — {@code quantityTotal} is the unit count owned, not a
 * per-bike attachment, so the same accessory pool can be offered alongside
 * any of the owner's bikes at booking time.
 * <p>Stage-3-Feature ("Business-Konten"). Eine Zeile pro Zubehörtyp, den das
 * Unternehmen vorrätig hat — {@code quantityTotal} ist die Stückzahl im
 * Besitz, keine fahrradgebundene Zuordnung, sodass derselbe Zubehörbestand
 * bei der Buchung zu jedem Fahrrad des Eigentümers angeboten werden kann.
 *
 * <p>Maps to PostgreSQL table {@code accessories}.
 * <p>Entspricht der PostgreSQL-Tabelle {@code accessories}.
 */
@Entity
@Table(name = "accessories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Accessory extends BaseEntity {

    /**
     * The BUSINESS account that owns/stocks this accessory.
     * Das BUSINESS-Konto, dem dieses Zubehör gehört/das es vorrätig hat.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, updatable = false)
    private User owner;

    /**
     * Accessory type (HELMET / CHILD_SEAT / LOCK).
     * Zubehörtyp (HELMET / CHILD_SEAT / LOCK).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    private AccessoryType type;

    /**
     * Display name shown to renters, e.g. "Adult helmet (M/L)".
     * Anzeigename für Mieter, z. B. "Erwachsenenhelm (M/L)".
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Total units the business owns. Used to validate stock when a renter
     * selects this accessory at booking time (see Task #20).
     * Gesamtzahl der Einheiten im Besitz des Unternehmens. Wird zur
     * Bestandsprüfung verwendet, wenn ein Mieter dieses Zubehör bei der
     * Buchung auswählt (siehe Task #20).
     */
    @Column(name = "quantity_total", nullable = false)
    private int quantityTotal;

    /**
     * Rental price per day, added to the booking total per unit selected.
     * Mietpreis pro Tag, wird pro ausgewählter Einheit zur Buchungssumme addiert.
     */
    @Column(name = "price_per_day", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerDay;
}
