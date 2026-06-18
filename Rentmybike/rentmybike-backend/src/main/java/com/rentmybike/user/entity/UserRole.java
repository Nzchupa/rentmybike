package com.rentmybike.user.entity;

/**
 * Enumeration of user roles in the RentMyBike application.
 * Aufzählung der Benutzerrollen in der RentMyBike-Anwendung.
 *
 * USER     - Regular user: can list bikes and make bookings / Regulärer Benutzer: kann Fahrräder anbieten und buchen
 * BUSINESS - Business account (bike rental shops, small companies): same capabilities as USER
 *            plus bulk bike management, accessories inventory, and a business dashboard with
 *            revenue/booking stats. May additionally be marked "verified" (see User.businessVerified).
 *            Geschäftskonto (Fahrradverleihe, kleine Unternehmen): gleiche Fähigkeiten wie USER,
 *            zusätzlich Massen-Fahrradverwaltung, Zubehör-Inventar und ein Business-Dashboard mit
 *            Umsatz-/Buchungsstatistiken. Kann zusätzlich als "verifiziert" markiert werden
 *            (siehe User.businessVerified).
 * ADMIN    - Administrator: can moderate content and manage users / Administrator: kann Inhalte moderieren und Benutzer verwalten
 */
public enum UserRole {

    /** Standard user with renter/owner capabilities / Standardbenutzer mit Mieter-/Eigentümerfähigkeiten */
    USER,

    /** Business account — bike shops / small rental companies / Geschäftskonto — Fahrradläden / kleine Vermietfirmen */
    BUSINESS,

    /** Platform administrator / Plattform-Administrator */
    ADMIN
}
