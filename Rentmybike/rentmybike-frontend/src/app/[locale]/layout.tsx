import type { Metadata } from "next";
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

export const metadata: Metadata = {
  title: {
    default: "RentMyBike",
    template: "%s | RentMyBike",
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
    <html lang={locale} suppressHydrationWarning>
      <body className="flex min-h-screen flex-col">
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
