import createMiddleware from "next-intl/middleware";
import { routing } from "./i18n/routing";

/**
 * next-intl middleware — handles locale detection and URL prefix routing.
 * next-intl Middleware — handhabt Spracherkennung und URL-Präfix-Routing.
 *
 * Runs on all routes except static files and Next.js internals.
 * Läuft auf allen Routen außer statischen Dateien und Next.js-Internas.
 */
export default createMiddleware(routing);

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
