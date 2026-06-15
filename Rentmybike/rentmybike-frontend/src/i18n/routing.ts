import { defineRouting } from "next-intl/routing";

/**
 * Supported locales and default locale for next-intl.
 * Unterstützte Sprachen und Standardsprache für next-intl.
 *
 * Routes:
 *   /en/bikes   — English
 *   /de/bikes   — German (Deutsch)
 *   /bikes      — redirects to /en/bikes (default)
 */
export const routing = defineRouting({
  locales: ["en", "de"],
  defaultLocale: "en",
  localePrefix: "always",
});

export type Locale = (typeof routing.locales)[number];
