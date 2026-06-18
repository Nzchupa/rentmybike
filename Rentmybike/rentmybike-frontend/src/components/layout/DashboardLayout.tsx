"use client";

import { useEffect } from "react";
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
} from "lucide-react";
import { useAuthStore } from "@/store/auth.store";
import { cn } from "@/lib/utils";

const tabs = [
  { key: "overview",      icon: LayoutDashboard, href: "/dashboard" },
  { key: "myBikes",       icon: Bike,            href: "/dashboard/bikes" },
  { key: "asRenter",      icon: CalendarSearch,  href: "/dashboard/bookings/renter" },
  { key: "asOwner",       icon: CalendarCheck,   href: "/dashboard/bookings/owner" },
  { key: "favorites",     icon: Heart,           href: "/dashboard/favorites" },
  { key: "notifications", icon: Bell,            href: "/dashboard/notifications" },
  { key: "profile",       icon: User,            href: "/dashboard/profile" },
];

/**
 * Shared dashboard layout — sidebar/tabs + auth guard.
 * Gemeinsames Dashboard-Layout — Seitenleiste/Tabs + Auth-Guard.
 */
export function DashboardLayout({ children }: { children: React.ReactNode }) {
  const t = useTranslations("dashboard.tabs");
  const locale = useLocale();
  const pathname = usePathname();
  const router = useRouter();
  const { user, isLoading } = useAuthStore();

  // Auth guard
  useEffect(() => {
    if (!isLoading && !user) {
      router.replace(`/${locale}/auth/login`);
    }
  }, [user, isLoading, locale, router]);

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

        {/* Mobile tab bar */}
        <div className="md:hidden fixed bottom-0 inset-x-0 bg-white border-t border-slate-200 flex z-40">
          {tabs.map(({ key, icon: Icon, href }) => {
            const fullHref = `/${locale}${href}`;
            const active = pathname.startsWith(fullHref);
            return (
              <Link
                key={key}
                href={fullHref}
                className={cn(
                  "flex-1 flex flex-col items-center gap-0.5 py-2 text-xs",
                  active ? "text-brand-600" : "text-slate-500"
                )}
              >
                <Icon size={20} />
                <span>{t(key).slice(0, 6)}</span>
              </Link>
            );
          })}
        </div>

        {/* Content */}
        <div className="flex-1 min-w-0 pb-20 md:pb-0">{children}</div>
      </div>
    </div>
  );
}
