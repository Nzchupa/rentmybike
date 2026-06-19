package com.rentmybike.bike.repository;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Projection for {@link BikeRepository#findPopularBikesByOwnerId} — one row
 * per bike with its view count, booking count, and completed-booking
 * revenue, ordered by booking count then view count. Spring Data maps the
 * native query's column aliases (bikeId, title, viewCount, bookingCount,
 * revenue) onto these getters by name.
 * Projektion für {@link BikeRepository#findPopularBikesByOwnerId} — eine
 * Zeile pro Fahrrad mit Aufrufzähler, Buchungsanzahl und Umsatz aus
 * abgeschlossenen Buchungen, sortiert nach Buchungsanzahl, dann
 * Aufrufzähler. Spring Data mappt die Spaltenaliase der nativen Abfrage
 * (bikeId, title, viewCount, bookingCount, revenue) anhand des Namens auf
 * diese Getter.
 */
public interface PopularBikeProjection {
    UUID getBikeId();
    String getTitle();
    long getViewCount();
    long getBookingCount();
    BigDecimal getRevenue();
}
