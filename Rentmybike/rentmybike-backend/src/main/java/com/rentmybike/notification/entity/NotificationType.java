package com.rentmybike.notification.entity;

/**
 * Category of an in-app notification — determines icon/copy on the frontend
 * and which event triggered it.
 * Kategorie einer In-App-Benachrichtigung — bestimmt Icon/Text im Frontend
 * und welches Ereignis sie ausgelöst hat.
 *
 * <p>{@code NEW_BOOKING_REQUEST} (Bug 5: owners weren't told when a renter
 * requested their bike) and {@code NEW_CHAT_MESSAGE} (renter/owner weren't
 * told when the other side sent a chat message) are produced today. The enum
 * is kept open-ended so later notification types (booking accepted/rejected,
 * new review, etc.) can be added without a schema change — {@code type} is
 * stored as VARCHAR.
 * <p>{@code NEW_BOOKING_REQUEST} (Bug 5: Eigentümer wurden nicht informiert,
 * wenn ein Mieter ihr Fahrrad anfragte) und {@code NEW_CHAT_MESSAGE} (Mieter/
 * Eigentümer wurden nicht informiert, wenn die Gegenseite eine Chatnachricht
 * sendete) werden heute erzeugt. Das Enum ist bewusst offen gehalten, damit
 * spätere Benachrichtigungstypen (Buchung akzeptiert/abgelehnt, neue
 * Bewertung, etc.) ohne Schemaänderung hinzugefügt werden können — {@code
 * type} wird als VARCHAR gespeichert.
 */
public enum NotificationType {
    /** A renter submitted a new booking request for one of the owner's bikes. */
    NEW_BOOKING_REQUEST,
    /** The other participant in a booking's chat thread sent a new message. */
    NEW_CHAT_MESSAGE,
    /** Admin-facing: a new bike listing is awaiting approval. */
    ADMIN_NEW_PENDING_BIKE,
    /** Admin-facing: a new report was filed and needs triage. */
    ADMIN_NEW_REPORT,
    /** Admin-facing: a new support ticket was filed and needs a reply. */
    ADMIN_NEW_SUPPORT_TICKET,
    /** User-facing: an admin replied to one of the user's support tickets. */
    SUPPORT_TICKET_REPLY
}
