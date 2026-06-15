"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
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
  );
}
