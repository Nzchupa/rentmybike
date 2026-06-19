"use client";

import { useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { MapPin, Calendar, Camera, MessageCircle, ChevronDown, ChevronUp } from "lucide-react";
import toast from "react-hot-toast";
import { bookingsApi } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { BookingStatusBadge } from "@/components/ui/Badge";
import { Avatar } from "@/components/ui/Avatar";
import { BookingPhotosPanel } from "@/components/booking/BookingPhotosPanel";
import { ChatPanel } from "@/components/booking/ChatPanel";
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
  const tReviews = useTranslations("reviews");
  const locale = useLocale();
  const queryClient = useQueryClient();
  const [showPhotos, setShowPhotos] = useState(false);
  const [showChat, setShowChat] = useState(false);

  // Condition photos only make sense once a booking is confirmed — there's
  // nothing to document before that, and the booking record disappears from
  // active use after rejection/cancellation.
  // Zustandsfotos sind erst sinnvoll, sobald eine Buchung bestätigt ist — vorher
  // gibt es nichts zu dokumentieren, und der Buchungsdatensatz wird nach
  // Ablehnung/Stornierung nicht mehr aktiv genutzt.
  const photosEnabled = booking.status === "ACCEPTED" || booking.status === "COMPLETED";

  // Chat opens earlier than photos — renters may want to ask the owner
  // something before the request is even accepted — but closes once the
  // booking is dead (rejected/cancelled), since there's nothing left to
  // coordinate.
  // Der Chat öffnet früher als die Fotos — Mieter möchten dem Eigentümer
  // vielleicht etwas fragen, bevor die Anfrage überhaupt akzeptiert wurde —
  // schließt aber, sobald die Buchung erledigt ist (abgelehnt/storniert), da
  // es nichts mehr zu koordinieren gibt.
  const chatEnabled = booking.status !== "REJECTED" && booking.status !== "CANCELLED";

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["owner-bookings"] });
    queryClient.invalidateQueries({ queryKey: ["renter-bookings"] });
  };

  const { mutate: accept, isPending: accepting } = useMutation({
    mutationFn: () => bookingsApi.accept(booking.id),
    onSuccess: () => { toast.success(t("acceptedToast")); invalidate(); },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutate: reject, isPending: rejecting } = useMutation({
    mutationFn: () => bookingsApi.reject(booking.id),
    onSuccess: () => { toast.success(t("rejectedToast")); invalidate(); },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutate: cancel, isPending: cancelling } = useMutation({
    mutationFn: () => bookingsApi.cancel(booking.id),
    onSuccess: () => { toast.success(t("cancelledToast")); invalidate(); },
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
                  {tReviews("writeReview")}
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

      {/* Before/after condition photos */}
      {photosEnabled && (
        <div className="mt-3 border-t border-slate-100 pt-3">
          <button
            type="button"
            onClick={() => setShowPhotos((v) => !v)}
            className="inline-flex items-center gap-1.5 text-sm font-medium text-slate-600 hover:text-slate-900"
          >
            <Camera size={15} />
            {t("photos.title")}
            {showPhotos ? <ChevronUp size={15} /> : <ChevronDown size={15} />}
          </button>

          {showPhotos && (
            <div className="mt-3">
              <BookingPhotosPanel bookingId={booking.id} />
            </div>
          )}
        </div>
      )}

      {/* Chat with the other party */}
      {chatEnabled && (
        <div className="mt-3 border-t border-slate-100 pt-3">
          <button
            type="button"
            onClick={() => setShowChat((v) => !v)}
            className="inline-flex items-center gap-1.5 text-sm font-medium text-slate-600 hover:text-slate-900"
          >
            <MessageCircle size={15} />
            {t("chat.title")}
            {showChat ? <ChevronUp size={15} /> : <ChevronDown size={15} />}
          </button>

          {showChat && (
            <div className="mt-3">
              <ChatPanel bookingId={booking.id} />
            </div>
          )}
        </div>
      )}
    </div>
  );
}
