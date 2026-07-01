import type { Metadata } from "next";
import { Inter } from "next/font/google";
import { NextIntlClientProvider } from "next-intl";
import { getMessages } from "next-intl/server";
import { notFound } from "next/navigation";
import { Analytics } from "@vercel/analytics/next";
import { SpeedInsights } from "@vercel/speed-insights/next";
import { routing } from "@/i18n/routing";
import { Providers } from "@/components/providers/Providers";
import { Navbar } from "@/components/layout/Navbar";
import { ConditionalFooter } from "@/components/layout/ConditionalFooter";
import "@/app/globals.css";

// Self-hosted via next/font instead of the old `@import url(fonts.googleapis.com/...)`
// in globals.css — that CSS @import forced the browser to discover and fetch
// the font on a separate round trip *after* the stylesheet itself loaded
// (render-blocking), and pulled from Google's CDN on every visit. next/font
// downloads the font at build time, self-hosts it from this app's own
// origin, and inlines the @font-face — no extra external request, no
// layout shift while the fallback-to-webfont swap happens (Next also
// auto-adds `size-adjust` for that). The `variable` option exposes it as
// `--font-inter` so tailwind.config's `fontFamily.sans` can reference it.
//
// Selbst gehostet via next/font statt des alten `@import
// url(fonts.googleapis.com/...)` in globals.css — dieser CSS-@import zwang
// den Browser, die Schriftart in einem separaten Roundtrip *nach* dem Laden
// des Stylesheets selbst zu entdecken und zu laden (render-blockierend), und
// lud sie bei jedem Besuch vom CDN von Google. next/font lädt die
// Schriftart zur Build-Zeit herunter, hostet sie selbst von der eigenen
// Origin dieser App und bindet die @font-face inline ein — keine externe
// Anfrage, kein Layout-Sprung beim Wechsel von Fallback zu Webfont (Next
// fügt dafür automatisch `size-adjust` hinzu). Die `variable`-Option macht
// sie als `--font-inter` verfügbar, damit `fontFamily.sans` in
// tailwind.config darauf verweisen kann.
const inter = Inter({
  subsets: ["latin"],
  variable: "--font-inter",
  display: "swap",
});

export const metadata: Metadata = {
  title: {
    default: "Velohood",
    template: "%s | Velohood",
  },
  description:
    "Peer-to-peer bike rental marketplace. Rent bikes from locals in your city.",
};

interface RootLayoutProps {
  children: React.ReactNode;
  params: Promise<{ locale: string }>;
}

/**
 * Root layout for all [locale] routes.
 * Root-Layout für alle [locale]-Routen.
 *
 * Provides: next-intl messages, React Query, Toast, Navbar, Footer.
 */
export default async function RootLayout({ children, params }: RootLayoutProps) {
  const { locale } = await params;

  // Validate locale — 404 on unknown locale
  if (!routing.locales.includes(locale as "en" | "de")) {
    notFound();
  }

  const messages = await getMessages();

  return (
    <html lang={locale} suppressHydrationWarning className={inter.variable}>
      <body className={`${inter.className} flex min-h-screen flex-col`}>
        <NextIntlClientProvider messages={messages}>
          <Providers>
            <Navbar />
            <main className="flex-1">{children}</main>
            <ConditionalFooter />
          </Providers>
        </NextIntlClientProvider>
        <Analytics />
        <SpeedInsights />
      </body>
    </html>
  );
}

// Generate static paths for supported locales
export function generateStaticParams() {
  return routing.locales.map((locale) => ({ locale }));
}
