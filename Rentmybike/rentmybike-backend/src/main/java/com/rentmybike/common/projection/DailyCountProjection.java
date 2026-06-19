package com.rentmybike.common.projection;

import java.time.LocalDate;

/**
 * Generic one-row-per-day count projection — used by the admin analytics
 * time-series queries (new signups, new bike listings, new bookings) so the
 * grouping/aggregation happens in the database instead of pulling every row
 * into Java and bucketing it there.
 * Generische Eine-Zeile-pro-Tag-Zählprojektion — wird von den
 * Admin-Analyse-Zeitreihen-Abfragen (neue Anmeldungen, neue
 * Fahrrad-Inserate, neue Buchungen) verwendet, damit die Gruppierung/
 * Aggregation in der Datenbank stattfindet, statt jede Zeile nach Java zu
 * laden und dort zu bündeln.
 *
 * <p>Spring Data maps native-query result columns to this interface by
 * name ({@code day}, {@code count}) — see the {@code @Query(nativeQuery =
 * true)} usages in {@code UserRepository}, {@code BikeRepository}, and
 * {@code BookingRepository}.
 * <p>Spring Data ordnet die Spalten des nativen Abfrageergebnisses anhand
 * des Namens ({@code day}, {@code count}) dieser Schnittstelle zu — siehe
 * die {@code @Query(nativeQuery = true)}-Verwendungen in {@code
 * UserRepository}, {@code BikeRepository} und {@code BookingRepository}.
 */
public interface DailyCountProjection {
    LocalDate getDay();
    long getCount();
}
