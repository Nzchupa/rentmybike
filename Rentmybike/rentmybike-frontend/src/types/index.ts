// ─────────────────────────────────────────────────────────────────────────────
// Shared / Allgemein
// ─────────────────────────────────────────────────────────────────────────────

/** Backend generic wrapper — matches ApiResponse<T> */
export interface ApiResponse<T> {
  success: boolean;
  message: string | null;
  data: T;
  timestamp: string;
}

/** Paginated result — matches PageResponse<T> */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

// ─────────────────────────────────────────────────────────────────────────────
// User / Benutzer
// ─────────────────────────────────────────────────────────────────────────────

export type UserRole = "USER" | "BUSINESS" | "ADMIN";

export interface UserProfileResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  fullName: string;
  phone: string | null;
  avatarUrl: string | null;
  role: UserRole;
  emailVerified: boolean;
  banned: boolean;
  /** Business display name — null unless role is BUSINESS / Geschäftsname — null außer bei Rolle BUSINESS */
  businessName: string | null;
  /** Whether an admin verified this business / Ob ein Admin dieses Unternehmen verifiziert hat */
  businessVerified: boolean;
  createdAt: string;
}

export interface UpgradeToBusinessRequest {
  businessName: string;
}

export interface PublicUserResponse {
  id: string;
  fullName: string;
  avatarUrl: string | null;
  createdAt: string;
  averageRating: number;
  reviewCount: number;
}

export interface UpdateProfileRequest {
  firstName: string;
  lastName: string;
  phone?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmNewPassword: string;
}

// ─────────────────────────────────────────────────────────────────────────────
// Auth / Authentifizierung
// ─────────────────────────────────────────────────────────────────────────────

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}

/** Shape returned by POST /api/auth/login and POST /api/auth/refresh */
export interface AuthResponse {
  userId: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  phone: string | null;
  role: UserRole;
  avatarUrl: string | null;
  emailVerified: boolean;
  createdAt: string;
  banned: boolean;
  businessName: string | null;
  businessVerified: boolean;
}

// ─────────────────────────────────────────────────────────────────────────────
// Bike / Fahrrad
// ─────────────────────────────────────────────────────────────────────────────

export type BikeCategory =
  | "CITY"
  | "MOUNTAIN"
  | "ROAD"
  | "ELECTRIC"
  | "HYBRID"
  | "CARGO"
  | "KIDS";

export type ApprovalStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface BikePhotoResponse {
  id: string;
  url: string;
  displayOrder: number;
  primary: boolean;
}

export interface BikeResponse {
  id: string;
  ownerId: string;
  ownerName: string;
  ownerAvatarUrl: string | null;
  title: string;
  description: string;
  category: BikeCategory;
  pricePerDay: number;
  city: string;
  address: string | null;
  latitude: number | null;
  longitude: number | null;
  available: boolean;
  approvalStatus: ApprovalStatus;
  rejectionReason: string | null;
  primaryPhotoUrl: string | null;
  photos: BikePhotoResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateBikeRequest {
  title: string;
  description: string;
  category: BikeCategory;
  pricePerDay: number;
  city: string;
  address?: string;
  latitude?: number;
  longitude?: number;
}

export interface UpdateBikeRequest extends CreateBikeRequest {
  available: boolean;
}

/** BUSINESS-only — POST /api/v1/bikes/bulk, up to 50 bikes per batch */
export interface BulkCreateBikeRequest {
  bikes: CreateBikeRequest[];
}

// ─────────────────────────────────────────────────────────────────────────────
// Booking / Buchung
// ─────────────────────────────────────────────────────────────────────────────

export type BookingStatus =
  | "PENDING"
  | "ACCEPTED"
  | "REJECTED"
  | "CANCELLED"
  | "COMPLETED";

export interface BookingResponse {
  id: string;
  bikeId: string;
  bikeTitle: string;
  bikeCity: string;
  bikePrimaryPhotoUrl: string | null;
  renterId: string;
  renterName: string;
  renterAvatarUrl: string | null;
  ownerId: string;
  ownerName: string;
  ownerAvatarUrl: string | null;
  startDate: string;   // ISO date: "2025-06-01"
  endDate: string;
  rentalDays: number;
  totalPrice: number;
  status: BookingStatus;
  message: string | null;
  /** Accessory add-ons (Stage 3 "Business accounts") / Zubehör-Add-ons (Stage 3 "Business-Konten") */
  accessories: BookingAccessoryResponse[];
  cancellable: boolean;
  reviewable: boolean;
  createdAt: string;
}

export interface CreateBookingRequest {
  bikeId: string;
  startDate: string;   // ISO date
  endDate: string;
  message?: string;
  accessories?: AccessorySelectionRequest[];
}

// ─────────────────────────────────────────────────────────────────────────────
// Accessories (Stage 3 "Business accounts") / Zubehör (Stage 3 "Business-Konten")
// ─────────────────────────────────────────────────────────────────────────────

export type AccessoryType = "HELMET" | "CHILD_SEAT" | "LOCK";

export interface AccessoryResponse {
  id: string;
  ownerId: string;
  ownerName: string;
  type: AccessoryType;
  name: string;
  quantityTotal: number;
  pricePerDay: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAccessoryRequest {
  type: AccessoryType;
  name: string;
  quantityTotal: number;
  pricePerDay: number;
}

export interface UpdateAccessoryRequest extends CreateAccessoryRequest {}

export interface AccessorySelectionRequest {
  accessoryId: string;
  quantity: number;
}

export interface BookingAccessoryResponse {
  accessoryId: string;
  type: AccessoryType;
  name: string;
  quantity: number;
  pricePerDayAtBooking: number;
  lineTotal: number;
}

// ─────────────────────────────────────────────────────────────────────────────
// Booking photos (before/after) / Buchungsfotos (Vorher/Nachher)
// ─────────────────────────────────────────────────────────────────────────────

export type BookingPhotoPhase = "BEFORE" | "AFTER";

export interface BookingPhotoResponse {
  id: string;
  phase: BookingPhotoPhase;
  photoUrl: string;
  uploaderId: string;
  uploaderName: string;
  createdAt: string;
}

// ─────────────────────────────────────────────────────────────────────────────
// Chat / Chat
// ─────────────────────────────────────────────────────────────────────────────

export interface ChatMessageResponse {
  id: string;
  bookingId: string;
  senderId: string;
  senderName: string;
  senderAvatarUrl: string | null;
  content: string;
  createdAt: string;
}

// ─────────────────────────────────────────────────────────────────────────────
// Review / Bewertung
// ─────────────────────────────────────────────────────────────────────────────

export type ReviewType = "RENTER_TO_OWNER" | "OWNER_TO_RENTER";

export interface ReviewResponse {
  id: string;
  bookingId: string;
  type: ReviewType;
  reviewerId: string;
  reviewerName: string;
  reviewerAvatarUrl: string | null;
  revieweeId: string;
  revieweeName: string;
  rating: number;
  comment: string | null;
  createdAt: string;
}

export interface CreateReviewRequest {
  bookingId: string;
  type: ReviewType;
  rating: number;
  comment?: string;
}

export interface UserRatingResponse {
  averageRating: number;
  reviewCount: number;
}

// ─────────────────────────────────────────────────────────────────────────────
// Admin / Administration
// ─────────────────────────────────────────────────────────────────────────────

export interface AdminUserResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  avatarUrl: string | null;
  role: UserRole;
  emailVerified: boolean;
  banned: boolean;
  bannedAt: string | null;
  /** Business display name — null unless role is BUSINESS / Geschäftsname — null außer bei Rolle BUSINESS */
  businessName: string | null;
  /** Whether an admin verified this business / Ob ein Admin dieses Unternehmen verifiziert hat */
  businessVerified: boolean;
  createdAt: string;
  deletedAt: string | null;
}

// ─────────────────────────────────────────────────────────────────────────────
// Notifications / Benachrichtigungen
// ─────────────────────────────────────────────────────────────────────────────

export type NotificationType = "NEW_BOOKING_REQUEST" | "NEW_CHAT_MESSAGE";

export interface NotificationResponse {
  id: string;
  type: NotificationType;
  title: string;
  message: string;
  read: boolean;
  createdAt: string;
  bookingId: string | null;
  bikeId: string | null;
  bikeTitle: string | null;
  // Which bookings list ("as owner" vs "as renter") this notification's
  // booking should deep-link to, null if not booking-related. NEW_BOOKING_REQUEST
  // always targets the owner, but NEW_CHAT_MESSAGE can go to either side.
  viewAsOwner: boolean | null;
}

export interface UnreadCountResponse {
  unreadCount: number;
}

// ─────────────────────────────────────────────────────────────────────────────
// Favorites / Favoriten
// ─────────────────────────────────────────────────────────────────────────────

export interface FavoriteStatusResponse {
  favorited: boolean;
  favoriteCount: number;
}

export interface AdminStatsResponse {
  totalUsers: number;
  bannedUsers: number;
  totalAdmins: number;
  totalBikes: number;
  pendingBikes: number;
  approvedBikes: number;
  rejectedBikes: number;
  totalBookings: number;
  pendingBookings: number;
  acceptedBookings: number;
  completedBookings: number;
  cancelledBookings: number;
  rejectedBookings: number;
  totalRevenue: number;
}

// ─────────────────────────────────────────────────────────────────────────────
// Business dashboard (Stage 3 "Business accounts") / Business-Dashboard
// ─────────────────────────────────────────────────────────────────────────────

export interface BusinessDashboardSummaryResponse {
  totalRevenue: number;
  activeBikes: number;
  totalBookings: number;
  averageRating: number;
}
