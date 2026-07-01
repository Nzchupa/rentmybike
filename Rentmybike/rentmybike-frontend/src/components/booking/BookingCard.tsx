"use client";

import { useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { MapPin, Calendar, Camera, MessageCircle, FileText, Banknote, ChevronDown, ChevronUp } from "lucide-react";
import toast from "react-hot-toast";
import { bookingsApi } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { BookingStatusBadge } from "@/components/ui/Badge";
import { Avatar } from "@/components/ui/Avatar";
import { BookingPhotosPanel } from "@/components/booking/BookingPhotosPanel";
import { ChatPanel } from "@/components/booking/ChatPanel";
import { ContractPanel } from "@/components/booking/ContractPanel";
import { PaymentPanel } from "@/components/booking/PaymentPanel";
import { useAuthStore } from "@/store/auth.store";
import { cn, formatPrice, formatDate, optimizedImageUrl } from "@/lib/utils";
import type { BookingResponse, PaymentMethod } from "@/types";

const PAYMENT_METHODS: PaymentMethod[] = ["CASH", "PAYPAL", "CARD_ON_SITE"];

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
  const isBusiness = useAuthStore((s) => s.isBusiness());
  const [showPhotos, setShowPhotos] = useState(false);
  const [showChat, setShowChat] = useState(false);
  const [showContract, setShowContract] = useState(false);
  const [showPayment, setShowPayment] = useState(false);
  const [showAcceptForm, setShowAcceptForm] = useState(false);
  const [selectedPaymentMethod, setSelectedPaymentMethod] = useState<PaymentMethod>("CASH");

  // The contract only exists once the owner has accepted (that's the moment
  // it's auto-generated server-side) — see ContractService.generateForBooking.
  // Der Vertrag existiert erst, sobald der Eigentümer akzeptiert hat (in
  // diesem Moment wird er serverseitig automatisch erstellt) — siehe
  // ContractService.generateForBooking.
  const contractEnabled = booking.status === "ACCEPTED" || booking.status === "COMPLETED";

  // The manual PayPal confirmation flow only applies to PAYPAL bookings —
  // CASH/CARD_ON_SITE are settled in person and never get a paymentStatus.
  // Der manuelle PayPal-Bestätigungsablauf gilt nur für PAYPAL-Buchungen —
  // CASH/CARD_ON_SITE werden persönlich beglichen und erhalten nie einen
  // paymentStatus.
  const paymentPanelEnabled = contractEnabled && booking.paymentMethod === "PAYPAL";

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

  // Price breakdown (spec item #9) — only worth itemizing once there's
  // something to itemize beyond the bike rental itself.
  // Preisaufschlüsselung — wird nur aufgeschlüsselt, wenn es neben der
  // reinen Fahrradmiete noch etwas zu zeigen gibt.
  const accessoriesTotal = booking.accessories.reduce((sum, a) => sum + a.lineTotal, 0);
  const bikeSubtotal = booking.totalPrice - accessoriesTotal;
  const hasBreakdown = booking.accessories.length > 0;

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["owner-bookings"] });
    queryClient.invalidateQueries({ queryKey: ["renter-bookings"] });
  };

  const { mutate: accept, isPending: accepting } = useMutation({
    mutationFn: (paymentMethod: PaymentMethod) => bookingsApi.accept(booking.id, paymentMethod),
    onSuccess: () => {
      toast.success(t("acceptedToast"));
      setShowAcceptForm(false);
      invalidate();
    },
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
                src={optimizedImageUrl(booking.bikePrimaryPhotoUrl, 160)}
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
            <BookingStatusBadge status={booking.status} size="md" />
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

          {/* Participant — the counterpart on the *other* side of the booking.
              Previously this only rendered for view==="owner" (showing the
              renter), so the renter's own booking list never showed who the
              bike owner was at all. Both views now show the other party. */}
          {/* Gegenpartei — zeigt jetzt in beiden Ansichten die andere Seite
              der Buchung (vorher fehlte der Eigentümername in der
              Mieteransicht komplett). */}
          <div className="flex items-center gap-2 mt-2">
            <Avatar
              name={view === "owner" ? booking.renterName : booking.ownerName}
              avatarUrl={view === "owner" ? booking.renterAvatarUrl : booking.ownerAvatarUrl}
              size="sm"
            />
            <span className="text-sm text-slate-600">
              {t(view === "owner" ? "renterLabel" : "ownerLabel")}{" "}
              {view === "owner" ? booking.renterName : booking.ownerName}
            </span>
          </div>

          {hasBreakdown && (
            <div className="mt-3 rounded-lg bg-slate-50 px-3 py-2 text-sm space-y-1">
              <div className="flex items-center justify-between text-slate-600">
                <span>{t("priceBreakdown.bikeRental", { days: booking.rentalDays })}</span>
                <span>{formatPrice(bikeSubtotal, locale)}</span>
              </div>
              {booking.accessories.map((a) => (
                <div key={a.accessoryId} className="flex items-center justify-between text-slate-600">
                  <span>
                    {a.name} {a.quantity > 1 && `× ${a.quantity}`}
                  </span>
                  <span>{formatPrice(a.lineTotal, locale)}</span>
                </div>
              ))}
              <div className="flex items-center justify-between font-semibold text-slate-900 pt-1 border-t border-slate-200">
                <span>{t("priceBreakdown.total")}</span>
                <span>{formatPrice(booking.totalPrice, locale)}</span>
              </div>
            </div>
          )}

          <div className="flex items-center justify-between mt-3">
            {!hasBreakdown && (
              <span className="font-semibold text-slate-900">
                {formatPrice(booking.totalPrice, locale)}
              </span>
            )}

            {/* Actions */}
            <div className={cn("flex gap-2", hasBreakdown && "ml-auto")}>
              {view === "owner" && booking.status === "PENDING" && !showAcceptForm && (
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
                    onClick={() => setShowAcceptForm(true)}
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

          {/* Payment method selection — required in the same step as
              acceptance, since it's immediately frozen into the
              auto-generated rental contract. */}
          {/* Zahlungsmethoden-Auswahl — im selben Schritt wie die Annahme
              erforderlich, da sie sofort in den automatisch erstellten
              Mietvertrag eingefroren wird. */}
          {showAcceptForm && (
            <div className="mt-3 rounded-xl bg-slate-50 p-3 space-y-2">
              <p className="text-sm font-medium text-slate-700">{t("selectPaymentMethod")}</p>
              <div className="flex flex-wrap gap-2">
                {PAYMENT_METHODS.filter((m) => m !== "CARD_ON_SITE" || isBusiness).map((method) => (
                  <button
                    key={method}
                    type="button"
                    onClick={() => setSelectedPaymentMethod(method)}
                    className={cn(
                      "px-3 py-1.5 rounded-lg text-sm border transition",
                      selectedPaymentMethod === method
                        ? "border-brand-500 bg-brand-50 text-brand-700 font-medium"
                        : "border-slate-200 text-slate-600 hover:border-slate-300"
                    )}
                  >
                    {t(`paymentMethod.${method}`)}
                  </button>
                ))}
              </div>
              <div className="flex gap-2 pt-1">
                <Button
                  size="sm"
                  loading={accepting}
                  onClick={() => {
                    if (confirm(t("acceptConfirm"))) accept(selectedPaymentMethod);
                  }}
                >
                  {t("actions.confirmAccept")}
                </Button>
                <Button size="sm" variant="ghost" onClick={() => setShowAcceptForm(false)}>
                  {t("actions.cancelAccept")}
                </Button>
              </div>
            </div>
          )}
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

      {/* Rental contract — auto-generated the moment the booking is accepted */}
      {/* Mietvertrag — automatisch erstellt, sobald die Buchung akzeptiert wird */}
      {contractEnabled && (
        <div className="mt-3 border-t border-slate-100 pt-3">
          <button
            type="button"
            onClick={() => setShowContract((v) => !v)}
            className="inline-flex items-center gap-1.5 text-sm font-medium text-slate-600 hover:text-slate-900"
          >
            <FileText size={15} />
            {t("contract.title")}
            {showContract ? <ChevronUp size={15} /> : <ChevronDown size={15} />}
          </button>

          {showContract && (
            <div className="mt-3">
              <ContractPanel bookingId={booking.id} />
            </div>
          )}
        </div>
      )}

      {/* Manual PayPal payment confirmation — receipt upload + owner confirm */}
      {/* Manuelle PayPal-Zahlungsbestätigung — Quittungs-Upload + Eigentümer-Bestätigung */}
      {paymentPanelEnabled && (
        <div className="mt-3 border-t border-slate-100 pt-3">
          <button
            type="button"
            onClick={() => setShowPayment((v) => !v)}
            className="inline-flex items-center gap-1.5 text-sm font-medium text-slate-600 hover:text-slate-900"
          >
            <Banknote size={15} />
            {t("payment.title")}
            {showPayment ? <ChevronUp size={15} /> : <ChevronDown size={15} />}
          </button>

          {showPayment && (
            <div className="mt-3">
              <PaymentPanel booking={booking} view={view} />
            </div>
          )}
        </div>
      )}
    </div>
  );
}
