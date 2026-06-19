"use client";

import { useState } from "react";
import { useLocale, useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { CalendarSearch, AlertCircle } from "lucide-react";
import { bookingsApi } from "@/lib/api";
import { BookingCard } from "@/components/booking/BookingCard";
import { ReviewModal } from "@/components/booking/ReviewModal";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import type { BookingResponse, BookingStatus } from "@/types";

const STATUS_FILTERS: (BookingStatus | "ALL")[] = [
  "ALL", "PENDING", "ACCEPTED", "COMPLETED", "CANCELLED", "REJECTED",
];

/**
 * "As Renter" bookings — all bookings made by the current user. Brought to
 * parity with the "As Owner" page (status filter, isError handling) — it
 * previously had neither, even though both pages hit the exact same kind of
 * paginated, filterable booking endpoint. Also wires up ReviewModal so a
 * renter can actually leave a RENTER_TO_OWNER review (see ReviewModal for
 * why that flow never existed before).
 *
 * "Als Mieter" Buchungen — alle Buchungen des aktuellen Benutzers. Auf den
 * gleichen Stand wie "Als Eigentümer" gebracht (Statusfilter,
 * isError-Behandlung) — vorher fehlten beide, obwohl beide Seiten denselben
 * paginierten, filterbaren Buchungs-Endpunkt aufrufen. Bindet außerdem
 * ReviewModal ein, damit ein Mieter tatsächlich eine
 * RENTER_TO_OWNER-Bewertung abgeben kann.
 */
export default function RenterBookingsPage() {
  const t = useTranslations("dashboard.tabs");
  const tb = useTranslations("dashboard.bookings");
  const tStatus = useTranslations("booking.status");
  const tDash = useTranslations("dashboard");
  const locale = useLocale();
  const [statusFilter, setStatusFilter] = useState<BookingStatus | undefined>(undefined);
  const [reviewTarget, setReviewTarget] = useState<BookingResponse | null>(null);

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["renter-bookings", statusFilter],
    queryFn: () => bookingsApi.getMyRenterBookings(0, 50),
    select: (r) => {
      const page = r.data.data;
      // The renter endpoint has no server-side status filter (unlike the
      // owner one), so filtering happens client-side over the fetched page.
      // Der Mieter-Endpunkt hat keinen serverseitigen Statusfilter (im
      // Gegensatz zum Eigentümer-Endpunkt), daher wird clientseitig über die
      // geladene Seite gefiltert.
      if (!statusFilter) return page;
      return { ...page, content: page.content.filter((b) => b.status === statusFilter) };
    },
  });

  const bookings = data?.content ?? [];

  return (
    <div className="space-y-6">
      <h1 className="section-title">{t("asRenter")}</h1>

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
            {tStatus(s)}
          </Button>
        ))}
      </div>

      {isLoading ? (
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="card h-32 animate-pulse bg-slate-100" />
          ))}
        </div>
      ) : isError ? (
        <EmptyState
          icon={AlertCircle}
          message={error instanceof Error ? error.message : tb("loadError")}
          variant="error"
        />
      ) : bookings.length === 0 ? (
        <EmptyState
          icon={CalendarSearch}
          message={tb("noRenterBookings")}
          action={{ label: tDash("browseBikes"), href: `/${locale}/bikes` }}
        />
      ) : (
        <div className="space-y-3">
          {bookings.map((b) => (
            <BookingCard
              key={b.id}
              booking={b}
              view="renter"
              onReview={setReviewTarget}
            />
          ))}
        </div>
      )}

      {reviewTarget && (
        <ReviewModal
          booking={reviewTarget}
          type="RENTER_TO_OWNER"
          onClose={() => setReviewTarget(null)}
        />
      )}
    </div>
  );
}
