"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import { usePathname, useRouter } from "next/navigation";
import {
  LayoutDashboard,
  Bike,
  CalendarSearch,
  CalendarCheck,
  Bell,
  Heart,
  User,
  Briefcase,
  Menu,
  X,
} from "lucide-react";
import { useAuthStore } from "@/store/auth.store";
import { cn } from "@/lib/utils";

const baseTabs = [
  { key: "overview",      icon: LayoutDashboard, href: "/dashboard" },
  { key: "myBikes",       icon: Bike,            href: "/dashboard/bikes" },
  { key: "asRenter",      icon: CalendarSearch,  href: "/dashboard/bookings/renter" },
  { key: "asOwner",       icon: CalendarCheck,   href: "/dashboard/bookings/owner" },
  { key: "favorites",     icon: Heart,           href: "/dashboard/favorites" },
  { key: "notifications", icon: Bell,            href: "/dashboard/notifications" },
  { key: "profile",       icon: User,            href: "/dashboard/profile" },
];

// Shown only for BUSINESS-role users — links to the Business Dashboard
// (stats, bike management, calendar, accessories). Nur für BUSINESS-Rolle —
// verlinkt zum Business-Dashboard.
const businessTab = { key: "business", icon: Briefcase, href: "/dashboard/business" };

// The mobile tab bar used to cram all 7-8 tabs into one row with labels
// truncated to 6 characters (e.g. "Notifi", "Favori") — unreadable and
// cramped touch targets. Now we show only the 4 most-used tabs at full
// width plus a "More" button that opens a sheet with the rest.
// Die mobile Tab-Bar quetschte früher alle 7-8 Tabs in eine Zeile mit auf
// 6 Zeichen abgeschnittenen Labels — unleserlich und enge Touch-Ziele.
// Jetzt zeigen wir nur die 4 meistgenutzten Tabs in voller Breite plus
// einen "Mehr"-Button, der die restlichen in einem Sheet öffnet.
const MOBILE_PRIMARY_KEYS = ["overview", "asRenter", "myBikes", "notifications"];

/**
 * Shared dashboard layout — sidebar/tabs + auth guard.
 * Gemeinsames Dashboard-Layout — Seitenleiste/Tabs + Auth-Guard.
 */
export function DashboardLayout({ children }: { children: React.ReactNode }) {
  const t = useTranslations("dashboard.tabs");
  const locale = useLocale();
  const pathname = usePathname();
  const router = useRouter();
  const { user, isLoading, isBusiness } = useAuthStore();
  const tabs = isBusiness() ? [...baseTabs, businessTab] : baseTabs;
  const [showMore, setShowMore] = useState(false);

  const mobilePrimaryTabs = tabs.filter((tab) => MOBILE_PRIMARY_KEYS.includes(tab.key));
  const mobileMoreTabs = tabs.filter((tab) => !MOBILE_PRIMARY_KEYS.includes(tab.key));

  // Auth guard
  useEffect(() => {
    if (!isLoading && !user) {
      router.replace(`/${locale}/auth/login`);
    }
  }, [user, isLoading, locale, router]);

  // Close the "More" sheet whenever the route changes (e.g. user tapped a
  // link inside it) so it doesn't stay open on the next page.
  // Schließt das "Mehr"-Sheet bei jedem Routenwechsel.
  useEffect(() => {
    setShowMore(false);
  }, [pathname]);

  if (isLoading || !user) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="w-8 h-8 border-4 border-brand-500 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex gap-8">
        {/* Sidebar */}
        <aside className="hidden md:flex flex-col w-52 shrink-0 gap-1">
          {tabs.map(({ key, icon: Icon, href }) => {
            const fullHref = `/${locale}${href}`;
            const active =
              href === "/dashboard"
                ? pathname === fullHref
                : pathname.startsWith(fullHref);

            return (
              <Link
                key={key}
                href={fullHref}
                className={cn(
                  "flex items-center gap-3 px-4 py-2.5 rounded-xl text-sm font-medium transition-colors",
                  active
                    ? "bg-brand-50 text-brand-700"
                    : "text-slate-600 hover:bg-slate-50 hover:text-slate-900"
                )}
              >
                <Icon size={18} />
                {t(key)}
              </Link>
            );
          })}
        </aside>

        {/* Mobile tab bar — 4 primary tabs at full width + "More" sheet for the rest */}
        <div className="md:hidden fixed bottom-0 inset-x-0 bg-white border-t border-slate-200 flex z-40">
          {mobilePrimaryTabs.map(({ key, icon: Icon, href }) => {
            const fullHref = `/${locale}${href}`;
            const active = pathname.startsWith(fullHref);
            return (
              <Link
                key={key}
                href={fullHref}
                className={cn(
                  "flex-1 flex flex-col items-center gap-0.5 py-2 text-[11px] leading-tight",
                  active ? "text-brand-600" : "text-slate-500"
                )}
              >
                <Icon size={20} />
                <span className="truncate max-w-full px-0.5">{t(key)}</span>
              </Link>
            );
          })}
          <button
            type="button"
            onClick={() => setShowMore(true)}
            className={cn(
              "flex-1 flex flex-col items-center gap-0.5 py-2 text-[11px] leading-tight",
              mobileMoreTabs.some(({ href }) => pathname.startsWith(`/${locale}${href}`))
                ? "text-brand-600"
                : "text-slate-500"
            )}
          >
            <Menu size={20} />
            <span>{t("more")}</span>
          </button>
        </div>

        {/* "More" sheet — slides up from the bottom, lists the remaining
            tabs with full labels and icons instead of squeezing them into
            the bar itself. */}
        {showMore && (
          <div className="md:hidden fixed inset-0 z-50 flex flex-col justify-end">
            <button
              type="button"
              aria-label={t("closeMore")}
              onClick={() => setShowMore(false)}
              className="absolute inset-0 bg-black/30"
            />
            <div className="relative bg-white rounded-t-2xl pb-[max(1rem,env(safe-area-inset-bottom))] shadow-xl">
              <div className="flex items-center justify-between px-5 pt-4 pb-2">
                <span className="text-sm font-semibold text-slate-900">{t("more")}</span>
                <button
                  type="button"
                  onClick={() => setShowMore(false)}
                  className="p-1 text-slate-400 hover:text-slate-700"
                  aria-label={t("closeMore")}
                >
                  <X size={18} />
                </button>
              </div>
              <div className="grid grid-cols-2 gap-1 px-3 pb-2">
                {mobileMoreTabs.map(({ key, icon: Icon, href }) => {
                  const fullHref = `/${locale}${href}`;
                  const active = pathname.startsWith(fullHref);
                  return (
                    <Link
                      key={key}
                      href={fullHref}
                      className={cn(
                        "flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium",
                        active
                          ? "bg-brand-50 text-brand-700"
                          : "text-slate-600 hover:bg-slate-50"
                      )}
                    >
                      <Icon size={18} />
                      {t(key)}
                    </Link>
                  );
                })}
              </div>
            </div>
          </div>
        )}

        {/* Content */}
        <div className="flex-1 min-w-0 pb-20 md:pb-0">{children}</div>
      </div>
    </div>
  );
}
