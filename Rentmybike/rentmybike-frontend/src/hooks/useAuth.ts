"use client";

import { useRouter } from "next/navigation";
import { useLocale, useTranslations } from "next-intl";
import toast from "react-hot-toast";
import { authApi, usersApi } from "@/lib/api";
import { useAuthStore } from "@/store/auth.store";
import type { LoginRequest, RegisterRequest } from "@/types";

/**
 * Hook wrapping auth actions — login, register, logout, load current user.
 * Hook für Auth-Aktionen — Anmelden, Registrieren, Abmelden, aktuellen Benutzer laden.
 */
export function useAuth() {
  const { user, isAuthenticated, isAdmin, setUser, setLoading, logout: clearUser } =
    useAuthStore();
  const router = useRouter();
  const locale = useLocale();
  const t = useTranslations("common");

  /**
   * Load the current user profile from the backend (called on app mount).
   *
   * Uses the "silent" variant of getMe(): this runs on every page, including
   * fully public ones, so a 401 here just means "anonymous visitor" — it
   * must not trigger the global refresh-then-redirect-to-login behavior
   * that the axios interceptor applies to normal authenticated requests.
   *
   * Verwendet die "stille" Variante von getMe(): läuft auf jeder Seite,
   * auch vollständig öffentlichen, daher bedeutet ein 401 hier einfach
   * "anonymer Besucher" — es darf nicht das globale
   * Refresh-dann-Redirect-zu-Login-Verhalten auslösen, das der
   * Axios-Interceptor auf normale authentifizierte Anfragen anwendet.
   */
  async function loadCurrentUser() {
    setLoading(true);
    try {
      const res = await usersApi.getMe(true);
      setUser(res.data.data);
    } catch {
      // 401 = not logged in — clear state silently
      clearUser();
    }
  }

  async function login(data: LoginRequest) {
    // authApi.login sets the httpOnly cookie and already returns the full
    // user profile, so there's no need for a separate getMe() round trip.
    // authApi.login setzt das httpOnly-Cookie und gibt bereits das
    // vollständige Benutzerprofil zurück, daher ist kein separater
    // getMe()-Roundtrip nötig.
    const res = await authApi.login(data);
    const { userId, ...profile } = res.data.data;
    setUser({ id: userId, ...profile });
    router.push(`/${locale}/dashboard`);
  }

  async function register(data: RegisterRequest) {
    await authApi.register(data);
    // Redirect to verify-email page; in dev (auto-verify-email=true) the account
    // is already verified so the user can immediately click "back to login".
    router.push(
      `/${locale}/auth/verify-email?email=${encodeURIComponent(data.email)}`
    );
  }

  async function logout() {
    try {
      await authApi.logout();
    } catch {
      // Ignore network errors on logout
    } finally {
      clearUser();
      router.push(`/${locale}/auth/login`);
      toast.success(t("loggedOut"));
    }
  }

  return {
    user,
    isAuthenticated: isAuthenticated(),
    isAdmin: isAdmin(),
    login,
    register,
    logout,
    loadCurrentUser,
  };
}
