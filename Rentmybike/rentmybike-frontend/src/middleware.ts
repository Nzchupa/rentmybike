import createMiddleware from "next-intl/middleware";
import { NextRequest, NextResponse } from "next/server";
import { routing } from "./i18n/routing";

const intlMiddleware = createMiddleware(routing);

// Path segments (after the locale prefix) that require an authenticated
// session. The actual auth check still happens in the backend (@PreAuthorize)
// and the access token itself isn't verified here (that would need a JWT
// library in the Edge runtime) — this only blocks the trivial case of an
// unauthenticated browser loading the page directly. Previously this was
// enforced only by a client-side useEffect redirect, which still rendered
// (and fetched data for) the protected page for a frame, and did nothing at
// all for non-JS clients/bots/curl.
//
// Pfadsegmente (nach dem Sprachpräfix), die eine authentifizierte Sitzung
// erfordern. Die eigentliche Auth-Prüfung erfolgt weiterhin im Backend
// (@PreAuthorize), und der Zugriffstoken selbst wird hier nicht verifiziert
// (dafür wäre eine JWT-Bibliothek in der Edge-Runtime nötig) — dies blockiert
// nur den trivialen Fall, dass ein nicht authentifizierter Browser die Seite
// direkt lädt. Vorher wurde dies nur durch einen Client-seitigen useEffect-
// Redirect erzwungen, der die geschützte Seite trotzdem kurz rendert (und
// dafür Daten abruft), und bei Nicht-JS-Clients/Bots/curl gar nicht griff.
const PROTECTED_SEGMENTS = ["/dashboard", "/admin"];

/**
 * next-intl middleware — handles locale detection and URL prefix routing,
 * plus a cookie-presence guard for /dashboard and /admin routes.
 * next-intl Middleware — handhabt Spracherkennung und URL-Präfix-Routing,
 * sowie eine Cookie-Prüfung für /dashboard- und /admin-Routen.
 *
 * Runs on all routes except static files and Next.js internals.
 * Läuft auf allen Routen außer statischen Dateien und Next.js-Internas.
 */
export default function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const pathWithoutLocale = pathname.replace(/^\/[a-z]{2}(?=\/|$)/, "");

  const isProtected = PROTECTED_SEGMENTS.some(
    (segment) => pathWithoutLocale === segment || pathWithoutLocale.startsWith(`${segment}/`)
  );

  if (isProtected) {
    const hasSession =
      request.cookies.has("access_token") || request.cookies.has("refresh_token");

    if (!hasSession) {
      const localeMatch = pathname.match(/^\/([a-z]{2})(?=\/|$)/);
      const locale = localeMatch ? localeMatch[1] : routing.defaultLocale;
      const loginUrl = new URL(`/${locale}/auth/login`, request.url);
      loginUrl.searchParams.set("from", pathname);
      return NextResponse.redirect(loginUrl);
    }
  }

  return intlMiddleware(request);
}

export const config = {
  matcher: [
    // Match all pathnames except:
    // - _next/static
    // - _next/image
    // - favicon.ico
    // - api routes
    "/((?!_next|_vercel|.*\\..*).*)",
  ],
};
