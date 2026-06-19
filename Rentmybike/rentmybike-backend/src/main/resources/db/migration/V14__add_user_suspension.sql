-- Adds the suspended_at column to users — a lighter, distinct alternative to
-- banned_at that admins can use to temporarily restrict an account. Like
-- banned_at, a non-null value blocks login (enforced via
-- User.isAccountNonLocked() in the Java entity).
-- Fügt die Spalte suspended_at zur users-Tabelle hinzu — eine leichtere,
-- von banned_at getrennte Alternative, mit der Admins ein Konto temporär
-- einschränken können. Wie banned_at blockiert ein Nicht-NULL-Wert die
-- Anmeldung (durchgesetzt über User.isAccountNonLocked() in der Java-Entity).

ALTER TABLE users
    ADD COLUMN suspended_at TIMESTAMP;  -- NULL = not suspended / NULL = nicht suspendiert
