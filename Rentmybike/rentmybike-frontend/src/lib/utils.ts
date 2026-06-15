import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";
import { format, differenceInCalendarDays, parseISO } from "date-fns";
import { de, enUS } from "date-fns/locale";

/**
 * Merges Tailwind class names — resolves conflicts correctly.
 * Zusammenführen von Tailwind-Klassen — löst Konflikte korrekt auf.
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/**
 * Format a price as EUR.
 * Preis als EUR formatieren.
 */
export function formatPrice(amount: number, locale = "en"): string {
  return new Intl.NumberFormat(locale === "de" ? "de-DE" : "en-US", {
    style: "currency",
    currency: "EUR",
    minimumFractionDigits: 2,
  }).format(amount);
}

/**
 * Format an ISO date string for display.
 * ISO-Datum-String zur Anzeige formatieren.
 */
export function formatDate(
  dateStr: string,
  locale: string = "en",
  fmt = "dd MMM yyyy"
): string {
  const date = parseISO(dateStr);
  return format(date, fmt, { locale: locale === "de" ? de : enUS });
}

/**
 * Calculate the number of rental days between two ISO date strings (inclusive).
 * Anzahl der Miettage zwischen zwei ISO-Datum-Strings berechnen (inklusiv).
 */
export function calcRentalDays(startDate: string, endDate: string): number {
  return differenceInCalendarDays(parseISO(endDate), parseISO(startDate)) + 1;
}

/**
 * Calculate the total booking price.
 * Gesamten Buchungspreis berechnen.
 */
export function calcTotalPrice(
  pricePerDay: number,
  startDate: string,
  endDate: string
): number {
  const days = calcRentalDays(startDate, endDate);
  return Math.round(pricePerDay * days * 100) / 100;
}

/**
 * Format a date to ISO date string (YYYY-MM-DD) for API requests.
 * Datum als ISO-Datum-String (YYYY-MM-DD) für API-Anfragen formatieren.
 */
export function toIsoDate(date: Date): string {
  return format(date, "yyyy-MM-dd");
}

/**
 * Returns the star array [1,2,3,4,5] for rendering star ratings.
 * Gibt das Stern-Array zurück für Stern-Bewertungsanzeige.
 */
export const STARS = [1, 2, 3, 4, 5] as const;

/**
 * Truncate a string to a max length with ellipsis.
 * Zeichenkette auf maximale Länge kürzen mit Ellipsis.
 */
export function truncate(str: string, maxLen: number): string {
  if (str.length <= maxLen) return str;
  return str.slice(0, maxLen - 3) + "...";
}

/**
 * Build initials from a full name (up to 2 chars).
 * Initialen aus vollem Namen erstellen (bis zu 2 Zeichen).
 */
export function getInitials(fullName: string): string {
  return fullName
    .split(" ")
    .map((n) => n[0])
    .join("")
    .toUpperCase()
    .slice(0, 2);
}
