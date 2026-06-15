package com.rentmybike.review.entity;

/**
 * Direction of a bidirectional review between renter and bike owner.
 * Richtung einer bidirektionalen Bewertung zwischen Mieter und Fahrrad-Eigentümer.
 *
 * <p>After a booking is COMPLETED, both parties may optionally leave a review:
 * <ul>
 *   <li>The renter reviews the owner experience (RENTER_TO_OWNER)</li>
 *   <li>The owner reviews the renter as a customer (OWNER_TO_RENTER)</li>
 * </ul>
 *
 * <p>Maps to PostgreSQL ENUM type {@code review_type} created in V1 migration.
 * <p>Entspricht dem PostgreSQL-ENUM-Typ {@code review_type} aus der V1-Migration.
 */
public enum ReviewType {

    /**
     * Renter reviews the bike and its owner.
     * Mieter bewertet das Fahrrad und seinen Eigentümer.
     *
     * <p>reviewer = renter, reviewee = owner
     */
    RENTER_TO_OWNER,

    /**
     * Owner reviews the renter as a customer.
     * Eigentümer bewertet den Mieter als Kunden.
     *
     * <p>reviewer = owner, reviewee = renter
     */
    OWNER_TO_RENTER
}
