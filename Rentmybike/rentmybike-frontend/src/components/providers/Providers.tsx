"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { ThemeProvider } from "next-themes";
import { Toaster } from "react-hot-toast";
import { useEffect, useRef } from "react";
import { useAuth } from "@/hooks/useAuth";

/** Initialise the QueryClient once per session. */
function makeQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 60_000,    // 1 minute
        retry: 1,
      },
    },
  });
}

let browserQueryClient: QueryClient | undefined;

function getQueryClient() {
  if (typeof window === "undefined") {
    // Server: always create a new client
    return makeQueryClient();
  }
  // Browser: reuse across HMR
  if (!browserQueryClient) browserQueryClient = makeQueryClient();
  return browserQueryClient;
}

/**
 * Loads the current user once on app boot (if JWT cookie exists).
 * Lädt den aktuellen Benutzer beim App-Start (wenn JWT-Cookie vorhanden).
 */
function AuthLoader() {
  const { loadCurrentUser } = useAuth();
  const called = useRef(false);

  useEffect(() => {
    if (!called.current) {
      called.current = true;
      loadCurrentUser();
    }
  }, [loadCurrentUser]);

  return null;
}

/**
 * Client-side providers — QueryClient, Toast, Auth hydration.
 * Client-seitige Provider — QueryClient, Toast, Auth-Hydration.
 */
export function Providers({ children }: { children: React.ReactNode }) {
  const queryClient = getQueryClient();

  return (
    // attribute="class" matches tailwind.config's darkMode: "class" — next-themes
    // toggles the `dark` class on <html> rather than relying on prefers-color-scheme
    // media queries, so the in-app toggle (ThemeToggle) can override the OS
    // preference. defaultTheme="system" respects the OS setting on first visit;
    // <html suppressHydrationWarning> in [locale]/layout.tsx is required alongside
    // this (next-themes sets the class before hydration, which would otherwise
    // cause a server/client markup mismatch warning) and was already present.
    //
    // attribute="class" passt zu tailwind.config's darkMode: "class" — next-themes
    // schaltet die `dark`-Klasse auf <html> um, statt sich auf
    // prefers-color-scheme-Media-Queries zu verlassen, damit der In-App-Umschalter
    // (ThemeToggle) die Betriebssystem-Einstellung überschreiben kann.
    // defaultTheme="system" respektiert beim ersten Besuch die Betriebssystem-
    // Einstellung; <html suppressHydrationWarning> in [locale]/layout.tsx ist dafür
    // nötig (next-themes setzt die Klasse vor der Hydration, was sonst eine
    // Server/Client-Markup-Mismatch-Warnung auslösen würde) und war bereits vorhanden.
    <ThemeProvider attribute="class" defaultTheme="system" enableSystem>
      <QueryClientProvider client={queryClient}>
        <AuthLoader />
        {children}
        <Toaster
          position="top-right"
          toastOptions={{
            duration: 4000,
            style: {
              borderRadius: "12px",
              fontSize: "14px",
            },
          }}
        />
        {process.env.NODE_ENV === "development" && (
          <ReactQueryDevtools initialIsOpen={false} />
        )}
      </QueryClientProvider>
    </ThemeProvider>
  );
}
