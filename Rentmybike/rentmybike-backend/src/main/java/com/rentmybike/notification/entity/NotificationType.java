package com.rentmybike.notification.entity;

/**
 * Category of an in-app notification — determines icon/copy on the frontend
 * and which event triggered it.
 * Kategorie einer In-App-Benachrichtigung — bestimmt Icon/Text im Frontend
 * und welches Ereignis sie ausgelöst hat.
 *
 * <p>Only {@code NEW_BOOKING_REQUEST} is produced today (Bug 5: owners weren't
 * told when a renter requested their bike). The enum is kept open-ended so
 * later notification types (booking accepted/rejected, new review, etc.) can
 * be added without a schema change — {@code type} is stored as VARCHAR.
 * <p>Heute wird nur {@code NEW_BOOKING_REQUEST} erzeugt (Bug 5: Eigentümer
 * wurden nicht informiert, wenn ein Mieter ihr Fahrrad anfragte). Das Enum ist
 * bewusst offen gehalten, damit spätere Benachrichtigungstypen (Buchung
 * akzeptiert/abgelehnt, neue Bewertung, etc.) ohne Schemaänderung hinzugefügt
 * werden können — {@code type} wird als VARCHAR gespeichert.
 */
public enum NotificationType {
    /** A renter submitted a new booking request for one of the owner's bikes. */
    NEW_BOOKING_REQUEST
}
