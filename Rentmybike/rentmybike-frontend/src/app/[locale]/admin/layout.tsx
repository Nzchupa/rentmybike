"use client";

import { useEffect } from "react";
import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import { usePathname, useRouter } from "next/navigation";
import { BarChart3, Users, Bike, ScrollText, ShieldAlert, Bell, Flag, BadgeCheck, LifeBuoy } from "lucide-react";
import { useAuthStore } from "@/store/auth.store";
import { cn } from "@/lib/utils";

const tabs = [
  { key: "stats",      labelKey: "stats",      icon: BarChart3,   href: "/admin" },
  { key: "users",      labelKey: "users",      icon: Users,       href: "/admin/users" },
  { key: "bikes",      labelKey: "bikes",      icon: Bike,        href: "/admin/bikes" },
  { key: "moderation", labelKey: "moderation", icon: ShieldAlert, href: "/admin/moderation" },
  { key: "businessVerification", labelKey: "businessVerification", icon: BadgeCheck, href: "/admin/business-verification" },
  { key: "reports",    labelKey: "reports",    icon: Flag,        href: "/admin/reports" },
  { key: "support",    labelKey: "support",    icon: LifeBuoy,    href: "/admin/support" },
  { key: "auditLog",   labelKey: "auditLog",   icon: ScrollText,  href: "/admin/audit-log" },
  { key: "notifications", labelKey: "notifications", icon: Bell, href: "/admin/notifications" },
];

/**
 * Admin panel layout — admin guard + sidebar.
 * Admin-Panel-Layout — Admin-Guard + Seitenleiste.
 */
export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const t = useTranslations("admin");
  const tNav = useTranslations("admin.nav");
  const locale = useLocale();
  const pathname = usePathname();
  const router = useRouter();
  const { user, isLoading } = useAuthStore();

  useEffect(() => {
    if (!isLoading) {
      if (!user) router.replace(`/${locale}/auth/login`);
      else if (user.role !== "ADMIN") router.replace(`/${locale}/dashboard`);
    }
  }, [user, isLoading, locale, router]);

  if (isLoading || !user || user.role !== "ADMIN") {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="w-8 h-8 border-4 border-brand-500 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-8">
      {/* Mobile tab strip — the desktop sidebar below is hidden under `md`,
          and without this there was no way to move between admin sections
          on a phone at all (no bottom-nav equivalent exists for /admin).
          Horizontally scrollable so it degrades gracefully as more tabs
          get added. */}
      {/* Mobile-Tableiste — die Desktop-Seitenleiste ist unter `md` verborgen,
          ohne dies gab es auf dem Handy keine Möglichkeit, zwischen den
          Admin-Bereichen zu wechseln. Horizontal scrollbar. */}
      <nav className="md:hidden -mx-4 sm:-mx-6 mb-6 px-4 sm:px-6 flex gap-1.5 overflow-x-auto pb-2 [&::-webkit-scrollbar]:hidden">
        {tabs.map(({ key, labelKey, icon: Icon, href }) => {
          const fullHref = `/${locale}${href}`;
          const active =
            href === "/admin"
              ? pathname === fullHref
              : pathname.startsWith(fullHref);
          return (
            <Link
              key={key}
              href={fullHref}
              className={cn(
                "shrink-0 flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-medium whitespace-nowrap transition-colors",
                active
                  ? "bg-slate-900 text-white dark:bg-slate-100 dark:text-slate-900"
                  : "bg-slate-100 text-slate-600 hover:bg-slate-200 dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-slate-700"
              )}
            >
              <Icon size={14} />
              {tNav(labelKey)}
            </Link>
          );
        })}
      </nav>

      <div className="flex gap-8">
        {/* Sidebar */}
        <aside className="hidden md:flex flex-col w-48 shrink-0 gap-1">
          <p className="px-4 text-xs font-semibold text-slate-400 dark:text-slate-500 uppercase tracking-wider mb-2">
            {t("title")}
          </p>
          {tabs.map(({ key, labelKey, icon: Icon, href }) => {
            const fullHref = `/${locale}${href}`;
            const active =
              href === "/admin"
                ? pathname === fullHref
                : pathname.startsWith(fullHref);
            return (
              <Link
                key={key}
                href={fullHref}
                className={cn(
                  "flex items-center gap-3 px-4 py-2.5 rounded-xl text-sm font-medium transition-colors",
                  active
                    ? "bg-slate-900 text-white dark:bg-slate-100 dark:text-slate-900"
                    : "text-slate-600 hover:bg-slate-100 hover:text-slate-900 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-white"
                )}
              >
                <Icon size={18} />
                {tNav(labelKey)}
              </Link>
            );
          })}
        </aside>

        <div className="flex-1 min-w-0">{children}</div>
      </div>
    </div>
  );
}
