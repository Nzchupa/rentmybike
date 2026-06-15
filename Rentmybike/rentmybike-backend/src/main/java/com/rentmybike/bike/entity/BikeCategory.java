package com.rentmybike.bike.entity;

/**
 * Categories of bikes available for rent.
 * Kategorien der zum Verleih verfügbaren Fahrräder.
 *
 * <p>Maps to PostgreSQL ENUM type {@code bike_category} created in V1 migration.
 * <p>Entspricht dem PostgreSQL-ENUM-Typ {@code bike_category}, der in der V1-Migration erstellt wurde.
 */
public enum BikeCategory {

    /** City / commuter bike — flat terrain, upright position / Stadtrad — flaches Gelände, aufrechte Haltung */
    CITY,

    /** Mountain bike — off-road, suspension / Mountainbike — Geländefahrt, Federung */
    MOUNTAIN,

    /** Road / racing bike — lightweight, drop bars / Rennrad — leicht, Tropflenker */
    ROAD,

    /** Electric-assist bike (e-bike) / Elektro-Fahrrad (E-Bike) */
    ELECTRIC,

    /** Hybrid — mix of city and road / Hybrid — Mischung aus Stadt- und Rennrad */
    HYBRID,

    /** Cargo bike — large payload, family / Lastenrad — große Nutzlast, Familie */
    CARGO,

    /** Kids bike / Kinderfahrrad */
    KIDS
}
