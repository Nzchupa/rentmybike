"use client";

import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import {
  TrendingUp,
  Bike,
  Calendar,
  Star,
  Layers,
  Package,
  CalendarDays,
  Clock,
  ChevronRight,
  BarChart3,
} from "lucide-react";
import { businessApi } from "@/lib/api";
import { useAuthStore } from "@/store/auth.store";
import { Avatar } from "@/components/ui/Avatar";
import { BookingStatusBadge } from "@/components/ui/Badge";
import { formatPrice, formatDate, cn } from "@/lib/utils";
import type { BookingResponse, ReviewResponse } from "@/types";

interface StatCardProps {
  label: string;
  value: string | number;
  icon: React.ReactNode;
}

function StatCard({ label, value, icon }: StatCardProps) {
  return (
    <div className="card p-5">
      <div className="flex items-center justify-between mb-3">
        <p className="text-sm font-medium text-slate-500 dark:text-slate-400">{label}</p>
        <div className="w-9 h-9 rounded-xl bg-brand-50 dark:bg-brand-900/30 flex items-center justify-center">
          {icon}
        </div>
      </div>
      <p className="text-3xl font-bold text-slate-900 dark:text-slate-100">{value}</p>
    </div>
  );
}

// Compact booking row shared by the "needs attention" and "upcoming" lists —
// just enough to recognize and triage at a glance; full accept/reject/manage
// actions live on the dedicated booking management pages (renter/owner), not
// duplicated here, per the same "link out, don't rebuild the table" approach
// used by the Moderation Center.
// Kompakte Buchungszeile, gemeinsam genutzt von "erfordert Aufmerksamkeit" und
// "anstehend" — nur genug zum schnellen Erkennen; vollständige Aktionen
// (Annehmen/Ablehnen/Verwalten) liegen auf den dedizierten
// Buchungsverwaltungsseiten, nicht hier dupliziert.
function BookingRow({ booking, locale }: { booking: BookingResponse; locale: string }) {
  return (
    <Link
      href={`/${locale}/dashboard/bookings/owner`}
      className="flex items-center gap-3 px-4 py-3 hover:bg-slate-50 dark:hover:bg-slate-700/50"
    >
      <Avatar name={booking.renterName} avatarUrl={booking.renterAvatarUrl} size="sm" />
      <div className="min-w-0 flex-1">
        <p className="text-sm font-medium text-slate-900 dark:text-slate-100 truncate">{booking.bikeTitle}</p>
        <p className="text-xs text-slate-500 dark:text-slate-400 truncate">
          {booking.renterName} · {formatDate(booking.startDate, locale, "d MMM")} – {formatDate(booking.endDate, locale, "d MMM")}
        </p>
      </div>
      <BookingStatusBadge status={booking.status} />
    </Link>
  );
}

function ReviewRow({ review }: { review: ReviewResponse }) {
  return (
    <div className="flex items-start gap-3 px-4 py-3">
      <Avatar name={review.reviewerName} avatarUrl={review.reviewerAvatarUrl} size="sm" />
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-2">
          <p className="text-sm font-medium text-slate-900 dark:text-slate-100 truncate">{review.reviewerName}</p>
          <span className="inline-flex items-center gap-0.5 text-amber-500 text-xs">
            <Star size={12} className="fill-current" /> {review.rating.toFixed(1)}
          </span>
        </div>
        {review.comment && (
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5 line-clamp-2">{review.comment}</p>
        )}
      </div>
    </div>
  );
}

/**
 * Business Dashboard overview — Stage 3 "Business accounts". Extended with
 * the overview-extras lists (pending-action bookings, upcoming bookings,
 * recent reviews) backed by GET /api/v1/business/dashboard/overview-extras —
 * the chart-heavy analytics layer lives on its own dedicated page.
 * Business-Dashboard-Übersicht — Stage 3 "Business-Konten". Erweitert um die
 * Übersichts-Zusatzlisten (Buchungen, die auf Aktion warten, anstehende
 * Buchungen, neueste Bewertungen). Die diagrammschwere Analytik-Ebene lebt
 * auf einer eigenen Seite.
 */
export default function BusinessDashboardPage() {
  const t = useTranslations("business");
  const tOverview = useTranslations("business.overview");
  const locale = useLocale();
  const { user } = useAuthStore();

  const { data: summary, isLoading } = useQuery({
    queryKey: ["business-dashboard-summary"],
    queryFn: () => businessApi.getDashboardSummary(),
    select: (r) => r.data.data,
  });

  const { data: extras, isLoading: extrasLoading } = useQuery({
    queryKey: ["business-overview-extras"],
    queryFn: () => businessApi.getOverviewExtras(),
    select: (r) => r.data.data,
  });

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <h1 className="section-title">{t("title")}</h1>
        {user && (
          <span
            className={cn(
              "inline-flex items-center px-3 py-1 rounded-full text-xs font-medium",
              user.businessVerified
                ? "bg-emerald-50 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400"
                : "bg-amber-50 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400"
            )}
          >
            {user.businessVerified ? t("verifiedBadge") : t("pendingBadge")}
          </span>
        )}
      </div>

      {/* Summary stats */}
      {isLoading || !summary ? (
        <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="card h-28 animate-pulse bg-slate-100 dark:bg-slate-700" />
          ))}
        </div>
      ) : (
        <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <StatCard
            label={t("summary.totalRevenue")}
            value={formatPrice(summary.totalRevenue)}
            icon={<TrendingUp size={18} className="text-brand-600 dark:text-brand-400" />}
          />
          <StatCard
            label={t("summary.activeBikes")}
            value={summary.activeBikes}
            icon={<Bike size={18} className="text-brand-600 dark:text-brand-400" />}
          />
          <StatCard
            label={t("summary.totalBookings")}
            value={summary.totalBookings}
            icon={<Calendar size={18} className="text-brand-600 dark:text-brand-400" />}
          />
          <StatCard
            label={t("summary.averageRating")}
            value={summary.averageRating ? summary.averageRating.toFixed(1) : "—"}
            icon={<Star size={18} className="text-brand-600 dark:text-brand-400" />}
          />
        </div>
      )}

      {/* Quick links */}
      <div className="grid sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
        <Link href={`/${locale}/dashboard/bikes`} className="card p-5 hover:shadow-md transition-shadow flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-slate-100 dark:bg-slate-700 flex items-center justify-center">
            <Bike size={18} className="text-slate-600 dark:text-slate-300" />
          </div>
          <p className="text-sm font-medium text-slate-900 dark:text-slate-100">{t("quickLinks.bikes")}</p>
        </Link>

        <Link href={`/${locale}/dashboard/business/bikes/bulk`} className="card p-5 hover:shadow-md transition-shadow flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-slate-100 dark:bg-slate-700 flex items-center justify-center">
            <Layers size={18} className="text-slate-600 dark:text-slate-300" />
          </div>
          <p className="text-sm font-medium text-slate-900 dark:text-slate-100">{t("quickLinks.bulkAdd")}</p>
        </Link>

        <Link href={`/${locale}/dashboard/business/accessories`} className="card p-5 hover:shadow-md transition-shadow flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-slate-100 dark:bg-slate-700 flex items-center justify-center">
            <Package size={18} className="text-slate-600 dark:text-slate-300" />
          </div>
          <p className="text-sm font-medium text-slate-900 dark:text-slate-100">{t("quickLinks.accessories")}</p>
        </Link>

        <Link href={`/${locale}/dashboard/business/calendar`} className="card p-5 hover:shadow-md transition-shadow flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-slate-100 dark:bg-slate-700 flex items-center justify-center">
            <CalendarDays size={18} className="text-slate-600 dark:text-slate-300" />
          </div>
          <p className="text-sm font-medium text-slate-900 dark:text-slate-100">{t("quickLinks.calendar")}</p>
        </Link>

        <Link href={`/${locale}/dashboard/business/analytics`} className="card p-5 hover:shadow-md transition-shadow flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-slate-100 dark:bg-slate-700 flex items-center justify-center">
            <BarChart3 size={18} className="text-slate-600 dark:text-slate-300" />
          </div>
          <p className="text-sm font-medium text-slate-900 dark:text-slate-100">{t("quickLinks.analytics")}</p>
        </Link>
      </div>

      {/* Overview extras — needs attention / upcoming / recent reviews */}
      <div className="grid lg:grid-cols-3 gap-4">
        <div className="card overflow-hidden">
          <div className="flex items-center gap-2 px-4 py-3 border-b border-slate-100 dark:border-slate-700">
            <Clock size={16} className="text-amber-500" />
            <h2 className="text-sm font-semibold text-slate-900 dark:text-slate-100">{tOverview("needsAttention")}</h2>
          </div>
          {extrasLoading ? (
            <div className="p-4 space-y-2">
              {[1, 2].map((i) => <div key={i} className="h-12 rounded-xl bg-slate-100 dark:bg-slate-700 animate-pulse" />)}
            </div>
          ) : extras && extras.pendingActionBookings.length > 0 ? (
            <div className="divide-y divide-slate-100 dark:divide-slate-700">
              {extras.pendingActionBookings.slice(0, 5).map((b) => (
                <BookingRow key={b.id} booking={b} locale={locale} />
              ))}
            </div>
          ) : (
            <p className="px-4 py-6 text-sm text-slate-400 dark:text-slate-500 text-center">{tOverview("noPendingActions")}</p>
          )}
        </div>

        <div className="card overflow-hidden">
          <div className="flex items-center gap-2 px-4 py-3 border-b border-slate-100 dark:border-slate-700">
            <CalendarDays size={16} className="text-brand-600 dark:text-brand-400" />
            <h2 className="text-sm font-semibold text-slate-900 dark:text-slate-100">{tOverview("upcoming")}</h2>
          </div>
          {extrasLoading ? (
            <div className="p-4 space-y-2">
              {[1, 2].map((i) => <div key={i} className="h-12 rounded-xl bg-slate-100 dark:bg-slate-700 animate-pulse" />)}
            </div>
          ) : extras && extras.upcomingBookings.length > 0 ? (
            <div className="divide-y divide-slate-100 dark:divide-slate-700">
              {extras.upcomingBookings.slice(0, 5).map((b) => (
                <BookingRow key={b.id} booking={b} locale={locale} />
              ))}
            </div>
          ) : (
            <p className="px-4 py-6 text-sm text-slate-400 dark:text-slate-500 text-center">{tOverview("noUpcoming")}</p>
          )}
        </div>

        <div className="card overflow-hidden">
          <div className="flex items-center gap-2 px-4 py-3 border-b border-slate-100 dark:border-slate-700">
            <Star size={16} className="text-amber-500" />
            <h2 className="text-sm font-semibold text-slate-900 dark:text-slate-100">{tOverview("recentReviews")}</h2>
          </div>
          {extrasLoading ? (
            <div className="p-4 space-y-2">
              {[1, 2].map((i) => <div key={i} className="h-12 rounded-xl bg-slate-100 dark:bg-slate-700 animate-pulse" />)}
            </div>
          ) : extras && extras.recentReviews.length > 0 ? (
            <div className="divide-y divide-slate-100 dark:divide-slate-700">
              {extras.recentReviews.slice(0, 5).map((r) => (
                <ReviewRow key={r.id} review={r} />
              ))}
            </div>
          ) : (
            <p className="px-4 py-6 text-sm text-slate-400 dark:text-slate-500 text-center">{tOverview("noRecentReviews")}</p>
          )}
        </div>
      </div>

      <Link
        href={`/${locale}/dashboard/bookings/owner`}
        className="flex items-center justify-center gap-1.5 text-sm font-medium text-brand-600 hover:text-brand-700 dark:text-brand-400 dark:hover:text-brand-300"
      >
        {tOverview("viewAllBookings")}
        <ChevronRight size={16} />
      </Link>
    </div>
  );
}
