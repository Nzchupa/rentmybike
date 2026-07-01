"use client";

import { useState } from "react";
import { useLocale, useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { Users, Bike, Calendar, TrendingUp, Clock, CheckCircle } from "lucide-react";
import { adminApi } from "@/lib/api";
import { formatPrice, formatDate } from "@/lib/utils";
import { TimeSeriesChart } from "@/components/ui/TimeSeriesChart";

const RANGE_OPTIONS = [7, 30, 90] as const;

interface StatCardProps {
  label: string;
  value: string | number;
  icon: React.ReactNode;
  sub?: string;
  highlight?: boolean;
}

function StatCard({ label, value, icon, sub, highlight }: StatCardProps) {
  return (
    <div className={`card p-5 ${highlight ? "border-brand-200 dark:border-brand-800 bg-brand-50 dark:bg-brand-900/20" : ""}`}>
      <div className="flex items-center justify-between mb-3">
        <p className="text-sm font-medium text-slate-500 dark:text-slate-400">{label}</p>
        <div className={`w-9 h-9 rounded-xl flex items-center justify-center ${highlight ? "bg-brand-100 dark:bg-brand-900/40" : "bg-slate-100 dark:bg-slate-700"}`}>
          {icon}
        </div>
      </div>
      <p className={`text-3xl font-bold ${highlight ? "text-brand-700 dark:text-brand-400" : "text-slate-900 dark:text-slate-100"}`}>
        {value}
      </p>
      {sub && <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">{sub}</p>}
    </div>
  );
}

/**
 * Admin stats dashboard.
 * Admin-Statistik-Dashboard.
 */
export default function AdminStatsPage() {
  const t = useTranslations("admin.stats");
  const locale = useLocale();
  const [rangeDays, setRangeDays] = useState<number>(30);

  const { data: stats, isLoading } = useQuery({
    queryKey: ["admin-stats"],
    queryFn: () => adminApi.getStats(),
    select: (r) => r.data.data,
    refetchInterval: 30_000,   // auto-refresh every 30s
  });

  const { data: analytics, isLoading: analyticsLoading } = useQuery({
    queryKey: ["admin-analytics", rangeDays],
    queryFn: () => adminApi.getAnalytics(rangeDays),
    select: (r) => r.data.data,
  });

  const series = analytics?.series ?? [];
  const chartLabels = series.map((p) => formatDate(p.date, locale, "d MMM"));
  const usersData = series.map((p, i) => ({ label: chartLabels[i], value: p.newUsers }));
  const bikesData = series.map((p, i) => ({ label: chartLabels[i], value: p.newBikes }));
  const bookingsData = series.map((p, i) => ({ label: chartLabels[i], value: p.newBookings }));
  const revenueData = series.map((p, i) => ({ label: chartLabels[i], value: p.revenue }));

  if (isLoading || !stats) {
    return (
      <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {Array.from({ length: 9 }).map((_, i) => (
          <div key={i} className="card h-28 animate-pulse bg-slate-100 dark:bg-slate-700" />
        ))}
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <h1 className="section-title">{t("title")}</h1>
        <div className="flex gap-1">
          {RANGE_OPTIONS.map((d) => (
            <button
              key={d}
              onClick={() => setRangeDays(d)}
              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                rangeDays === d
                  ? "bg-slate-900 text-white dark:bg-slate-100 dark:text-slate-900"
                  : "text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700"
              }`}
            >
              {t("rangeDays", { count: d })}
            </button>
          ))}
        </div>
      </div>

      {/* Activity charts — daily new-user/new-bike/new-booking/revenue
          time-series, backed by /api/v1/admin/analytics (built in an earlier
          backend session) but never surfaced on the frontend until now. */}
      {/* Aktivitätsdiagramme — tägliche Zeitreihen für neue Benutzer/Fahrräder/
          Buchungen/Umsatz, gestützt auf /api/v1/admin/analytics (in einer
          früheren Backend-Sitzung erstellt, aber bisher nie im Frontend
          angezeigt). */}
      <section>
        <h2 className="text-sm font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-3">
          {t("sectionActivity")}
        </h2>
        {analyticsLoading ? (
          <div className="grid sm:grid-cols-2 gap-4">
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="card h-48 animate-pulse bg-slate-100 dark:bg-slate-700" />
            ))}
          </div>
        ) : (
          <div className="grid sm:grid-cols-2 gap-4">
            <div className="card p-5">
              <p className="text-sm font-medium text-slate-500 dark:text-slate-400 mb-2">{t("chartNewUsers")}</p>
              <TimeSeriesChart data={usersData} color="#0ea5e9" />
            </div>
            <div className="card p-5">
              <p className="text-sm font-medium text-slate-500 dark:text-slate-400 mb-2">{t("chartNewBikes")}</p>
              <TimeSeriesChart data={bikesData} color="#8b5cf6" />
            </div>
            <div className="card p-5">
              <p className="text-sm font-medium text-slate-500 dark:text-slate-400 mb-2">{t("chartNewBookings")}</p>
              <TimeSeriesChart data={bookingsData} color="#f59e0b" />
            </div>
            <div className="card p-5">
              <p className="text-sm font-medium text-slate-500 dark:text-slate-400 mb-2">{t("chartRevenue")}</p>
              <TimeSeriesChart
                data={revenueData}
                color="#10b981"
                formatValue={(v) => formatPrice(v, locale)}
              />
            </div>
          </div>
        )}
      </section>

      {/* Revenue highlight */}
      <StatCard
        label={t("totalRevenue")}
        value={formatPrice(stats.totalRevenue)}
        icon={<TrendingUp size={18} className="text-brand-600" />}
        sub={t("completedBookingsSub", { count: stats.completedBookings })}
        highlight
      />

      {/* User stats */}
      <section>
        <h2 className="text-sm font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-3">
          {t("sectionUsers")}
        </h2>
        <div className="grid sm:grid-cols-3 gap-4">
          <StatCard
            label={t("totalUsers")}
            value={stats.totalUsers}
            icon={<Users size={18} className="text-slate-600 dark:text-slate-300" />}
          />
          <StatCard
            label={t("bannedUsers")}
            value={stats.bannedUsers}
            icon={<Users size={18} className="text-red-500" />}
            sub={t("lockedAccounts")}
          />
          <StatCard
            label={t("admins")}
            value={stats.totalAdmins}
            icon={<Users size={18} className="text-purple-500" />}
          />
        </div>
      </section>

      {/* Bike stats */}
      <section>
        <h2 className="text-sm font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-3">
          {t("sectionBikes")}
        </h2>
        <div className="grid sm:grid-cols-3 gap-4">
          <StatCard
            label={t("totalBikes")}
            value={stats.totalBikes}
            icon={<Bike size={18} className="text-slate-600 dark:text-slate-300" />}
          />
          <StatCard
            label={t("pendingBikes")}
            value={stats.pendingBikes}
            icon={<Clock size={18} className="text-amber-500" />}
            sub={t("awaitingModeration")}
          />
          <StatCard
            label={t("approvedBikes")}
            value={stats.approvedBikes}
            icon={<CheckCircle size={18} className="text-green-500" />}
            sub={t("liveOnPlatform")}
          />
        </div>
      </section>

      {/* Booking stats */}
      <section>
        <h2 className="text-sm font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-3">
          {t("sectionBookings")}
        </h2>
        <div className="grid sm:grid-cols-3 gap-4">
          <StatCard
            label={t("totalBookings")}
            value={stats.totalBookings}
            icon={<Calendar size={18} className="text-slate-600 dark:text-slate-300" />}
          />
          <StatCard
            label={t("pendingBookings")}
            value={stats.pendingBookings}
            icon={<Clock size={18} className="text-amber-500" />}
          />
          <StatCard
            label={t("completedBookings")}
            value={stats.completedBookings}
            icon={<CheckCircle size={18} className="text-green-500" />}
          />
        </div>
      </section>
    </div>
  );
}
