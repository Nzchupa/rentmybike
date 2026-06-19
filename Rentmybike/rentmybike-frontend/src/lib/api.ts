import axios, { AxiosError, AxiosRequestConfig } from "axios";
import { useAuthStore } from "@/store/auth.store";

/**
 * Pre-configured Axios instance pointing at the Spring Boot backend.
 * Vorkonfigurierte Axios-Instanz, die auf das Spring Boot Backend zeigt.
 *
 * Key settings:
 * - withCredentials: true  — sends httpOnly JWT cookies on every request
 * - baseURL from env       — defaults to http://localhost:8080
 */
export const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080",
  withCredentials: true,      // ← must be true for cookie-based JWT auth
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 15_000,
  // CSRF double-submit cookie: the backend issues a readable XSRF-TOKEN
  // cookie (CookieCsrfTokenRepository) and rejects state-changing requests
  // unless the same value is echoed back as the X-XSRF-TOKEN header — a
  // header a cross-site attacker's form/script cannot read or set. Axios
  // does this automatically given these three options; `withXSRFToken: true`
  // is required because our frontend and backend are on different origins
  // (axios only auto-attaches the header for same-origin requests by default).
  // CSRF Double-Submit-Cookie: Das Backend stellt ein lesbares
  // XSRF-TOKEN-Cookie aus (CookieCsrfTokenRepository) und lehnt
  // zustandsändernde Anfragen ab, sofern nicht derselbe Wert als
  // X-XSRF-TOKEN-Header zurückgesendet wird — ein Header, den das
  // Formular/Skript eines Cross-Site-Angreifers nicht lesen oder setzen
  // kann. Axios erledigt dies automatisch mit diesen drei Optionen;
  // `withXSRFToken: true` ist erforderlich, da Frontend und Backend auf
  // unterschiedlichen Origins liegen (Axios hängt den Header standardmäßig
  // nur bei Anfragen zum gleichen Origin automatisch an).
  xsrfCookieName: "XSRF-TOKEN",
  xsrfHeaderName: "X-XSRF-TOKEN",
  withXSRFToken: true,
});

// ─────────────────────────────────────────────────────────────────────────────
// CSRF cookie priming — fixes "403 on first mutation of the session"
// CSRF-Cookie-Priming — behebt "403 bei erster Mutation der Sitzung"
//
// The backend only writes the XSRF-TOKEN cookie lazily (CsrfCookieFilter
// forces it on every request, but the cookie still has to round-trip back
// from a *prior* response before the browser has it to echo on the *next*
// request). In practice the app-boot AuthLoader fires a GET /users/me that
// would normally prime this cookie — but that GET runs in a fire-and-forget
// useEffect, so a user who lands directly on a page and submits a mutation
// (e.g. PUT /bikes/{id}) immediately can have their very first request go
// out before that priming GET has resolved and the cookie has been stored.
// The result: no X-XSRF-TOKEN header on that first PUT/POST/DELETE → 403 from
// Spring's CsrfFilter. The retry then succeeds because the cookie is in place
// by then — exactly the "fails once, then works" symptom.
//
// This interceptor closes the gap structurally: before any state-changing
// request, if no XSRF-TOKEN cookie is present yet, it fires a lightweight GET
// to obtain one and waits for it, so the mutation that follows always has a
// token to echo back — regardless of whether AuthLoader's own priming request
// has completed yet.
//
// Das Backend schreibt das XSRF-TOKEN-Cookie nur lazy (CsrfCookieFilter
// erzwingt es bei jeder Anfrage, aber das Cookie muss erst aus einer
// *vorherigen* Antwort zurückkommen, bevor der Browser es bei der *nächsten*
// Anfrage zurücksenden kann). In der Praxis löst der App-Start-AuthLoader ein
// GET /users/me aus, das dieses Cookie normalerweise vorab setzen würde —
// aber dieses GET läuft in einem Fire-and-Forget-useEffect, sodass ein
// Benutzer, der direkt auf einer Seite landet und sofort eine Mutation
// absendet (z. B. PUT /bikes/{id}), seine allererste Anfrage abschicken kann,
// bevor diese Priming-GET-Anfrage abgeschlossen und das Cookie gespeichert
// wurde. Ergebnis: kein X-XSRF-TOKEN-Header bei diesem ersten PUT/POST/DELETE
// → 403 von Spring's CsrfFilter. Der Wiederholungsversuch klappt dann, weil
// das Cookie inzwischen vorhanden ist — genau das Symptom "schlägt einmal
// fehl, funktioniert dann".
//
// Dieser Interceptor schließt die Lücke strukturell: Vor jeder
// zustandsändernden Anfrage wird, falls noch kein XSRF-TOKEN-Cookie vorhanden
// ist, ein leichtgewichtiges GET ausgelöst, um eines zu erhalten, und
// abgewartet — sodass die nachfolgende Mutation immer ein Token zum
// Zurücksenden hat, unabhängig davon, ob die eigene Priming-Anfrage von
// AuthLoader schon abgeschlossen ist.
// ─────────────────────────────────────────────────────────────────────────────

const CSRF_SAFE_METHODS = new Set(["get", "head", "options"]);

function readCookie(name: string): string | null {
  if (typeof document === "undefined") return null;
  const match = document.cookie.match(
    new RegExp("(?:^|; )" + name.replace(/([.$?*|{}()[\]\\/+^])/g, "\\$1") + "=([^;]*)")
  );
  return match ? decodeURIComponent(match[1]) : null;
}

let csrfPrimingPromise: Promise<void> | null = null;

/**
 * Fires (once, shared) a cheap GET so the backend issues the XSRF-TOKEN
 * cookie. Reuses /api/v1/users/me rather than adding a new endpoint: it
 * already passes through the full security filter chain (so
 * CsrfCookieFilter forces the token to be resolved and the cookie written),
 * works whether the visitor is authenticated or not (skipAuthRedirect avoids
 * the 401→refresh→redirect dance for an anonymous caller), and costs nothing
 * extra since AuthLoader already calls it once per session anyway.
 *
 * Löst (einmal, gemeinsam genutzt) ein günstiges GET aus, damit das Backend
 * das XSRF-TOKEN-Cookie ausstellt. Nutzt /api/v1/users/me wieder, statt einen
 * neuen Endpunkt hinzuzufügen: durchläuft bereits die vollständige
 * Sicherheits-Filterkette (sodass CsrfCookieFilter das Token auflöst und das
 * Cookie schreibt), funktioniert unabhängig davon, ob der Besucher
 * authentifiziert ist (skipAuthRedirect vermeidet den
 * 401→Refresh→Redirect-Ablauf für einen anonymen Aufrufer), und kostet
 * nichts zusätzlich, da AuthLoader es ohnehin einmal pro Sitzung aufruft.
 */
function primeCsrfCookie(): Promise<void> {
  if (!csrfPrimingPromise) {
    csrfPrimingPromise = api
      .get("/api/v1/users/me", { skipAuthRedirect: true } as unknown as AxiosRequestConfig)
      .then(() => undefined)
      .catch(() => undefined) // best-effort — the mutation's own error handling covers failure
      .finally(() => {
        csrfPrimingPromise = null;
      });
  }
  return csrfPrimingPromise;
}

api.interceptors.request.use(async (config) => {
  const method = (config.method ?? "get").toLowerCase();
  if (!CSRF_SAFE_METHODS.has(method) && !readCookie("XSRF-TOKEN")) {
    await primeCsrfCookie();
  }
  return config;
});

// ─────────────────────────────────────────────────────────────────────────────
// 401 → refresh → retry
// 401 → Aktualisieren → Wiederholen
//
// The access token is short-lived (15 min). Previously a 401 from an expired
// access token just bubbled up as an error — authApi.refreshToken existed but
// was never called, so every user got silently logged out every 15 minutes.
// This interceptor transparently calls /auth/refresh once and retries the
// original request; only if the refresh itself fails do we clear local auth
// state and send the user to /login.
//
// Der Zugriffstoken ist kurzlebig (15 Min). Vorher wurde ein 401 durch einen
// abgelaufenen Zugriffstoken einfach als Fehler durchgereicht — authApi.
// refreshToken existierte, wurde aber nie aufgerufen, sodass jeder Benutzer
// alle 15 Minuten stillschweigend abgemeldet wurde. Dieser Interceptor ruft
// transparent einmal /auth/refresh auf und wiederholt die ursprüngliche
// Anfrage; nur wenn die Aktualisierung selbst fehlschlägt, wird der lokale
// Auth-Zustand gelöscht und der Benutzer zu /login geschickt.
// ─────────────────────────────────────────────────────────────────────────────

// `skipAuthRedirect` marks a request as a "silent" auth check — used by the
// app-boot /users/me probe (AuthLoader), which runs on EVERY page including
// fully public ones. Without this flag, a first-time/anonymous visitor (no
// cookies at all) hitting any page would trigger: 401 on /users/me → this
// interceptor tries /auth/refresh → that also 401s (no refresh cookie) →
// the catch block below called redirectToLogin() unconditionally, hard-
// navigating an anonymous visitor browsing the homepage straight to /login.
// With the flag set, a failure here is treated as "simply not logged in"
// and resolved silently — no refresh attempt, no redirect.
//
// `skipAuthRedirect` markiert eine Anfrage als "stillen" Auth-Check —
// verwendet vom App-Start-/users/me-Check (AuthLoader), der auf JEDER
// Seite läuft, auch vollständig öffentlichen. Ohne dieses Flag würde ein
// Erstbesucher/anonymer Besucher (keine Cookies) auf jeder Seite Folgendes
// auslösen: 401 bei /users/me → dieser Interceptor versucht /auth/refresh →
// das gibt ebenfalls 401 (kein Refresh-Cookie) → der catch-Block unten rief
// bedingungslos redirectToLogin() auf und navigierte einen anonymen, die
// Startseite besuchenden Nutzer hart zu /login. Mit gesetztem Flag wird ein
// Fehlschlag hier still als "einfach nicht angemeldet" behandelt — kein
// Refresh-Versuch, kein Redirect.
type RetryableConfig = AxiosRequestConfig & { _retry?: boolean; skipAuthRedirect?: boolean };

/**
 * Error thrown by the response interceptor. Carries the original HTTP status
 * code so callers can distinguish e.g. a 401 (session issue) from a 500
 * (transient server error) instead of treating every failure identically.
 *
 * Fehler, der vom Response-Interceptor geworfen wird. Trägt den
 * ursprünglichen HTTP-Statuscode, damit Aufrufer z. B. einen 401
 * (Sitzungsproblem) von einem 500 (vorübergehender Serverfehler)
 * unterscheiden können, statt jeden Fehler gleich zu behandeln.
 */
export class ApiError extends Error {
  readonly status?: number;

  constructor(message: string, status?: number) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

// Endpoints that must never trigger a refresh attempt themselves, otherwise
// a failed login/refresh could recurse into another refresh call.
const AUTH_ENDPOINTS = ["/api/v1/auth/refresh", "/api/v1/auth/login", "/api/v1/auth/register"];

// Shared in-flight refresh promise so concurrent 401s from several requests
// firing at once only trigger a single /auth/refresh call.
let refreshPromise: Promise<void> | null = null;

// This module runs outside the React tree (axios interceptor), so the
// next-intl useTranslations() hook isn't available here. The active locale
// is instead read from the URL path segment (same trick redirectToLogin()
// already uses below), and these two generic fallback messages are looked
// up from a tiny inline dictionary so they don't always render bilingually
// regardless of the active locale.
//
// Dieses Modul läuft außerhalb des React-Baums (Axios-Interceptor), daher
// ist der next-intl useTranslations()-Hook hier nicht verfügbar. Die aktive
// Sprache wird stattdessen aus dem URL-Pfadsegment gelesen (derselbe Trick,
// den redirectToLogin() unten bereits verwendet), und diese beiden
// allgemeinen Fallback-Meldungen werden aus einem kleinen, eingebetteten
// Wörterbuch geholt, damit sie nicht unabhängig von der aktiven Sprache
// immer zweisprachig angezeigt werden.
const FALLBACK_MESSAGES = {
  en: { sessionExpired: "Session expired — please log in again", unknownError: "Unknown error" },
  de: { sessionExpired: "Sitzung abgelaufen — bitte erneut anmelden", unknownError: "Unbekannter Fehler" },
} as const;

function getCurrentLocale(): keyof typeof FALLBACK_MESSAGES {
  if (typeof window === "undefined") return "en";
  const seg = window.location.pathname.split("/")[1];
  return seg === "de" ? "de" : "en";
}

function redirectToLogin() {
  useAuthStore.getState().logout();
  if (typeof window !== "undefined") {
    const locale = window.location.pathname.split("/")[1] || "en";
    window.location.href = `/${locale}/auth/login`;
  }
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<{ message?: string }>) => {
    const originalRequest = error.config as RetryableConfig | undefined;
    const status = error.response?.status;
    const url = originalRequest?.url ?? "";
    const isAuthEndpoint = AUTH_ENDPOINTS.some((p) => url.includes(p));

    if (status === 401 && originalRequest && !originalRequest._retry && !isAuthEndpoint) {
      originalRequest._retry = true;

      try {
        if (!refreshPromise) {
          refreshPromise = api
            .post("/api/v1/auth/refresh")
            .then(() => undefined)
            .finally(() => {
              refreshPromise = null;
            });
        }
        // Still attempt the refresh even for a "silent" request (e.g. the
        // AuthLoader boot check) — a returning user with an expired access
        // token but a still-valid refresh token must get their session
        // restored. Only the *redirect on ultimate failure* is skipped for
        // silent requests, since a 401 there can legitimately mean
        // "anonymous visitor", not "session expired".
        //
        // Versuche das Refresh auch bei einer "stillen" Anfrage (z. B. der
        // AuthLoader-Boot-Check) — ein wiederkehrender Nutzer mit
        // abgelaufenem Zugriffstoken, aber noch gültigem Refresh-Token, muss
        // seine Sitzung wiederherstellen können. Nur das *Redirect bei
        // endgültigem Fehlschlag* wird bei stillen Anfragen übersprungen, da
        // ein 401 dort legitim "anonymer Besucher" bedeuten kann, nicht
        // "Sitzung abgelaufen".
        await refreshPromise;
        return api(originalRequest);
      } catch {
        if (!originalRequest.skipAuthRedirect) {
          redirectToLogin();
        }
        return Promise.reject(
          new ApiError(FALLBACK_MESSAGES[getCurrentLocale()].sessionExpired, 401)
        );
      }
    }

    // Previously the status code was discarded here, so every failure (a
    // 401 session issue, a 422 validation error, a 500 transient server
    // error) looked identical to callers — code that wanted to e.g. show a
    // "try again" message only for 5xx, or surface field errors only for
    // 422, had no way to do that.
    //
    // Vorher wurde der Statuscode hier verworfen, sodass jeder Fehler (ein
    // 401-Sitzungsproblem, ein 422-Validierungsfehler, ein vorübergehender
    // 500-Serverfehler) für Aufrufer identisch aussah — Code, der z. B. nur
    // bei 5xx eine "Erneut versuchen"-Meldung oder nur bei 422 Feldfehler
    // anzeigen wollte, hatte dazu keine Möglichkeit.
    const message =
      error.response?.data?.message ??
      error.message ??
      FALLBACK_MESSAGES[getCurrentLocale()].unknownError;
    return Promise.reject(new ApiError(message, status));
  }
);

// ─────────────────────────────────────────────────────────────────────────────
// Typed API helper functions
// Typisierte API-Hilfsfunktionen
// ─────────────────────────────────────────────────────────────────────────────

import type {
  ApiResponse,
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  UserProfileResponse,
  PublicUserResponse,
  UpdateProfileRequest,
  ChangePasswordRequest,
  BikeResponse,
  CreateBikeRequest,
  UpdateBikeRequest,
  PageResponse,
  BookingResponse,
  CreateBookingRequest,
  ReviewResponse,
  CreateReviewRequest,
  UserRatingResponse,
  AdminUserResponse,
  AdminStatsResponse,
  NotificationResponse,
  UnreadCountResponse,
  FavoriteStatusResponse,
  BookingPhotoResponse,
  BookingPhotoPhase,
  ChatMessageResponse,
  UpgradeToBusinessRequest,
  BusinessDashboardSummaryResponse,
  BusinessOverviewExtrasResponse,
  BusinessAnalyticsResponse,
  BulkCreateBikeRequest,
  AccessoryResponse,
  CreateAccessoryRequest,
  UpdateAccessoryRequest,
  AuditLogResponse,
  AuditAction,
  AdminAnalyticsResponse,
  ReportResponse,
  ReportStatus,
  ReportTargetType,
  CreateReportRequest,
} from "@/types";

// ── Auth ─────────────────────────────────────────────────────────────────────

export const authApi = {
  login: (data: LoginRequest) =>
    api.post<ApiResponse<AuthResponse>>("/api/v1/auth/login", data),

  register: (data: RegisterRequest) =>
    api.post<ApiResponse<{ message: string }>>("/api/v1/auth/register", data),

  logout: () =>
    api.post<ApiResponse<null>>("/api/v1/auth/logout"),

  refreshToken: () =>
    api.post<ApiResponse<null>>("/api/v1/auth/refresh"),

  verifyEmail: (token: string) =>
    api.get<ApiResponse<null>>(`/api/v1/auth/verify-email?token=${token}`),
};

// ── Users ─────────────────────────────────────────────────────────────────────

export const usersApi = {
  // `silent`: pass true for the app-boot auth probe (AuthLoader) so an
  // anonymous visitor's 401 here doesn't trigger a refresh attempt or a
  // hard redirect to /login — see the `skipAuthRedirect` comment in
  // api.ts's response interceptor for the full story.
  getMe: (silent = false) =>
    api.get<ApiResponse<UserProfileResponse>>("/api/v1/users/me", {
      skipAuthRedirect: silent,
    } as unknown as AxiosRequestConfig),

  updateProfile: (data: UpdateProfileRequest) =>
    api.put<ApiResponse<UserProfileResponse>>("/api/v1/users/me", data),

  uploadAvatar: (file: File) => {
    const form = new FormData();
    form.append("file", file);
    // See uploadPhoto below for why Content-Type must stay unset for FormData uploads.
    return api.put<ApiResponse<UserProfileResponse>>("/api/v1/users/me/avatar", form, {
      headers: { "Content-Type": undefined },
    });
  },

  changePassword: (data: ChangePasswordRequest) =>
    api.put<ApiResponse<null>>("/api/v1/users/me/password", data),

  getPublicProfile: (userId: string) =>
    api.get<ApiResponse<PublicUserResponse>>(`/api/v1/users/${userId}/public`),
};

// ── Bikes ─────────────────────────────────────────────────────────────────────

export const bikesApi = {
  search: (params: {
    city?: string;
    category?: string;
    minPrice?: number;
    maxPrice?: number;
    page?: number;
    size?: number;
  }) =>
    api.get<ApiResponse<PageResponse<BikeResponse>>>("/api/v1/bikes", { params }),

  getById: (id: string) =>
    api.get<ApiResponse<BikeResponse>>(`/api/v1/bikes/${id}`),

  // Owner-scoped variant — returns the bike regardless of approval status
  // (PENDING/REJECTED/APPROVED) plus owner-only fields. Use this instead of
  // getById() whenever the caller is the bike's owner (e.g. the edit page),
  // since getById() hits the public endpoint which 404s for anything that
  // isn't APPROVED.
  // Eigentümer-spezifische Variante — gibt das Fahrrad unabhängig vom
  // Genehmigungsstatus zurück (PENDING/REJECTED/APPROVED) plus
  // eigentümer-spezifische Felder. Anstelle von getById() verwenden, wenn
  // der Aufrufer der Fahrrad-Eigentümer ist (z. B. die Bearbeitungsseite),
  // da getById() den öffentlichen Endpunkt aufruft, der bei allem außer
  // APPROVED mit 404 antwortet.
  getOwnerById: (id: string) =>
    api.get<ApiResponse<BikeResponse>>(`/api/v1/bikes/${id}/owner`),

  getMyBikes: (page = 0, size = 20) =>
    api.get<ApiResponse<PageResponse<BikeResponse>>>("/api/v1/bikes/my", {
      params: { page, size },
    }),

  create: (data: CreateBikeRequest) =>
    api.post<ApiResponse<BikeResponse>>("/api/v1/bikes", data),

  update: (id: string, data: UpdateBikeRequest) =>
    api.put<ApiResponse<BikeResponse>>(`/api/v1/bikes/${id}`, data),

  delete: (id: string) =>
    api.delete<ApiResponse<null>>(`/api/v1/bikes/${id}`),

  uploadPhoto: (bikeId: string, file: File) => {
    const form = new FormData();
    form.append("file", file);
    // Don't set Content-Type explicitly: the axios instance defaults to
    // "application/json", but a hardcoded "multipart/form-data" (without a
    // boundary) here stomped the browser's own header instead of adding to
    // it, so the backend received a multipart body with no boundary
    // delimiter and failed to parse it — surfaced to the user as a generic
    // upload error. Leaving the header unset lets the browser generate
    // "multipart/form-data; boundary=..." itself from the FormData body.
    //
    // Content-Type hier nicht explizit setzen: Die Axios-Instanz verwendet
    // standardmäßig "application/json", aber ein hartkodiertes
    // "multipart/form-data" (ohne boundary) hat hier den vom Browser
    // gesetzten Header überschrieben statt ihn zu ergänzen, sodass das
    // Backend einen multipart-Body ohne boundary-Trennzeichen erhielt und
    // ihn nicht parsen konnte — dem Benutzer als allgemeiner Upload-Fehler
    // angezeigt. Bleibt der Header unset, erzeugt der Browser
    // "multipart/form-data; boundary=..." selbst aus dem FormData-Body.
    return api.post<ApiResponse<BikeResponse>>(
      `/api/v1/bikes/${bikeId}/photos`,
      form,
      { headers: { "Content-Type": undefined } }
    );
  },

  deletePhoto: (bikeId: string, photoId: string) =>
    api.delete<ApiResponse<null>>(`/api/v1/bikes/${bikeId}/photos/${photoId}`),

  // Admin
  adminList: (status?: string, page = 0, size = 20) =>
    api.get<ApiResponse<PageResponse<BikeResponse>>>("/api/v1/admin/bikes", {
      params: { status, page, size },
    }),

  adminApprove: (id: string) =>
    api.post<ApiResponse<BikeResponse>>(`/api/v1/admin/bikes/${id}/approve`),

  adminReject: (id: string, reason: string) =>
    api.post<ApiResponse<BikeResponse>>(`/api/v1/admin/bikes/${id}/reject`, {
      reason,
    }),

  adminRequestChanges: (id: string, reason: string) =>
    api.post<ApiResponse<BikeResponse>>(`/api/v1/admin/bikes/${id}/request-changes`, {
      reason,
    }),
};

// ── Bookings ──────────────────────────────────────────────────────────────────

export const bookingsApi = {
  create: (data: CreateBookingRequest) =>
    api.post<ApiResponse<BookingResponse>>("/api/v1/bookings", data),

  getById: (id: string) =>
    api.get<ApiResponse<BookingResponse>>(`/api/v1/bookings/${id}`),

  getMyRenterBookings: (page = 0, size = 20) =>
    api.get<ApiResponse<PageResponse<BookingResponse>>>("/api/v1/bookings/my/renter", {
      params: { page, size },
    }),

  getMyOwnerBookings: (status?: string, page = 0, size = 20) =>
    api.get<ApiResponse<PageResponse<BookingResponse>>>("/api/v1/bookings/my/owner", {
      params: { status, page, size },
    }),

  accept: (id: string) =>
    api.post<ApiResponse<BookingResponse>>(`/api/v1/bookings/${id}/accept`),

  reject: (id: string) =>
    api.post<ApiResponse<BookingResponse>>(`/api/v1/bookings/${id}/reject`),

  cancel: (id: string) =>
    api.post<ApiResponse<BookingResponse>>(`/api/v1/bookings/${id}/cancel`),

  adminComplete: (id: string) =>
    api.post<ApiResponse<BookingResponse>>(`/api/v1/admin/bookings/${id}/complete`),

  // Public — occupied date ranges for a bike, used by the booking calendar
  // to disable already-taken dates / Öffentlich — belegte Datumsbereiche für
  // ein Fahrrad, vom Buchungskalender zum Deaktivieren vergebener Termine genutzt.
  getBookedDates: (bikeId: string) =>
    api.get<ApiResponse<{ startDate: string; endDate: string }[]>>(
      `/api/v1/bookings/bike/${bikeId}/booked-dates`
    ),
};

// ── Booking photos (before/after) ───────────────────────────────────────────

export const bookingPhotosApi = {
  list: (bookingId: string) =>
    api.get<ApiResponse<BookingPhotoResponse[]>>(`/api/v1/bookings/${bookingId}/photos`),

  upload: (bookingId: string, file: File, phase: BookingPhotoPhase) => {
    const form = new FormData();
    form.append("file", file);
    form.append("phase", phase);
    // See bikesApi.uploadPhoto above for why Content-Type must stay unset for FormData uploads.
    return api.post<ApiResponse<BookingPhotoResponse>>(
      `/api/v1/bookings/${bookingId}/photos`,
      form,
      { headers: { "Content-Type": undefined } }
    );
  },

  remove: (bookingId: string, photoId: string) =>
    api.delete<ApiResponse<null>>(`/api/v1/bookings/${bookingId}/photos/${photoId}`),
};

// ── Chat ──────────────────────────────────────────────────────────────────────
// Sending happens over the STOMP /app/chat/{bookingId}/send destination
// (see ChatPanel) — this REST call only loads the initial history.
// Senden erfolgt über das STOMP-Ziel /app/chat/{bookingId}/send (siehe
// ChatPanel) — dieser REST-Aufruf lädt nur den initialen Verlauf.

export const chatApi = {
  getHistory: (bookingId: string) =>
    api.get<ApiResponse<ChatMessageResponse[]>>(`/api/v1/bookings/${bookingId}/chat`),
};

// ── Reviews ───────────────────────────────────────────────────────────────────

export const reviewsApi = {
  create: (data: CreateReviewRequest) =>
    api.post<ApiResponse<ReviewResponse>>("/api/v1/reviews", data),

  getBikeReviews: (bikeId: string, page = 0, size = 10) =>
    api.get<ApiResponse<PageResponse<ReviewResponse>>>(
      `/api/v1/reviews/bike/${bikeId}`,
      { params: { page, size } }
    ),

  getBikeRating: (bikeId: string) =>
    api.get<ApiResponse<UserRatingResponse>>(
      `/api/v1/reviews/bike/${bikeId}/rating`
    ),

  getUserReviews: (userId: string, page = 0, size = 10) =>
    api.get<ApiResponse<PageResponse<ReviewResponse>>>(
      `/api/v1/reviews/user/${userId}`,
      { params: { page, size } }
    ),

  getUserRating: (userId: string) =>
    api.get<ApiResponse<UserRatingResponse>>(
      `/api/v1/reviews/user/${userId}/rating`
    ),

  getBookingReviews: (bookingId: string) =>
    api.get<ApiResponse<ReviewResponse[]>>(
      `/api/v1/reviews/booking/${bookingId}`
    ),
};

// ── Admin ─────────────────────────────────────────────────────────────────────

export const adminApi = {
  getStats: () =>
    api.get<ApiResponse<AdminStatsResponse>>("/api/v1/admin/stats"),

  // Daily activity time-series for the stats dashboard's charts.
  // Tägliche Aktivitäts-Zeitreihe für die Diagramme des Statistik-Dashboards.
  getAnalytics: (days = 30) =>
    api.get<ApiResponse<AdminAnalyticsResponse>>("/api/v1/admin/analytics", {
      params: { days },
    }),

  listUsers: (search?: string, page = 0, size = 20) =>
    api.get<ApiResponse<PageResponse<AdminUserResponse>>>("/api/v1/admin/users", {
      params: { search, page, size },
    }),

  banUser: (id: string) =>
    api.post<ApiResponse<AdminUserResponse>>(`/api/v1/admin/users/${id}/ban`),

  unbanUser: (id: string) =>
    api.post<ApiResponse<AdminUserResponse>>(`/api/v1/admin/users/${id}/unban`),

  suspendUser: (id: string) =>
    api.post<ApiResponse<AdminUserResponse>>(`/api/v1/admin/users/${id}/suspend`),

  unsuspendUser: (id: string) =>
    api.post<ApiResponse<AdminUserResponse>>(`/api/v1/admin/users/${id}/unsuspend`),

  promoteToBusiness: (id: string) =>
    api.patch<ApiResponse<AdminUserResponse>>(`/api/v1/admin/users/${id}/promote-to-business`),

  promoteToAdmin: (id: string) =>
    api.patch<ApiResponse<AdminUserResponse>>(`/api/v1/admin/users/${id}/promote-to-admin`),

  deleteUser: (id: string) =>
    api.delete<ApiResponse<null>>(`/api/v1/admin/users/${id}`),

  // Stage 3 "Business accounts" — admin-granted verification badge.
  verifyBusiness: (id: string) =>
    api.patch<ApiResponse<AdminUserResponse>>(`/api/v1/admin/business/${id}/verify`),

  unverifyBusiness: (id: string) =>
    api.patch<ApiResponse<AdminUserResponse>>(`/api/v1/admin/business/${id}/unverify`),

  // Audit log — paginated, filterable browse over admin/moderation/account events.
  // Audit-Log — paginiertes, filterbares Durchsuchen von Admin-/Moderations-/Kontoereignissen.
  getAuditLog: (params: {
    action?: AuditAction;
    targetType?: string;
    search?: string;
    page?: number;
    size?: number;
  }) =>
    api.get<ApiResponse<PageResponse<AuditLogResponse>>>("/api/v1/admin/audit-log", {
      params: {
        action: params.action,
        targetType: params.targetType,
        search: params.search,
        page: params.page ?? 0,
        size: params.size ?? 20,
      },
    }),
};

// ── Reports — user-filed content moderation reports ────────────────────────────
// Meldungen — von Benutzern eingereichte Inhalts-Meldungen
//
// Note: ReportController has no class-level @RequestMapping — its routes
// (/api/v1/reports, /api/v1/admin/reports/**) live directly on the methods,
// not nested under AdminController. / Hinweis: ReportController hat kein
// klassenweites @RequestMapping — seine Routen liegen direkt auf den
// Methoden, nicht unter AdminController.
export const reportsApi = {
  create: (data: CreateReportRequest) =>
    api.post<ApiResponse<ReportResponse>>("/api/v1/reports", data),

  adminList: (params: {
    status?: ReportStatus;
    targetType?: ReportTargetType;
    search?: string;
    page?: number;
    size?: number;
  }) =>
    api.get<ApiResponse<PageResponse<ReportResponse>>>("/api/v1/admin/reports", {
      params: {
        status: params.status,
        targetType: params.targetType,
        search: params.search,
        page: params.page ?? 0,
        size: params.size ?? 20,
      },
    }),

  adminGet: (id: string) =>
    api.get<ApiResponse<ReportResponse>>(`/api/v1/admin/reports/${id}`),

  // /review takes no request body. / /review erwartet keinen Request-Body.
  adminReview: (id: string) =>
    api.post<ApiResponse<ReportResponse>>(`/api/v1/admin/reports/${id}/review`),

  adminResolve: (id: string, resolutionNote?: string) =>
    api.post<ApiResponse<ReportResponse>>(`/api/v1/admin/reports/${id}/resolve`, {
      resolutionNote,
    }),

  adminDismiss: (id: string, resolutionNote?: string) =>
    api.post<ApiResponse<ReportResponse>>(`/api/v1/admin/reports/${id}/dismiss`, {
      resolutionNote,
    }),

  adminWarn: (id: string, resolutionNote?: string) =>
    api.post<ApiResponse<ReportResponse>>(`/api/v1/admin/reports/${id}/warn`, {
      resolutionNote,
    }),

  adminBan: (id: string, resolutionNote?: string) =>
    api.post<ApiResponse<ReportResponse>>(`/api/v1/admin/reports/${id}/ban`, {
      resolutionNote,
    }),
};

// ── Notifications ─────────────────────────────────────────────────────────────

export const notificationsApi = {
  list: (page = 0, size = 20) =>
    api.get<ApiResponse<PageResponse<NotificationResponse>>>("/api/v1/notifications", {
      params: { page, size },
    }),

  getUnreadCount: () =>
    api.get<ApiResponse<UnreadCountResponse>>("/api/v1/notifications/unread-count"),

  markAsRead: (id: string) =>
    api.post<ApiResponse<null>>(`/api/v1/notifications/${id}/read`),

  markAllAsRead: () =>
    api.post<ApiResponse<null>>("/api/v1/notifications/read-all"),
};

// ── Favorites ────────────────────────────────────────────────────────────────

export const favoritesApi = {
  add: (bikeId: string) =>
    api.post<ApiResponse<null>>(`/api/v1/favorites/${bikeId}`),

  remove: (bikeId: string) =>
    api.delete<ApiResponse<null>>(`/api/v1/favorites/${bikeId}`),

  // Public — works for anonymous visitors too (favorited is always false for them).
  // Öffentlich — funktioniert auch für anonyme Besucher (favorited ist für sie immer false).
  getStatus: (bikeId: string) =>
    api.get<ApiResponse<FavoriteStatusResponse>>(`/api/v1/favorites/${bikeId}/status`),

  list: (page = 0, size = 20) =>
    api.get<ApiResponse<PageResponse<BikeResponse>>>("/api/v1/favorites", {
      params: { page, size },
    }),
};

// ── Business (Stage 3 "Business accounts") ──────────────────────────────────

export const businessApi = {
  upgrade: (data: UpgradeToBusinessRequest) =>
    api.post<ApiResponse<UserProfileResponse>>("/api/v1/business/upgrade", data),

  getDashboardSummary: () =>
    api.get<ApiResponse<BusinessDashboardSummaryResponse>>(
      "/api/v1/business/dashboard/summary"
    ),

  getBookingCalendar: (from: string, to: string) =>
    api.get<ApiResponse<BookingResponse[]>>("/api/v1/business/bookings/calendar", {
      params: { from, to },
    }),

  getOverviewExtras: () =>
    api.get<ApiResponse<BusinessOverviewExtrasResponse>>(
      "/api/v1/business/dashboard/overview-extras"
    ),

  // Richer analytics layer — daily booking/revenue chart, popular bikes,
  // average rental duration, views-to-bookings conversion rate. The backend
  // endpoint (BusinessDashboardController.getAnalytics) already existed in
  // full; this call was never added, so the page calling it never existed.
  getAnalytics: (days = 30) =>
    api.get<ApiResponse<BusinessAnalyticsResponse>>(
      "/api/v1/business/dashboard/analytics",
      { params: { days } }
    ),

  bulkCreateBikes: (data: BulkCreateBikeRequest) =>
    api.post<ApiResponse<BikeResponse[]>>("/api/v1/bikes/bulk", data),
};

// ── Accessories (Stage 3 "Business accounts") ───────────────────────────────

export const accessoriesApi = {
  // Public — used on a bike's detail/booking page to offer add-ons.
  // Öffentlich — wird auf der Detail-/Buchungsseite eines Fahrrads genutzt.
  getByOwner: (ownerId: string) =>
    api.get<ApiResponse<AccessoryResponse[]>>(`/api/v1/accessories/owner/${ownerId}`),

  getMine: () =>
    api.get<ApiResponse<AccessoryResponse[]>>("/api/v1/accessories/my"),

  create: (data: CreateAccessoryRequest) =>
    api.post<ApiResponse<AccessoryResponse>>("/api/v1/accessories", data),

  update: (id: string, data: UpdateAccessoryRequest) =>
    api.put<ApiResponse<AccessoryResponse>>(`/api/v1/accessories/${id}`, data),

  delete: (id: string) =>
    api.delete<ApiResponse<null>>(`/api/v1/accessories/${id}`),
};
