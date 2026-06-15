import axios, { AxiosError } from "axios";

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
// Response interceptor — unwrap data.data and normalize errors
// Response-Interceptor — data.data auspacken und Fehler normalisieren
// ─────────────────────────────────────────────────────────────────────────────

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError<{ message?: string }>) => {
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
