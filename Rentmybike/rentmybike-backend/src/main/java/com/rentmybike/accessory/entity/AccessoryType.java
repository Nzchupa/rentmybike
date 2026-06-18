package com.rentmybike.accessory.entity;

/**
 * Kinds of rental accessories a BUSINESS account can offer alongside its bikes.
 * Arten von Verleih-Zubehör, die ein BUSINESS-Konto zusätzlich zu seinen Fahrrädern anbieten kann.
 *
 * <p>Maps to PostgreSQL VARCHAR column {@code type} created in V12 migration
 * (enum-as-VARCHAR pattern, see V5 migration — no native Postgres ENUM type).
 * <p>Entspricht der PostgreSQL-VARCHAR-Spalte {@code type}, die in der
 * V12-Migration erstellt wurde (Enum-als-VARCHAR-Muster, siehe V5-Migration —
 * kein natives Postgres-ENUM).
 */
public enum AccessoryType {

    /** Bike helmet / Fahrradhelm */
    HELMET,

    /** Child seat / Kindersitz */
    CHILD_SEAT,

    /** Bike lock / Fahrradschloss */
    LOCK
}
