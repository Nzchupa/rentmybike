"use client";

import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { Users, Bike, Calendar, TrendingUp, Clock, CheckCircle } from "lucide-react";
import { adminApi } from "@/lib/api";
import { formatPrice } from "@/lib/utils";

interface StatCardProps {
  label: string;
  value: string | number;
  icon: React.ReactNode;
  sub?: string;
  highlight?: boolean;
}

function StatCard({ label, value, icon, sub, highlight }: StatCardProps) {
  return (
    <div className={`card p-5 ${highlight ? "border-brand-200 bg-brand-50" : ""}`}>
      <div className="flex items-center justify-between mb-3">
        <p className="text-sm font-medium text-slate-500">{label}</p>
        <div className={`w-9 h-9 rounded-xl flex items-center justify-center ${highlight ? "bg-brand-100" : "bg-slate-100"}`}>
          {icon}
        </div>
      </div>
      <p className={`text-3xl font-bold ${highlight ? "text-brand-700" : "text-slate-900"}`}>
        {value}
      </p>
      {sub && <p className="text-xs text-slate-500 mt-1">{sub}</p>}
    </div>
  );
}

/**
 * Admin stats dashboard.
 * Admin-Statistik-Dashboard.
 */
export default function AdminStatsPage() {
  const t = useTranslations("admin.stats");

  const { data: stats, isLoading } = useQuery({
    queryKey: ["admin-stats"],
    queryFn: () => adminApi.getStats(),
    select: (r) => r.data.data,
    refetchInterval: 30_000,   // auto-refresh every 30s
  });

  if (isLoading || !stats) {
    return (
      <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {Array.from({ length: 9 }).map((_, i) => (
          <div key={i} className="card h-28 animate-pulse bg-slate-100" />
        ))}
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <h1 className="section-title">{t("title")}</h1>

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
        <h2 className="text-sm font-semibold text-slate-500 uppercase tracking-wider mb-3">
          {t("sectionUsers")}
        </h2>
        <div className="grid sm:grid-cols-3 gap-4">
          <StatCard
            label={t("totalUsers")}
            value={stats.totalUsers}
            icon={<Users size={18} className="text-slate-600" />}
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
        <h2 className="text-sm font-semibold text-slate-500 uppercase tracking-wider mb-3">
          {t("sectionBikes")}
        </h2>
        <div className="grid sm:grid-cols-3 gap-4">
          <StatCard
            label={t("totalBikes")}
            value={stats.totalBikes}
            icon={<Bike size={18} className="text-slate-600" />}
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
        <h2 className="text-sm font-semibold text-slate-500 uppercase tracking-wider mb-3">
          {t("sectionBookings")}
        </h2>
        <div className="grid sm:grid-cols-3 gap-4">
          <StatCard
            label={t("totalBookings")}
            value={stats.totalBookings}
            icon={<Calendar size={18} className="text-slate-600" />}
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
