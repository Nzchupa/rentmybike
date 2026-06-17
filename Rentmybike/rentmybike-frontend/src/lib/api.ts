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

type RetryableConfig = AxiosRequestConfig & { _retry?: boolean };

// Endpoints that must never trigger a refresh attempt themselves, otherwise
// a failed login/refresh could recurse into another refresh call.
const AUTH_ENDPOINTS = ["/api/v1/auth/refresh", "/api/v1/auth/login", "/api/v1/auth/register"];

// Shared in-flight refresh promise so concurrent 401s from several requests
// firing at once only trigger a single /auth/refresh call.
let refreshPromise: Promise<void> | null = null;

function redirectToLogin() {
  useAuthStore.getState().logout();
  if (typeof window !== "undefined") {
    const locale = window.location.pathname.split("/")[1] || "en";
    window.location.href = `/${locale}/login`;
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
        await refreshPromise;
        return api(originalRequest);
      } catch {
        redirectToLogin();
        return Promise.reject(new Error("Session expired — please log in again / Sitzung abgelaufen — bitte erneut anmelden"));
      }
    }

    const message =
      error.response?.data?.message ??
      error.message ??
      "Unknown error / Unbekannter Fehler";
    return Promise.reject(new Error(message));
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
  getMe: () =>
    api.get<ApiResponse<UserProfileResponse>>("/api/v1/users/me"),

  updateProfile: (data: UpdateProfileRequest) =>
    api.put<ApiResponse<UserProfileResponse>>("/api/v1/users/me", data),

  uploadAvatar: (file: File) => {
    const form = new FormData();
    form.append("file", file);
    return api.put<ApiResponse<UserProfileResponse>>("/api/v1/users/me/avatar", form, {
      headers: { "Content-Type": "multipart/form-data" },
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
    return api.post<ApiResponse<BikeResponse>>(
      `/api/v1/bikes/${bikeId}/photos`,
      form,
      { headers: { "Content-Type": "multipart/form-data" } }
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

  listUsers: (search?: string, page = 0, size = 20) =>
    api.get<ApiResponse<PageResponse<AdminUserResponse>>>("/api/v1/admin/users", {
      params: { search, page, size },
    }),

  banUser: (id: string) =>
    api.post<ApiResponse<AdminUserResponse>>(`/api/v1/admin/users/${id}/ban`),

  unbanUser: (id: string) =>
    api.post<ApiResponse<AdminUserResponse>>(`/api/v1/admin/users/${id}/unban`),

  deleteUser: (id: string) =>
    api.delete<ApiResponse<null>>(`/api/v1/admin/users/${id}`),
};
