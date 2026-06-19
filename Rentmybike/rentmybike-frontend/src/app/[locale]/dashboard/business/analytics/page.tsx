"use client";

import { useState } from "react";
import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { ChevronLeft, TrendingUp, Eye, Calendar, Clock, Percent } from "lucide-react";
import { businessApi } from "@/lib/api";
import { formatPrice, formatDate } from "@/lib/utils";
import { TimeSeriesChart } from "@/components/ui/TimeSeriesChart";

const RANGE_OPTIONS = [7, 30, 90] as const;

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
 * Business Analytics page — daily bookings/revenue charts, popular-bikes
 * panel, average rental duration, and view-to-booking conversion rate.
 * Backed by GET /api/v1/business/dashboard/analytics, which existed fully on
 * the backend but was never called from the frontend until now.
 * Business-Analytik-Seite — tägliche Buchungs-/Umsatzdiagramme,
 * "Beliebte Fahrräder"-Panel, durchschnittliche Mietdauer und
 * Ansicht-zu-Buchung-Konversionsrate.
 */
export default function BusinessAnalyticsPage() {
  const t = useTranslations("business.analytics");
  const locale = useLocale();
  const [rangeDays, setRangeDays] = useState<number>(30);

  const { data: analytics, isLoading } = useQuery({
    queryKey: ["business-analytics", rangeDays],
    queryFn: () => businessApi.getAnalytics(rangeDays),
    select: (r) => r.data.data,
  });

  const series = analytics?.series ?? [];
  const chartLabels = series.map((p) => formatDate(p.date, locale, "d MMM"));
  const bookingsData = series.map((p, i) => ({ label: chartLabels[i], value: p.newBookings }));
  const revenueData = series.map((p, i) => ({ label: chartLabels[i], value: p.revenue }));

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <Link
            href={`/${locale}/dashboard/business`}
            className="inline-flex items-center gap-1 text-sm text-slate-500 hover:text-slate-700 mb-1"
          >
            <ChevronLeft size={14} />
            {t("backToOverview")}
          </Link>
          <h1 className="section-title">{t("title")}</h1>
        </div>
        <div className="flex gap-1">
          {RANGE_OPTIONS.map((d) => (
            <button
              key={d}
              onClick={() => setRangeDays(d)}
              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                rangeDays === d
                  ? "bg-slate-900 text-white"
                  : "text-slate-600 hover:bg-slate-100"
              }`}
            >
              {t("rangeDays", { count: d })}
            </button>
          ))}
        </div>
      </div>

      {/* Activity charts */}
      {isLoading ? (
        <div className="grid sm:grid-cols-2 gap-4">
          {[1, 2].map((i) => (
            <div key={i} className="card h-48 animate-pulse bg-slate-100" />
          ))}
        </div>
      ) : (
        <div className="grid sm:grid-cols-2 gap-4">
          <div className="card p-5">
            <p className="text-sm font-medium text-slate-500 mb-2">{t("chartBookings")}</p>
            <TimeSeriesChart data={bookingsData} color="#0ea5e9" />
          </div>
          <div className="card p-5">
            <p className="text-sm font-medium text-slate-500 mb-2">{t("chartRevenue")}</p>
            <TimeSeriesChart
              data={revenueData}
              color="#10b981"
              formatValue={(v) => formatPrice(v, locale)}
            />
          </div>
        </div>
      )}

      {/* Summary stats */}
      {!isLoading && analytics && (
        <div className="grid sm:grid-cols-2 gap-4">
          <StatCard
            label={t("averageDuration")}
            value={t("daysValue", { count: Math.round(analytics.averageBookingDurationDays * 10) / 10 })}
            icon={<Clock size={18} className="text-brand-600" />}
          />
          <StatCard
            label={t("conversionRate")}
            value={`${(analytics.conversionRate * 100).toFixed(1)}%`}
            icon={<Percent size={18} className="text-brand-600" />}
          />
        </div>
      )}

      {/* Popular bikes */}
      <section>
        <h2 className="text-sm font-semibold text-slate-500 uppercase tracking-wider mb-3">
          {t("popularBikes")}
        </h2>
        <div className="card overflow-hidden">
          {isLoading ? (
            <div className="p-4 space-y-2">
              {[1, 2, 3].map((i) => (
                <div key={i} className="h-12 rounded-xl bg-slate-100 animate-pulse" />
              ))}
            </div>
          ) : analytics && analytics.popularBikes.length > 0 ? (
            <div className="divide-y divide-slate-100">
              {analytics.popularBikes.map((bike) => (
                <div key={bike.bikeId} className="flex items-center gap-3 px-4 py-3">
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-slate-900 truncate">{bike.title}</p>
                    <p className="text-xs text-slate-500 flex items-center gap-3 mt-0.5">
                      <span className="inline-flex items-center gap-1">
                        <Eye size={12} /> {bike.viewCount}
                      </span>
                      <span className="inline-flex items-center gap-1">
                        <Calendar size={12} /> {bike.bookingCount}
                      </span>
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="text-sm font-semibold text-slate-900">{formatPrice(bike.revenue, locale)}</p>
                    <p className="text-xs text-slate-400">{t("revenue")}</p>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="px-4 py-6 text-sm text-slate-400 text-center">{t("noPopularBikes")}</p>
          )}
        </div>
      </section>
    </div>
  );
}
