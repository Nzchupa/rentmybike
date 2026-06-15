import { getRequestConfig } from "next-intl/server";
import { routing } from "./routing";

/**
 * Loads the message bundle for the current locale on every request.
 * Lädt das Nachrichten-Bundle für die aktuelle Sprache bei jeder Anfrage.
 */
export default getRequestConfig(async ({ requestLocale }) => {
  let locale = await requestLocale;

  // Fall back to default if locale is invalid
  // Auf Standard zurückfallen wenn Sprache ungültig
  if (!locale || !routing.locales.includes(locale as "en" | "de")) {
    locale = routing.defaultLocale;
  }

  return {
    locale,
    messages: (await import(`../../messages/${locale}.json`)).default,
  };
});
