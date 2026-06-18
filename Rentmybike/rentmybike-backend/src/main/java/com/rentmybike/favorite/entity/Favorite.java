package com.rentmybike.favorite.entity;

import com.rentmybike.bike.entity.Bike;
import com.rentmybike.common.entity.BaseEntity;
import com.rentmybike.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * A user's bookmark of a bike they're interested in.
 * Das Lesezeichen eines Benutzers für ein Fahrrad, das ihn interessiert.
 *
 * <p>Stage 2 ("Beta launch") trust feature. One row per (user, bike) pair,
 * enforced unique at the DB level via {@code uq_favorites_user_bike} —
 * favoriting the same bike twice is a no-op, not a duplicate row.
 * <p>Stage-2-Feature ("Beta-Start") für Vertrauen. Eine Zeile pro
 * (Benutzer, Fahrrad)-Paar, auf DB-Ebene eindeutig erzwungen über
 * {@code uq_favorites_user_bike} — das zweimalige Favorisieren desselben
 * Fahrrads ist ein No-op, keine doppelte Zeile.
 *
 * <p>Maps to PostgreSQL table {@code favorites}.
 * <p>Entspricht der PostgreSQL-Tabelle {@code favorites}.
 */
@Entity
@Table(name = "favorites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Favorite extends BaseEntity {

    /**
     * The user who favorited the bike.
     * Der Benutzer, der das Fahrrad favorisiert hat.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    /**
     * The favorited bike.
     * Das favorisierte Fahrrad.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bike_id", nullable = false, updatable = false)
    private Bike bike;
}
