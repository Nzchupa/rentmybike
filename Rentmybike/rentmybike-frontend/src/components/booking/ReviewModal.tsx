"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { X } from "lucide-react";
import toast from "react-hot-toast";
import { reviewsApi } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { StarRating } from "@/components/ui/StarRating";
import type { BookingResponse, ReviewType } from "@/types";

interface ReviewModalProps {
  booking: BookingResponse;
  /** Who is being reviewed — the renter leaves a review FOR the owner, and vice versa. */
  type: ReviewType;
  onClose: () => void;
}

/**
 * Review-submission modal opened from a completed booking's "Write a review"
 * button. CreateReviewRequest / POST /api/v1/reviews already existed on the
 * backend and reviewsApi.create() was already wired up in api.ts, but no
 * frontend UI ever called it — BookingCard exposed an `onReview` callback
 * prop that no page actually passed, so the button never rendered anywhere
 * and reviews could never be left through the app.
 *
 * Bewertungs-Modal, das über die "Bewertung schreiben"-Schaltfläche einer
 * abgeschlossenen Buchung geöffnet wird. CreateReviewRequest / POST
 * /api/v1/reviews existierten bereits im Backend und reviewsApi.create()
 * war bereits in api.ts verdrahtet, aber keine Frontend-UI rief es je auf —
 * BookingCard stellte eine `onReview`-Callback-Prop bereit, die keine Seite
 * tatsächlich übergab, sodass die Schaltfläche nie irgendwo angezeigt wurde
 * und über die App nie Bewertungen abgegeben werden konnten.
 */
export function ReviewModal({ booking, type, onClose }: ReviewModalProps) {
  const t = useTranslations("reviews");
  const tCommon = useTranslations("common");
  const queryClient = useQueryClient();
  const [rating, setRating] = useState(0);
  const [comment, setComment] = useState("");

  const { mutate: submit, isPending } = useMutation({
    mutationFn: () =>
      reviewsApi.create({
        bookingId: booking.id,
        type,
        rating,
        comment: comment.trim() || undefined,
      }),
    onSuccess: () => {
      toast.success(t("reviewSubmitted"));
      queryClient.invalidateQueries({ queryKey: ["owner-bookings"] });
      queryClient.invalidateQueries({ queryKey: ["renter-bookings"] });
      onClose();
    },
    onError: (e: Error) => toast.error(e.message),
  });

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4"
      onClick={onClose}
    >
      <div
        className="card w-full max-w-md p-6"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-slate-900">
            {type === "RENTER_TO_OWNER" ? t("renterToOwner") : t("ownerToRenter")}
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="text-slate-400 hover:text-slate-600"
          >
            <X size={20} />
          </button>
        </div>

        <p className="text-sm text-slate-500 mb-4 line-clamp-1">{booking.bikeTitle}</p>

        <div className="mb-4">
          <label className="block text-sm font-medium text-slate-700 mb-2">
            {t("rating")}
          </label>
          <StarRating rating={rating} onChange={setRating} interactive size="md" />
        </div>

        <div className="mb-5">
          <label className="block text-sm font-medium text-slate-700 mb-2">
            {t("comment")}
          </label>
          <textarea
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            placeholder={t("commentPlaceholder")}
            rows={4}
            className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-brand-500 resize-none"
          />
        </div>

        <div className="flex justify-end gap-2">
          <Button variant="ghost" onClick={onClose} disabled={isPending}>
            {tCommon("cancel")}
          </Button>
          <Button
            loading={isPending}
            disabled={rating === 0}
            onClick={() => submit()}
          >
            {t("submitReview")}
          </Button>
        </div>
      </div>
    </div>
  );
}
