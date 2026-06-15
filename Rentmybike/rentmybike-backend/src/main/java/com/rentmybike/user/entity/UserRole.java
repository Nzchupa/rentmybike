package com.rentmybike.user.entity;

/**
 * Enumeration of user roles in the RentMyBike application.
 * Aufzählung der Benutzerrollen in der RentMyBike-Anwendung.
 *
 * USER  - Regular user: can list bikes and make bookings / Regulärer Benutzer: kann Fahrräder anbieten und buchen
 * ADMIN - Administrator: can moderate content and manage users / Administrator: kann Inhalte moderieren und Benutzer verwalten
 */
public enum UserRole {

    /** Standard user with renter/owner capabilities / Standardbenutzer mit Mieter-/Eigentümerfähigkeiten */
    USER,

    /** Platform administrator / Plattform-Administrator */
    ADMIN
}
