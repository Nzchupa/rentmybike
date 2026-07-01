"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { CalendarCheck, AlertCircle } from "lucide-react";
import { bookingsApi } from "@/lib/api";
import { BookingCard } from "@/components/booking/BookingCard";
import { ReviewModal } from "@/components/booking/ReviewModal";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import type { BookingResponse, BookingStatus } from "@/types";

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
  const tBookings = useTranslations("dashboard.bookings");
  const [statusFilter, setStatusFilter] = useState<BookingStatus | undefined>(undefined);
  // Booking currently being reviewed — the owner leaves an OWNER_TO_RENTER
  // review once a booking is reviewable (see BookingResponse.reviewable on
  // the backend). Previously BookingCard's onReview callback was never
  // passed from here, so the "Write a review" button never rendered.
  // Aktuell zu bewertende Buchung — der Eigentümer gibt eine
  // OWNER_TO_RENTER-Bewertung ab, sobald eine Buchung bewertbar ist. Vorher
  // wurde der onReview-Callback von BookingCard hier nie übergeben, sodass
  // die "Bewertung schreiben"-Schaltfläche nie angezeigt wurde.
  const [reviewTarget, setReviewTarget] = useState<BookingResponse | null>(null);

  // Surface isError instead of letting a failed request render identically to
  // "you have no incoming bookings" — see the BookingService.getOwnerBookings
  // fix (readOnly transaction + bulk-update conflict) for the bug this used to
  // mask: before that backend fix, the very first call here reliably threw a
  // 500 and this page just showed the empty state.
  // isError anzeigen, statt eine fehlgeschlagene Anfrage identisch zu "Sie
  // haben keine eingehenden Buchungen" darzustellen — siehe die Korrektur in
  // BookingService.getOwnerBookings (readOnly-Transaktion + Massen-Update-
  // Konflikt) für den Fehler, den dies vorher verschleiert hat: vor dieser
  // Backend-Korrektur warf der allererste Aufruf hier zuverlässig einen 500er,
  // und diese Seite zeigte einfach den Leerzustand.
  const { data, isLoading, isError, error } = useQuery({
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
            {tb(s)}
          </Button>
        ))}
      </div>

      {isLoading ? (
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="card h-32 animate-pulse bg-slate-100 dark:bg-slate-700" />
          ))}
        </div>
      ) : isError ? (
        <EmptyState
          icon={AlertCircle}
          message={error instanceof Error ? error.message : tBookings("loadError")}
          variant="error"
        />
      ) : bookings.length === 0 ? (
        <EmptyState icon={CalendarCheck} message={tBookings("noOwnerBookings")} />
      ) : (
        <div className="space-y-3">
          {bookings.map((b) => (
            <BookingCard
              key={b.id}
              booking={b}
              view="owner"
              onReview={setReviewTarget}
            />
          ))}
        </div>
      )}

      {reviewTarget && (
        <ReviewModal
          booking={reviewTarget}
          type="OWNER_TO_RENTER"
          onClose={() => setReviewTarget(null)}
        />
      )}
    </div>
  );
}
