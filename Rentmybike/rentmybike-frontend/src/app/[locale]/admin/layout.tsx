"use client";

import { useEffect } from "react";
import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import { usePathname, useRouter } from "next/navigation";
import { BarChart3, Users, Bike } from "lucide-react";
import { useAuthStore } from "@/store/auth.store";
import { cn } from "@/lib/utils";

const tabs = [
  { key: "stats",  label: "Statistics", icon: BarChart3, href: "/admin" },
  { key: "users",  label: "Users",      icon: Users,     href: "/admin/users" },
  { key: "bikes",  label: "Bikes",      icon: Bike,      href: "/admin/bikes" },
];

/**
 * Admin panel layout — admin guard + sidebar.
 * Admin-Panel-Layout — Admin-Guard + Seitenleiste.
 */
export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const t = useTranslations("admin");
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
      <div className="flex gap-8">
        {/* Sidebar */}
        <aside className="hidden md:flex flex-col w-48 shrink-0 gap-1">
          <p className="px-4 text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">
            {t("title")}
          </p>
          {tabs.map(({ key, label, icon: Icon, href }) => {
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
                    ? "bg-slate-900 text-white"
                    : "text-slate-600 hover:bg-slate-100 hover:text-slate-900"
                )}
              >
                <Icon size={18} />
                {label}
              </Link>
            );
          })}
        </aside>

        <div className="flex-1 min-w-0">{children}</div>
      </div>
    </div>
  );
}
