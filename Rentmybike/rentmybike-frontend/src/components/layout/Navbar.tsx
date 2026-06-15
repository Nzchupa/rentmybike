"use client";

import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import { usePathname } from "next/navigation";
import { Bike, Menu, X } from "lucide-react";
import { useState } from "react";
import { useAuthStore } from "@/store/auth.store";
import { useAuth } from "@/hooks/useAuth";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { cn } from "@/lib/utils";

/**
 * Top navigation bar.
 * Obere Navigationsleiste.
 */
export function Navbar() {
  const t = useTranslations("nav");
  const locale = useLocale();
  const pathname = usePathname();
  const { user, isAuthenticated, isAdmin } = useAuthStore();
  const [mobileOpen, setMobileOpen] = useState(false);
  const { logout } = useAuth();

  const localePath = (path: string) => `/${locale}${path}`;

  const isActive = (path: string) => pathname.startsWith(localePath(path));

  const navLinks = [
    { label: t("home"),  href: "/" },
    { label: t("bikes"), href: "/bikes" },
    ...(isAuthenticated ? [{ label: t("dashboard"), href: "/dashboard" }] : []),
    ...(isAdmin ? [{ label: t("admin"), href: "/admin" }] : []),
  ];

  return (
    <nav className="sticky top-0 z-50 bg-white/90 backdrop-blur border-b border-slate-200">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex h-16 items-center justify-between">

          {/* Logo */}
          <Link href={localePath("/")} className="flex items-center gap-2 font-bold text-brand-600 text-lg">
            <Bike size={24} className="text-brand-500" />
            RentMyBike
          </Link>

          {/* Desktop nav */}
          <div className="hidden md:flex items-center gap-6">
            {navLinks.map((link) => (
              <Link
                key={link.href}
                href={localePath(link.href)}
                className={cn(
                  "text-sm font-medium transition-colors",
                  isActive(link.href)
                    ? "text-brand-600"
                    : "text-slate-600 hover:text-slate-900"
                )}
              >
                {link.label}
              </Link>
            ))}
          </div>

          {/* Auth buttons */}
          <div className="hidden md:flex items-center gap-3">
            {/* Language switcher */}
            <Link
              href={pathname.replace(`/${locale}`, locale === "en" ? "/de" : "/en")}
              className="text-xs font-medium text-slate-500 hover:text-slate-900 border border-slate-200 rounded-lg px-2 py-1"
            >
              {locale === "en" ? "DE" : "EN"}
            </Link>

            {isAuthenticated && user ? (
              <div className="flex items-center gap-3">
                <Link href={localePath("/dashboard/profile")}>
                  <Avatar name={user.fullName} avatarUrl={user.avatarUrl} size="sm" />
                </Link>
                <Button variant="ghost" size="sm" onClick={logout}>
                  {t("logout")}
                </Button>
              </div>
            ) : (
              <>
                <Button variant="ghost" size="sm" asChild>
                  <Link href={localePath("/auth/login")}>{t("login")}</Link>
                </Button>
                <Button size="sm" asChild>
                  <Link href={localePath("/auth/register")}>{t("register")}</Link>
                </Button>
              </>
            )}
          </div>

          {/* Mobile hamburger */}
          <button
            className="md:hidden p-2 text-slate-600 hover:text-slate-900"
            onClick={() => setMobileOpen(!mobileOpen)}
          >
            {mobileOpen ? <X size={20} /> : <Menu size={20} />}
          </button>
        </div>
      </div>

      {/* Mobile menu */}
      {mobileOpen && (
        <div className="md:hidden border-t border-slate-200 bg-white px-4 pb-4 pt-2 space-y-2">
          {navLinks.map((link) => (
            <Link
              key={link.href}
              href={localePath(link.href)}
              className="block py-2 text-sm font-medium text-slate-700"
              onClick={() => setMobileOpen(false)}
            >
              {link.label}
            </Link>
          ))}
          <div className="pt-2 flex flex-col gap-2">
            {isAuthenticated ? (
              <Button variant="outline" size="sm" onClick={logout} className="w-full">
                {t("logout")}
              </Button>
            ) : (
              <>
                <Button variant="outline" size="sm" asChild className="w-full">
                  <Link href={localePath("/auth/login")}>{t("login")}</Link>
                </Button>
                <Button size="sm" asChild className="w-full">
                  <Link href={localePath("/auth/register")}>{t("register")}</Link>
                </Button>
              </>
            )}
          </div>
        </div>
      )}
    </nav>
  );
}
