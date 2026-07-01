"use client";

import { useEffect, useState } from "react";
import { useTheme } from "next-themes";
import { Moon, Sun } from "lucide-react";
import { useTranslations } from "next-intl";
import { cn } from "@/lib/utils";

interface ThemeToggleProps {
  className?: string;
}

/**
 * Light/dark mode toggle button.
 * Hell-/Dunkelmodus-Umschalter.
 *
 * <p>`resolvedTheme` (not `theme`) drives the icon/click target, since
 * `theme` can be "system" — in that case `resolvedTheme` is next-themes'
 * computed actual value ("light" or "dark") based on the OS preference, and
 * clicking should flip *that*, not toggle between "system" and one fixed
 * mode.
 * <p>`resolvedTheme` (nicht `theme`) steuert Icon/Klickziel, da `theme` auch
 * "system" sein kann — in diesem Fall ist `resolvedTheme` der von next-themes
 * berechnete tatsächliche Wert ("light" oder "dark") basierend auf der
 * Betriebssystem-Einstellung, und ein Klick sollte genau diesen umschalten,
 * nicht zwischen "system" und einem fixen Modus wechseln.
 *
 * <p>Renders a same-size invisible placeholder until mounted — next-themes
 * can only know the resolved theme on the client (it depends on
 * localStorage/OS preference), so rendering the real icon during SSR/first
 * paint would either guess wrong or cause a hydration mismatch.
 * <p>Rendert bis zum Mount einen unsichtbaren Platzhalter in gleicher
 * Größe — next-themes kennt das aufgelöste Theme erst auf dem Client (es
 * hängt von localStorage/Betriebssystem-Einstellung ab), daher würde das
 * Rendern des echten Icons während SSR/erstem Paint entweder falsch raten
 * oder eine Hydration-Mismatch verursachen.
 */
export function ThemeToggle({ className }: ThemeToggleProps) {
  const t = useTranslations("nav");
  const { resolvedTheme, setTheme } = useTheme();
  const [mounted, setMounted] = useState(false);

  useEffect(() => setMounted(true), []);

  if (!mounted) {
    return <div className={cn("w-9 h-9", className)} aria-hidden="true" />;
  }

  const isDark = resolvedTheme === "dark";

  return (
    <button
      type="button"
      onClick={() => setTheme(isDark ? "light" : "dark")}
      aria-label={isDark ? t("switchToLightMode") : t("switchToDarkMode")}
      className={cn(
        "flex items-center justify-center w-9 h-9 rounded-lg text-slate-600 hover:text-slate-900 hover:bg-slate-100",
        "dark:text-slate-300 dark:hover:text-white dark:hover:bg-slate-800",
        "transition-colors",
        className
      )}
    >
      {isDark ? <Sun size={18} /> : <Moon size={18} />}
    </button>
  );
}
