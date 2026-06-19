package com.rentmybike.common.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Generic one-row-per-day monetary sum projection — used by the admin
 * analytics revenue time-series query. Kept separate from {@link
 * DailyCountProjection} rather than reusing a single generic numeric
 * interface, since revenue needs {@link BigDecimal} precision while plain
 * counts are fine as {@code long}.
 * Generische Eine-Zeile-pro-Tag-Geldsummen-Projektion — wird von der
 * Admin-Analyse-Umsatz-Zeitreihen-Abfrage verwendet. Bewusst getrennt von
 * {@link DailyCountProjection} gehalten, statt eine einzelne generische
 * numerische Schnittstelle wiederzuverwenden, da Umsatz {@link BigDecimal}
 * -Präzision benötigt, während einfache Zählungen als {@code long} genügen.
 */
public interface DailyAmountProjection {
    LocalDate getDay();
    BigDecimal getAmount();
}
