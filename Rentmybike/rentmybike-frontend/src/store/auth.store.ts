import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { UserProfileResponse } from "@/types";

/**
 * Global auth state — persisted to sessionStorage so page refresh keeps user logged in.
 * Globaler Auth-Zustand — in sessionStorage gespeichert, damit Seitenaktualisierung
 * den Benutzer angemeldet hält.
 *
 * Note: the actual JWT is in an httpOnly cookie, managed by the browser.
 * The store only holds the user profile for UI rendering.
 *
 * Hinweis: Das eigentliche JWT befindet sich in einem httpOnly-Cookie, das vom Browser
 * verwaltet wird. Der Store enthält nur das Benutzerprofil für die UI-Darstellung.
 */
interface AuthState {
  user: UserProfileResponse | null;
  isLoading: boolean;

  /** Call after successful login or on initial page load / Nach Login oder beim Laden aufrufen */
  setUser: (user: UserProfileResponse | null) => void;

  /** Whether any user is authenticated / Ob ein Benutzer authentifiziert ist */
  isAuthenticated: () => boolean;

  /** Whether the current user is an admin / Ob der aktuelle Benutzer Admin ist */
  isAdmin: () => boolean;

  setLoading: (loading: boolean) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      isLoading: true,

      setUser: (user) => set({ user, isLoading: false }),

      isAuthenticated: () => get().user !== null,

      isAdmin: () => get().user?.role === "ADMIN",

      setLoading: (isLoading) => set({ isLoading }),

      logout: () => set({ user: null, isLoading: false }),
    }),
    {
      name: "rmb-auth",
      storage: {
        // Use sessionStorage so auth clears on browser close
        // sessionStorage verwenden damit Auth beim Schließen des Browsers gelöscht wird
        getItem: (key) => {
          if (typeof window === "undefined") return null;
          const item = sessionStorage.getItem(key);
          return item ? JSON.parse(item) : null;
        },
        setItem: (key, value) => {
          if (typeof window !== "undefined") {
            sessionStorage.setItem(key, JSON.stringify(value));
          }
        },
        removeItem: (key) => {
          if (typeof window !== "undefined") {
            sessionStorage.removeItem(key);
          }
        },
      },
    }
  )
);
