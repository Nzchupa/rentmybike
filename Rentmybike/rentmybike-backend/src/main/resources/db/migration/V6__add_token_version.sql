-- Add server-side refresh-token revocation support.
-- Serverseitige Refresh-Token-Sperrung hinzufügen.
--
-- Previously logout() only cleared cookies on the client; a refresh token
-- issued before logout (or before a password change) remained valid for its
-- full 7-day lifetime and could still be used to mint new access tokens if
-- it had leaked or been copied elsewhere. token_version is bumped on logout
-- and embedded as a claim in every issued token; refresh requests are
-- rejected once the token's version no longer matches the user's current one.
--
-- Vorher hat logout() nur die Cookies auf dem Client gelöscht; ein vor dem
-- Logout (oder einer Passwortänderung) ausgestellter Refresh-Token blieb für
-- seine volle 7-Tage-Lebensdauer gültig und konnte weiterhin zum Ausstellen
-- neuer Zugriffstoken verwendet werden, falls er geleakt wurde. token_version
-- wird beim Logout erhöht und als Claim in jedem ausgestellten Token
-- eingebettet; Refresh-Anfragen werden abgelehnt, sobald die Token-Version
-- nicht mehr mit der aktuellen Version des Benutzers übereinstimmt.

ALTER TABLE users ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;
