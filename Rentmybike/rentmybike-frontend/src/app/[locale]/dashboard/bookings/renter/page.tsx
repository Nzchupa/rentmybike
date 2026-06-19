"use client";

import { useLocale, useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { CalendarSearch } from "lucide-react";
import { bookingsApi } from "@/lib/api";
import { BookingCard } from "@/components/booking/BookingCard";
import { EmptyState } from "@/components/ui/EmptyState";

/**
 * "As Renter" bookings — all bookings made by the current user.
 * "Als Mieter" Buchungen — alle Buchungen des aktuellen Benutzers.
 */
export default function RenterBookingsPage() {
  const t = useTranslations("dashboard.tabs");
  const tb = useTranslations("dashboard.bookings");
  const tDash = useTranslations("dashboard");
  const locale = useLocale();

  const { data, isLoading } = useQuery({
    queryKey: ["renter-bookings"],
    queryFn: () => bookingsApi.getMyRenterBookings(0, 50),
    select: (r) => r.data.data,
  });

  const bookings = data?.content ?? [];

  return (
    <div className="space-y-6">
      <h1 className="section-title">{t("asRenter")}</h1>

      {isLoading ? (
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="card h-32 animate-pulse bg-slate-100" />
          ))}
        </div>
      ) : bookings.length === 0 ? (
        <EmptyState
          icon={CalendarSearch}
          message={tb("noRenterBookings")}
          action={{ label: tDash("browseBikes"), href: `/${locale}/bikes` }}
        />
      ) : (
        <div className="space-y-3">
          {bookings.map((b) => (
            <BookingCard key={b.id} booking={b} view="renter" />
          ))}
        </div>
      )}
    </div>
  );
}
