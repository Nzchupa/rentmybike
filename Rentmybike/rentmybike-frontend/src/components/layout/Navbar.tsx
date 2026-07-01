"use client";

import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import { usePathname, useSearchParams } from "next/navigation";
import { Menu, X } from "lucide-react";
import { VelohoodLogo } from "@/components/VelohoodLogo";
import { useState } from "react";
import { useAuthStore } from "@/store/auth.store";
import { useAuth } from "@/hooks/useAuth";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { NotificationBell } from "@/components/layout/NotificationBell";
import { ThemeToggle } from "@/components/layout/ThemeToggle";
import { cn } from "@/lib/utils";

/**
 * Top navigation bar.
 * Obere Navigationsleiste.
 */
export function Navbar() {
  const t = useTranslations("nav");
  const locale = useLocale();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const { user, isAuthenticated, isAdmin } = useAuthStore();
  const authenticated = isAuthenticated();
  const admin = isAdmin();
  const [mobileOpen, setMobileOpen] = useState(false);
  const { logout } = useAuth();

  const localePath = (path: string) => `/${locale}${path}`;

  const isActive = (path: string) => pathname.startsWith(localePath(path));

  // Builds the href for the language switcher. Previously this used
  // pathname.replace(`/${locale}`, ...) which (a) is a plain string
  // replacement, so it could match `/en` anywhere in the path — not just the
  // locale prefix — and corrupt URLs like /en/bikes/english-rider, and
  // (b) usePathname() never includes the query string, so switching
  // language silently dropped params like ?city=Berlin&page=2. We now
  // replace only the leading locale segment and re-append the current
  // search params.
  //
  // Erstellt den Href für den Sprachumschalter. Vorher wurde
  // pathname.replace(`/${locale}`, ...) verwendet, was (a) eine reine
  // String-Ersetzung ist und daher `/en` überall im Pfad treffen konnte —
  // nicht nur im Locale-Präfix — und URLs wie /en/bikes/english-rider
  // beschädigte, und (b) usePathname() enthält nie den Query-String,
  // wodurch beim Sprachwechsel Parameter wie ?city=Berlin&page=2 stillschweigend
  // verloren gingen. Jetzt wird nur das führende Locale-Segment ersetzt und
  // die aktuellen Suchparameter werden wieder angehängt.
  const otherLocale = locale === "en" ? "de" : "en";
  const localeSwitchHref = (() => {
    const newPath = pathname.replace(new RegExp(`^/${locale}`), `/${otherLocale}`);
    const query = searchParams.toString();
    return query ? `${newPath}?${query}` : newPath;
  })();

  const navLinks = [
    { label: t("home"),  href: "/" },
    { label: t("bikes"), href: "/bikes" },
    ...(authenticated ? [{ label: t("dashboard"), href: "/dashboard" }] : []),
    ...(admin ? [{ label: t("admin"), href: "/admin" }] : []),
  ];

  return (
    <nav className="sticky top-0 z-50 bg-white/90 dark:bg-slate-900/90 backdrop-blur border-b border-slate-200 dark:border-slate-800">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex h-16 items-center justify-between">

          {/* Logo */}
          <Link href={localePath("/")} className="flex items-center gap-2 font-bold text-brand-600 dark:text-brand-400 text-lg">
            <VelohoodLogo size={28} />
            Velohood
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
                    ? "text-brand-600 dark:text-brand-400"
                    : "text-slate-600 hover:text-slate-900 dark:text-slate-300 dark:hover:text-white"
                )}
              >
                {link.label}
              </Link>
            ))}
          </div>

          {/* Auth buttons */}
          <div className="hidden md:flex items-center gap-3">
            <ThemeToggle />

            {/* Language switcher */}
            <Link
              href={localeSwitchHref}
              className="text-xs font-medium text-slate-500 hover:text-slate-900 border border-slate-200 rounded-lg px-2 py-1 dark:text-slate-400 dark:hover:text-white dark:border-slate-700"
            >
              {locale === "en" ? "DE" : "EN"}
            </Link>

            {authenticated && user ? (
              <div className="flex items-center gap-3">
                <NotificationBell />
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

          {/* Mobile-only icons + hamburger. The notification bell previously
              only rendered inside the "hidden md:flex" auth block, so
              authenticated users on mobile had no way to see notifications
              from the public navbar at all. */}
          {/* Mobile-Icons + Hamburger. Die Benachrichtigungsglocke wurde
              bisher nur im "hidden md:flex"-Block gerendert — angemeldete
              Nutzer hatten auf Mobilgeräten daher gar keinen Zugriff auf
              Benachrichtigungen über die Navbar. */}
          <div className="flex md:hidden items-center gap-1">
            {authenticated && <NotificationBell />}
            <ThemeToggle />
            <button
              className="p-2 text-slate-600 hover:text-slate-900 dark:text-slate-300 dark:hover:text-white"
              aria-label={mobileOpen ? t("closeMenu") : t("openMenu")}
              onClick={() => setMobileOpen(!mobileOpen)}
            >
              {mobileOpen ? <X size={20} /> : <Menu size={20} />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile menu */}
      {mobileOpen && (
        <div className="md:hidden border-t border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 px-4 pb-4 pt-2 space-y-2">
          {navLinks.map((link) => (
            <Link
              key={link.href}
              href={localePath(link.href)}
              className="block py-2 text-sm font-medium text-slate-700 dark:text-slate-300"
              onClick={() => setMobileOpen(false)}
            >
              {link.label}
            </Link>
          ))}
          {authenticated && user && (
            <Link
              href={localePath("/dashboard/profile")}
              className="flex items-center gap-3 py-2"
              onClick={() => setMobileOpen(false)}
            >
              <Avatar name={user.fullName} avatarUrl={user.avatarUrl} size="sm" />
              <span className="text-sm font-medium text-slate-700 dark:text-slate-300">{user.fullName}</span>
            </Link>
          )}

          <div className="flex items-center justify-between pt-2 border-t border-slate-100 dark:border-slate-800">
            <Link
              href={localeSwitchHref}
              className="text-xs font-medium text-slate-500 hover:text-slate-900 border border-slate-200 rounded-lg px-2 py-1 dark:text-slate-400 dark:hover:text-white dark:border-slate-700"
              onClick={() => setMobileOpen(false)}
            >
              {locale === "en" ? "DE" : "EN"}
            </Link>
          </div>

          <div className="pt-2 flex flex-col gap-2">
            {authenticated ? (
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
