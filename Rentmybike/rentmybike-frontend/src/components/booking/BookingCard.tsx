"use client";

import Image from "next/image";
import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { MapPin, Calendar } from "lucide-react";
import toast from "react-hot-toast";
import { bookingsApi } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { BookingStatusBadge } from "@/components/ui/Badge";
import { Avatar } from "@/components/ui/Avatar";
import { formatPrice, formatDate } from "@/lib/utils";
import type { BookingResponse } from "@/types";

interface BookingCardProps {
  booking: BookingResponse;
  /** "renter" = show bike info; "owner" = show renter info */
  view: "renter" | "owner";
  onReview?: (booking: BookingResponse) => void;
}

/**
 * Single booking card for the dashboard booking lists.
 * Einzelne Buchungskarte für die Dashboard-Buchungslisten.
 */
export function BookingCard({ booking, view, onReview }: BookingCardProps) {
  const t = useTranslations("booking");
  const locale = useLocale();
  const queryClient = useQueryClient();

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["owner-bookings"] });
    queryClient.invalidateQueries({ queryKey: ["renter-bookings"] });
  };

  const { mutate: accept, isPending: accepting } = useMutation({
    mutationFn: () => bookingsApi.accept(booking.id),
    onSuccess: () => { toast.success("Booking accepted!"); invalidate(); },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutate: reject, isPending: rejecting } = useMutation({
    mutationFn: () => bookingsApi.reject(booking.id),
    onSuccess: () => { toast.success("Booking rejected."); invalidate(); },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutate: cancel, isPending: cancelling } = useMutation({
    mutationFn: () => bookingsApi.cancel(booking.id),
    onSuccess: () => { toast.success("Booking cancelled."); invalidate(); },
    onError: (e: Error) => toast.error(e.message),
  });

  return (
    <div className="card p-5">
      <div className="flex gap-4">
        {/* Bike thumbnail */}
        <Link href={`/${locale}/bikes/${booking.bikeId}`} className="shrink-0">
          <div className="relative w-20 h-20 rounded-xl overflow-hidden bg-slate-100">
            {booking.bikePrimaryPhotoUrl && (
              <Image
                src={booking.bikePrimaryPhotoUrl}
                alt={booking.bikeTitle}
                fill
                className="object-cover"
                sizes="80px"
              />
            )}
          </div>
        </Link>

        {/* Info */}
        <div className="flex-1 min-w-0">
          <div className="flex items-start justify-between gap-2">
            <Link
              href={`/${locale}/bikes/${booking.bikeId}`}
              className="font-semibold text-slate-900 hover:text-brand-600 line-clamp-1"
            >
              {booking.bikeTitle}
            </Link>
            <BookingStatusBadge status={booking.status} />
          </div>

          <div className="flex items-center gap-1 text-sm text-slate-500 mt-0.5">
            <MapPin size={13} />
            <span>{booking.bikeCity}</span>
          </div>

          <div className="flex items-center gap-1 text-sm text-slate-500 mt-0.5">
            <Calendar size={13} />
            <span>
              {formatDate(booking.startDate, locale, "dd MMM")} –{" "}
              {formatDate(booking.endDate, locale, "dd MMM yyyy")}
            </span>
            <span className="text-slate-400">· {booking.rentalDays}d</span>
          </div>

          {/* Participant */}
          {view === "owner" && (
            <div className="flex items-center gap-2 mt-2">
              <Avatar name={booking.renterName} avatarUrl={booking.renterAvatarUrl} size="sm" />
              <span className="text-sm text-slate-600">{booking.renterName}</span>
            </div>
          )}

          <div className="flex items-center justify-between mt-3">
            <span className="font-semibold text-slate-900">
              {formatPrice(booking.totalPrice, locale)}
            </span>

            {/* Actions */}
            <div className="flex gap-2">
              {view === "owner" && booking.status === "PENDING" && (
                <>
                  <Button
                    size="sm"
                    variant="secondary"
                    loading={rejecting}
                    onClick={() => {
                      if (confirm(t("rejectConfirm"))) reject();
                    }}
                  >
                    {t("actions.reject")}
                  </Button>
                  <Button
                    size="sm"
                    loading={accepting}
                    onClick={() => {
                      if (confirm(t("acceptConfirm"))) accept();
                    }}
                  >
                    {t("actions.accept")}
                  </Button>
                </>
              )}

              {booking.cancellable && (
                <Button
                  size="sm"
                  variant="danger"
                  loading={cancelling}
                  onClick={() => {
                    if (confirm(t("cancelConfirm"))) cancel();
                  }}
                >
                  {t("actions.cancel")}
                </Button>
              )}

              {booking.reviewable && onReview && (
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => onReview(booking)}
                >
                  Review
                </Button>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Optional message */}
      {booking.message && (
        <p className="mt-3 text-sm text-slate-500 border-t border-slate-100 pt-3 italic">
          "{booking.message}"
        </p>
      )}
    </div>
  );
}
