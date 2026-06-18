package com.rentmybike.booking.entity;

import com.rentmybike.accessory.entity.Accessory;
import com.rentmybike.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * A line item recording an accessory (helmet, child seat, lock) selected
 * for a specific booking, with its price locked at booking time.
 * Eine Positionszeile, die ein für eine bestimmte Buchung ausgewähltes
 * Zubehör (Helm, Kindersitz, Schloss) erfasst, mit zum Buchungszeitpunkt
 * gesperrtem Preis.
 *
 * <p>Stage 3 ("Business accounts") feature. Mirrors {@link Booking#getTotalPrice()}'s
 * "lock price at booking time" rule — {@code pricePerDayAtBooking} is copied
 * from {@code Accessory.pricePerDay} when the booking is created so later
 * price changes by the business don't affect existing bookings.
 * <p>Stage-3-Feature ("Business-Konten"). Spiegelt die Regel "Preis zum
 * Buchungszeitpunkt sperren" von {@link Booking#getTotalPrice()} —
 * {@code pricePerDayAtBooking} wird beim Erstellen der Buchung aus
 * {@code Accessory.pricePerDay} kopiert, damit spätere Preisänderungen des
 * Unternehmens bestehende Buchungen nicht beeinflussen.
 *
 * <p>Maps to PostgreSQL table {@code booking_accessories}.
 * <p>Entspricht der PostgreSQL-Tabelle {@code booking_accessories}.
 */
@Entity
@Table(name = "booking_accessories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingAccessory extends BaseEntity {

    /**
     * The booking this accessory selection belongs to.
     * Die Buchung, zu der diese Zubehörauswahl gehört.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, updatable = false)
    private Booking booking;

    /**
     * The selected accessory.
     * Das ausgewählte Zubehör.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "accessory_id", nullable = false, updatable = false)
    private Accessory accessory;

    /**
     * Number of units of this accessory selected for the booking.
     * Anzahl der für die Buchung ausgewählten Einheiten dieses Zubehörs.
     */
    @Column(nullable = false)
    private int quantity;

    /**
     * Accessory's price-per-day at the moment the booking was created — locked
     * so later price changes don't retroactively change this booking's total.
     * Preis pro Tag des Zubehörs zum Zeitpunkt der Buchungserstellung —
     * gesperrt, damit spätere Preisänderungen die Gesamtsumme dieser Buchung
     * nicht rückwirkend ändern.
     */
    @Column(name = "price_per_day_at_booking", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerDayAtBooking;

    /**
     * Line total for this accessory selection = pricePerDayAtBooking × quantity × rentalDays.
     * Positionssumme für diese Zubehörauswahl = pricePerDayAtBooking × quantity × rentalDays.
     */
    public BigDecimal getLineTotal(long rentalDays) {
        return pricePerDayAtBooking
                .multiply(BigDecimal.valueOf(quantity))
                .multiply(BigDecimal.valueOf(rentalDays));
    }
}
