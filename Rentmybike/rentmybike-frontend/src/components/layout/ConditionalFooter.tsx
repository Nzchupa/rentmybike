"use client";

import { usePathname } from "next/navigation";
import { Footer } from "@/components/layout/Footer";

// Routes (after the /{locale} prefix) that already provide their own
// navigation (sidebar/top nav + mobile tab bar) and don't need the public
// marketing footer underneath — it just adds scroll on small screens.
// Routen (nach dem /{locale}-Präfix), die bereits eine eigene Navigation
// haben und den öffentlichen Footer nicht brauchen.
const FOOTER_HIDDEN_PREFIXES = ["/dashboard", "/admin"];

/**
 * Renders the Footer everywhere except authenticated dashboard/admin pages.
 * Rendert den Footer überall außer auf authentifizierten Dashboard-/Admin-Seiten.
 */
export function ConditionalFooter() {
  const pathname = usePathname();

  // pathname includes the locale segment, e.g. "/en/dashboard/bikes"
  const pathWithoutLocale = pathname.replace(/^\/[a-z]{2}(?=\/|$)/, "");

  const hideFooter = FOOTER_HIDDEN_PREFIXES.some((prefix) =>
    pathWithoutLocale.startsWith(prefix)
  );

  if (hideFooter) return null;

  return <Footer />;
}
