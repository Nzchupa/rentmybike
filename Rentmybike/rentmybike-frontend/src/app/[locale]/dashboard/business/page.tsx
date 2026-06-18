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
} from "lucide-react";
import { businessApi } from "@/lib/api";
import { useAuthStore } from "@/store/auth.store";
import { formatPrice, cn } from "@/lib/utils";

interface StatCardProps {
  label: string;
  value: string | number;
  icon: React.ReactNode;
}

function StatCard({ label, value, icon }: StatCardProps) {
  return (
    <div className="card p-5">
      <div className="flex items-center justify-between mb-3">
        <p className="text-sm font-medium text-slate-500">{label}</p>
        <div className="w-9 h-9 rounded-xl bg-brand-50 flex items-center justify-center">
          {icon}
        </div>
      </div>
      <p className="text-3xl font-bold text-slate-900">{value}</p>
    </div>
  );
}

/**
 * Business Dashboard overview — Stage 3 "Business accounts".
 * Business-Dashboard-Übersicht — Stage 3 "Business-Konten".
 */
export default function BusinessDashboardPage() {
  const t = useTranslations("business");
  const locale = useLocale();
  const { user } = useAuthStore();

  const { data: summary, isLoading } = useQuery({
    queryKey: ["business-dashboard-summary"],
    queryFn: () => businessApi.getDashboardSummary(),
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
                ? "bg-emerald-50 text-emerald-700"
                : "bg-amber-50 text-amber-700"
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
            <div key={i} className="card h-28 animate-pulse bg-slate-100" />
          ))}
        </div>
      ) : (
        <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <StatCard
            label={t("summary.totalRevenue")}
            value={formatPrice(summary.totalRevenue)}
            icon={<TrendingUp size={18} className="text-brand-600" />}
          />
          <StatCard
            label={t("summary.activeBikes")}
            value={summary.activeBikes}
            icon={<Bike size={18} className="text-brand-600" />}
          />
          <StatCard
            label={t("summary.totalBookings")}
            value={summary.totalBookings}
            icon={<Calendar size={18} className="text-brand-600" />}
          />
          <StatCard
            label={t("summary.averageRating")}
            value={summary.averageRating ? summary.averageRating.toFixed(1) : "—"}
            icon={<Star size={18} className="text-brand-600" />}
          />
        </div>
      )}

      {/* Quick links */}
      <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Link href={`/${locale}/dashboard/bikes`} className="card p-5 hover:shadow-md transition-shadow flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-slate-100 flex items-center justify-center">
            <Bike size={18} className="text-slate-600" />
          </div>
          <p className="text-sm font-medium text-slate-900">{t("quickLinks.bikes")}</p>
        </Link>

        <Link href={`/${locale}/dashboard/business/bikes/bulk`} className="card p-5 hover:shadow-md transition-shadow flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-slate-100 flex items-center justify-center">
            <Layers size={18} className="text-slate-600" />
          </div>
          <p className="text-sm font-medium text-slate-900">{t("quickLinks.bulkAdd")}</p>
        </Link>

        <Link href={`/${locale}/dashboard/business/accessories`} className="card p-5 hover:shadow-md transition-shadow flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-slate-100 flex items-center justify-center">
            <Package size={18} className="text-slate-600" />
          </div>
          <p className="text-sm font-medium text-slate-900">{t("quickLinks.accessories")}</p>
        </Link>

        <Link href={`/${locale}/dashboard/business/calendar`} className="card p-5 hover:shadow-md transition-shadow flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-slate-100 flex items-center justify-center">
            <CalendarDays size={18} className="text-slate-600" />
          </div>
          <p className="text-sm font-medium text-slate-900">{t("quickLinks.calendar")}</p>
        </Link>
      </div>
    </div>
  );
}
