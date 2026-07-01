"use client";

import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { Bike, CalendarCheck, CalendarSearch } from "lucide-react";
import { bikesApi, bookingsApi } from "@/lib/api";
import { useAuthStore } from "@/store/auth.store";
import { Button } from "@/components/ui/Button";
import { Avatar } from "@/components/ui/Avatar";

/**
 * Dashboard overview page.
 * Dashboard-Übersichtsseite.
 */
export default function DashboardPage() {
  const t = useTranslations("dashboard");
  const locale = useLocale();
  const { user } = useAuthStore();

  const { data: myBikes } = useQuery({
    queryKey: ["my-bikes"],
    queryFn: () => bikesApi.getMyBikes(0, 3),
    select: (r) => r.data.data,
  });

  const { data: renterBookings } = useQuery({
    queryKey: ["my-renter-bookings"],
    queryFn: () => bookingsApi.getMyRenterBookings(0, 3),
    select: (r) => r.data.data,
  });

  const { data: ownerBookings } = useQuery({
    queryKey: ["owner-bookings"],
    queryFn: () => bookingsApi.getMyOwnerBookings("PENDING", 0, 5),
    select: (r) => r.data.data,
  });

  const pendingCount = ownerBookings?.totalElements ?? 0;

  return (
    <div className="space-y-8">
      {/* Welcome */}
      <div className="flex items-center gap-4">
        {user && <Avatar name={user.fullName} avatarUrl={user.avatarUrl} size="lg" />}
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">
            {t("greeting", { name: user?.firstName ?? "" })}
          </h1>
          <p className="text-slate-500 dark:text-slate-400 text-sm">{user?.email}</p>
        </div>
      </div>

      {/* Stat cards */}
      <div className="grid sm:grid-cols-3 gap-4">
        <Link href={`/${locale}/dashboard/bikes`} className="card p-5 hover:shadow-md transition-shadow">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-brand-50 dark:bg-brand-900/30 flex items-center justify-center">
              <Bike size={20} className="text-brand-600 dark:text-brand-400" />
            </div>
            <div>
              <p className="text-2xl font-bold text-slate-900 dark:text-slate-100">
                {myBikes?.totalElements ?? 0}
              </p>
              <p className="text-sm text-slate-500 dark:text-slate-400">{t("bikes.title")}</p>
            </div>
          </div>
        </Link>

        <Link href={`/${locale}/dashboard/bookings/renter`} className="card p-5 hover:shadow-md transition-shadow">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-blue-50 dark:bg-blue-900/30 flex items-center justify-center">
              <CalendarSearch size={20} className="text-blue-600 dark:text-blue-400" />
            </div>
            <div>
              <p className="text-2xl font-bold text-slate-900 dark:text-slate-100">
                {renterBookings?.totalElements ?? 0}
              </p>
              <p className="text-sm text-slate-500 dark:text-slate-400">{t("tabs.asRenter")}</p>
            </div>
          </div>
        </Link>

        <Link href={`/${locale}/dashboard/bookings/owner`} className="card p-5 hover:shadow-md transition-shadow">
          <div className="flex items-center gap-3">
            <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${pendingCount > 0 ? "bg-amber-50 dark:bg-amber-900/30" : "bg-slate-50 dark:bg-slate-700"}`}>
              <CalendarCheck size={20} className={pendingCount > 0 ? "text-amber-600 dark:text-amber-400" : "text-slate-400"} />
            </div>
            <div>
              <p className="text-2xl font-bold text-slate-900 dark:text-slate-100">
                {pendingCount}
              </p>
              <p className="text-sm text-slate-500 dark:text-slate-400">{t("pendingRequests")}</p>
            </div>
          </div>
        </Link>
      </div>

      {/* Quick actions */}
      <div>
        <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100 mb-4">{t("quickActions")}</h2>
        <div className="flex flex-wrap gap-3">
          <Button asChild>
            <Link href={`/${locale}/dashboard/bikes/new`}>
              <Bike size={16} />
              {t("bikes.addBike")}
            </Link>
          </Button>
          <Button variant="secondary" asChild>
            <Link href={`/${locale}/bikes`}>{t("browseBikes")}</Link>
          </Button>
        </div>
      </div>
    </div>
  );
}
