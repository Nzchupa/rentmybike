"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { bookingsApi } from "@/lib/api";
import { BookingCard } from "@/components/booking/BookingCard";
import { Button } from "@/components/ui/Button";
import type { BookingStatus } from "@/types";

const STATUS_FILTERS: (BookingStatus | "ALL")[] = [
  "ALL", "PENDING", "ACCEPTED", "COMPLETED", "CANCELLED",
];

/**
 * "As Owner" bookings — incoming requests for the user's bikes.
 * "Als Eigentümer" Buchungen — eingehende Anfragen für die Fahrräder des Benutzers.
 */
export default function OwnerBookingsPage() {
  const t = useTranslations("dashboard.tabs");
  const tb = useTranslations("booking.status");
  const [statusFilter, setStatusFilter] = useState<BookingStatus | undefined>(undefined);

  const { data, isLoading } = useQuery({
    queryKey: ["owner-bookings", statusFilter],
    queryFn: () => bookingsApi.getMyOwnerBookings(statusFilter, 0, 50),
    select: (r) => r.data.data,
  });

  const bookings = data?.content ?? [];

  return (
    <div className="space-y-6">
      <h1 className="section-title">{t("asOwner")}</h1>

      {/* Status filter */}
      <div className="flex flex-wrap gap-2">
        {STATUS_FILTERS.map((s) => (
          <Button
            key={s}
            size="sm"
            variant={
              (s === "ALL" && !statusFilter) || s === statusFilter
                ? "primary"
                : "outline"
            }
            onClick={() => setStatusFilter(s === "ALL" ? undefined : (s as BookingStatus))}
          >
            {s === "ALL" ? "All" : tb(s)}
          </Button>
        ))}
      </div>

      {isLoading ? (
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="card h-32 animate-pulse bg-slate-100" />
          ))}
        </div>
      ) : bookings.length === 0 ? (
        <div className="card p-12 text-center text-slate-500">
          <p>No booking requests yet. / Noch keine Buchungsanfragen.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {bookings.map((b) => (
            <BookingCard key={b.id} booking={b} view="owner" />
          ))}
        </div>
      )}
    </div>
  );
}
