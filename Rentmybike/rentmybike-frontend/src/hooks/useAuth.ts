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

  /** Load the current user profile from the backend (called on app mount). */
  async function loadCurrentUser() {
    setLoading(true);
    try {
      const res = await usersApi.getMe();
      setUser(res.data.data);
    } catch {
      // 401 = not logged in — clear state silently
      clearUser();
    }
  }

  async function login(data: LoginRequest) {
    // authApi.login sets the httpOnly cookie; then load the full profile
    await authApi.login(data);
    const profileRes = await usersApi.getMe();
    setUser(profileRes.data.data);
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
