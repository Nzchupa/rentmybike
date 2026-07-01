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

export type ApprovalStatus = "PENDING" | "APPROVED" | "REJECTED" | "CHANGES_REQUESTED";

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
  /** Set when the owner is a business account verified by admins */
  ownerBusinessVerified?: boolean;
  /** Business display name, present when ownerBusinessVerified is true */
  ownerBusinessName?: string | null;
  title: string;
  description: string;
  /** Optional brand/model, e.g. "Trek FX2 Disc" */
  model: string | null;
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
  /** Public detail-page view count / Anzahl öffentlicher Detailseiten-Aufrufe */
  viewCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateBikeRequest {
  title: string;
  description: string;
  /** Optional brand/model, e.g. "Trek FX2 Disc" */
  model?: string;
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

/** How the renter pays the owner — chosen by the owner at accept time. */
export type PaymentMethod = "CASH" | "PAYPAL" | "CARD_ON_SITE";

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
  /** Set once the owner accepts — null while PENDING / Gesetzt nach Annahme durch den Eigentümer — null solange PENDING */
  paymentMethod: PaymentMethod | null;
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
// Rental contract / Mietvertrag
// ─────────────────────────────────────────────────────────────────────────────

export interface ContractSectionResponse {
  title: string;
  body: string;
}

export interface ContractResponse {
  id: string;
  bookingId: string;
  ownerName: string;
  ownerEmail: string;
  renterName: string;
  renterEmail: string;
  bikeTitle: string;
  bikeModel: string | null;
  bikeCategory: string;
  bikeCity: string;
  bikeAddress: string | null;
  startDate: string;
  endDate: string;
  rentalDays: number;
  pricePerDay: number;
  totalPrice: number;
  paymentMethod: PaymentMethod;
  depositAmount: number | null;
  ownerAcceptedAt: string | null;
  renterAcceptedAt: string | null;
  fullyAccepted: boolean;
  /** Whether the current viewer (based on which role they hold) has already clicked accept. */
  acceptedByMe: boolean;
  createdAt: string;
  /** Fully rendered legal text, §1 first / Vollständig gerenderter Rechtstext, §1 zuerst */
  sections: ContractSectionResponse[];
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
  suspendedAt: string | null;
  /** Business display name — null unless role is BUSINESS / Geschäftsname — null außer bei Rolle BUSINESS */
  businessName: string | null;
  /** Whether an admin verified this business / Ob ein Admin dieses Unternehmen verifiziert hat */
  businessVerified: boolean;
  createdAt: string;
  deletedAt: string | null;
  bikeCount: number;
  bookingCount: number;
  /** Best-available "last activity" proxy — see backend AdminUserResponse for caveat. */
  lastActivityAt: string | null;
}

// ─────────────────────────────────────────────────────────────────────────────
// Notifications / Benachrichtigungen
// ─────────────────────────────────────────────────────────────────────────────

export type NotificationType =
  | "NEW_BOOKING_REQUEST"
  | "NEW_CHAT_MESSAGE"
  // Admin-only fan-out types (see NotificationService.notifyAdminsOfNewPendingBike /
  // notifyAdminsOfNewReport on the backend) — every admin gets one row per event.
  // Nur für Admins — Fan-out-Typen, jeder Admin erhält eine Zeile pro Ereignis.
  | "ADMIN_NEW_PENDING_BIKE"
  | "ADMIN_NEW_REPORT"
  | "ADMIN_NEW_SUPPORT_TICKET"
  | "SUPPORT_TICKET_REPLY";

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

// ─────────────────────────────────────────────────────────────────────────────
// Audit log / Audit-Log
// ─────────────────────────────────────────────────────────────────────────────

export type AuditAction =
  | "USER_REGISTERED"
  | "USER_BANNED"
  | "USER_UNBANNED"
  | "USER_SUSPENDED"
  | "USER_UNSUSPENDED"
  | "USER_DELETED"
  | "USER_PROMOTED_TO_BUSINESS"
  | "USER_PROMOTED_TO_ADMIN"
  | "USER_VERIFIED_EMAIL"
  | "BIKE_APPROVED"
  | "BIKE_REJECTED"
  | "BIKE_CHANGES_REQUESTED"
  | "BUSINESS_VERIFIED"
  | "BUSINESS_UNVERIFIED"
  | "BOOKING_CANCELLED"
  | "SUPPORT_TICKET_RESOLVED"
  | "SUPPORT_TICKET_CLOSED";

export interface AuditLogResponse {
  id: string;
  /** Null for system events / Null bei Systemereignissen */
  actorId: string | null;
  /** Denormalized snapshot of the actor's name at event time / Momentaufnahme des Akteursnamens */
  actorName: string | null;
  action: AuditAction;
  /** e.g. "USER" / "BIKE" / "BOOKING" */
  targetType: string;
  targetId: string | null;
  details: string | null;
  createdAt: string;
}

// ─────────────────────────────────────────────────────────────────────────────
// Reports / Meldungen — content moderation reports filed by users
// ─────────────────────────────────────────────────────────────────────────────

export type ReportTargetType = "BIKE" | "USER" | "REVIEW";

export type ReportReason =
  | "SPAM"
  | "INAPPROPRIATE_CONTENT"
  | "FRAUD_OR_SCAM"
  | "HARASSMENT"
  | "FAKE_LISTING"
  | "SAFETY_CONCERN"
  | "OTHER";

export type ReportStatus = "PENDING" | "UNDER_REVIEW" | "RESOLVED" | "DISMISSED";

export interface ReportResponse {
  id: string;
  reporterId: string;
  reporterName: string;
  targetType: ReportTargetType;
  targetId: string;
  /** Resolved bike title / username / review snippet, for display / Aufgelöster Anzeigename des Ziels */
  targetLabel: string | null;
  reason: ReportReason;
  details: string | null;
  status: ReportStatus;
  resolutionNote: string | null;
  resolvedBy: string | null;
  resolvedByName: string | null;
  resolvedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateReportRequest {
  targetType: ReportTargetType;
  targetId: string;
  reason: ReportReason;
  details?: string;
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

/** One day's platform-activity counters, zero-filled for empty days. / Ein Tag an Plattform-Aktivitätszählern, bei Inaktivität nullaufgefüllt. */
export interface AdminTimeSeriesPoint {
  date: string;
  newUsers: number;
  newBikes: number;
  newBookings: number;
  revenue: number;
}

export interface AdminAnalyticsResponse {
  rangeDays: number;
  series: AdminTimeSeriesPoint[];
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

/** GET /api/v1/business/dashboard/overview-extras — see BusinessOverviewExtrasResponse on the backend. */
export interface BusinessOverviewExtrasResponse {
  pendingActionBookings: BookingResponse[];
  upcomingBookings: BookingResponse[];
  recentReviews: ReviewResponse[];
}

/** One day's booking/revenue counters for a business's analytics chart, zero-filled. / Ein Tag an Buchungs-/Umsatzzählern, nullaufgefüllt. */
export interface BusinessTimeSeriesPoint {
  date: string;
  newBookings: number;
  revenue: number;
}

/** One row in the "popular bikes" panel — see PopularBikeResponse on the backend. */
export interface PopularBikeResponse {
  bikeId: string;
  title: string;
  viewCount: number;
  bookingCount: number;
  revenue: number;
}

/** GET /api/v1/business/dashboard/analytics — see BusinessAnalyticsResponse on the backend. This
 * richer layer (daily chart, popular bikes, average rental duration, view-to-booking conversion
 * rate) existed fully on the backend but was never called from the frontend until now — exactly
 * the same "wired up but never used" gap closed elsewhere this session. */
export interface BusinessAnalyticsResponse {
  rangeDays: number;
  series: BusinessTimeSeriesPoint[];
  popularBikes: PopularBikeResponse[];
  averageBookingDurationDays: number;
  conversionRate: number;
}

// ─────────────────────────────────────────────────────────────────────────────
// Support tickets / Support-Tickets — user-facing help desk
// ─────────────────────────────────────────────────────────────────────────────

export type SupportCategory = "BOOKING" | "PAYMENT" | "ACCOUNT" | "BIKE_LISTING" | "OTHER";

export type SupportTicketStatus = "OPEN" | "IN_PROGRESS" | "RESOLVED" | "CLOSED";

export interface SupportMessageResponse {
  id: string;
  senderId: string;
  senderName: string;
  fromAdmin: boolean;
  body: string;
  createdAt: string;
}

export interface SupportTicketResponse {
  id: string;
  userId: string;
  userName: string;
  userEmail: string;
  subject: string;
  category: SupportCategory;
  status: SupportTicketStatus;
  messageCount: number;
  lastMessagePreview: string | null;
  lastMessageAt: string | null;
  /** Full thread — populated only by single-ticket "get" calls, null on list responses. */
  messages: SupportMessageResponse[] | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateSupportTicketRequest {
  subject: string;
  category: SupportCategory;
  message: string;
}

export interface SendSupportMessageRequest {
  body: string;
}

export interface UpdateSupportTicketStatusRequest {
  status: SupportTicketStatus;
}
