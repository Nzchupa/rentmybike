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

/**
 * Inserts Cloudinary's automatic-format/quality delivery transformation
 * (`f_auto,q_auto`, optionally capped to `maxWidth`) into a Cloudinary
 * upload URL.
 *
 * Every photo/avatar is stored via CloudinaryService.uploadImage(), which
 * passes `quality`/`fetch_format` as upload-time params — but those only
 * affect Cloudinary's *incoming* processing, not what the stored
 * `secure_url` actually points at. The URL saved to the DB (and returned by
 * the API) is the plain, unoptimized original, so every page was serving
 * full-size JPEGs/PNGs with no format negotiation. This wraps that URL at
 * render time instead of changing what's stored — safer than baking the
 * transformation into the stored URL, since CloudinaryService.extractPublicId()
 * (used by deleteImage()) parses the segment right after `/upload/` assuming
 * it's the version prefix, and would silently extract the wrong public_id
 * if a transformation segment were inserted there instead.
 *
 * Fügt Cloudinarys automatische Format-/Qualitäts-Übertragungstransformation
 * (`f_auto,q_auto`, optional auf `maxWidth` begrenzt) in eine
 * Cloudinary-Upload-URL ein.
 *
 * Jedes Foto/Avatar wird über CloudinaryService.uploadImage() gespeichert,
 * das `quality`/`fetch_format` als Upload-Zeit-Parameter übergibt — diese
 * wirken sich aber nur auf Cloudinarys *eingehende* Verarbeitung aus, nicht
 * darauf, worauf die gespeicherte `secure_url` tatsächlich zeigt. Die in der
 * DB gespeicherte (und von der API zurückgegebene) URL ist das reine,
 * unoptimierte Original, sodass jede Seite vollformatige JPEGs/PNGs ohne
 * Formatverhandlung auslieferte. Dies umschließt die URL zur Renderzeit,
 * statt das Gespeicherte zu ändern — sicherer, als die Transformation in die
 * gespeicherte URL einzubacken, da CloudinaryService.extractPublicId()
 * (verwendet von deleteImage()) das Segment direkt nach `/upload/` als
 * Versionspräfix interpretiert und stillschweigend die falsche public_id
 * extrahieren würde, wenn dort stattdessen ein Transformationssegment stünde.
 *
 * @param url      a Cloudinary secure_url — returned unchanged if it doesn't
 *                 contain "/upload/" (e.g. not a Cloudinary URL) / eine
 *                 Cloudinary-secure_url — unverändert zurückgegeben, wenn sie
 *                 kein "/upload/" enthält
 * @param maxWidth optional delivered-width cap (never upscales — `c_limit`)
 *                 / optionale Obergrenze für die gelieferte Breite (skaliert
 *                 nie hoch — `c_limit`)
 */
export function optimizedImageUrl(url: string, maxWidth?: number): string {
  const marker = "/upload/";
  const idx = url.indexOf(marker);
  if (idx === -1) return url;

  const transform = maxWidth ? `f_auto,q_auto,w_${maxWidth},c_limit` : "f_auto,q_auto";
  return url.slice(0, idx + marker.length) + transform + "/" + url.slice(idx + marker.length);
}
