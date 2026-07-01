"use client";

import { useRef } from "react";
import Image from "next/image";
import { useTranslations } from "next-intl";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, Upload, Loader2 } from "lucide-react";
import toast from "react-hot-toast";
import { bookingsApi } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import type { BookingResponse } from "@/types";

interface PaymentPanelProps {
  booking: BookingResponse;
  /** "renter" = can upload a receipt; "owner" = can confirm payment received */
  view: "renter" | "owner";
}

/**
 * Manual PayPal payment confirmation panel — only rendered for PAYPAL
 * bookings (see BookingCard's paymentPanelEnabled gate). The platform never
 * touches the money itself: the renter pays the owner directly via PayPal
 * outside the app, uploads a receipt here as proof, and the owner reviews
 * and confirms before the flow is considered done.
 * Manuelles PayPal-Zahlungsbestätigungs-Panel — wird nur für PAYPAL-Buchungen
 * gerendert (siehe die paymentPanelEnabled-Bedingung in BookingCard). Die
 * Plattform fasst das Geld selbst nie an: Der Mieter zahlt den Eigentümer
 * direkt per PayPal außerhalb der App, lädt hier eine Quittung als Nachweis
 * hoch, und der Eigentümer prüft und bestätigt, bevor der Ablauf als
 * abgeschlossen gilt.
 *
 * <p>Unlike ContractPanel, no separate fetch is needed here — the booking's
 * payment fields are already part of the BookingResponse the card already
 * has, so this component just renders straight from props and invalidates
 * the booking lists on mutation.
 * <p>Anders als ContractPanel wird hier kein separater Abruf benötigt — die
 * Zahlungsfelder der Buchung sind bereits Teil der BookingResponse, die die
 * Karte schon hat, daher rendert diese Komponente direkt aus den Props und
 * invalidiert bei Mutationen die Buchungslisten.
 */
export function PaymentPanel({ booking, view }: PaymentPanelProps) {
  const t = useTranslations("booking.payment");
  const queryClient = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["owner-bookings"] });
    queryClient.invalidateQueries({ queryKey: ["renter-bookings"] });
  };

  const { mutate: uploadReceipt, isPending: uploading } = useMutation({
    mutationFn: (file: File) => bookingsApi.submitPaymentReceipt(booking.id, file),
    onSuccess: () => {
      toast.success(t("receiptUploadedToast"));
      invalidate();
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutate: confirmPayment, isPending: confirming } = useMutation({
    mutationFn: () => bookingsApi.confirmPayment(booking.id),
    onSuccess: () => {
      toast.success(t("confirmedToast"));
      invalidate();
    },
    onError: (e: Error) => toast.error(e.message),
  });

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (file) uploadReceipt(file);
    e.target.value = "";
  }

  const status = booking.paymentStatus;

  return (
    <div className="space-y-3">
      {/* Status line */}
      <div className="rounded-xl bg-slate-50 px-3 py-2.5 text-sm">
        {status === "CONFIRMED" ? (
          <span className="inline-flex items-center gap-1.5 text-emerald-700 font-medium">
            <CheckCircle2 size={15} />
            {t("confirmed")}
          </span>
        ) : status === "RECEIPT_SUBMITTED" ? (
          <span className="text-slate-600">
            {view === "owner" ? t("receiptAwaitingYourConfirmation") : t("receiptAwaitingOwnerConfirmation")}
          </span>
        ) : (
          <span className="text-slate-600">{t("awaitingTransfer")}</span>
        )}
      </div>

      {/* Receipt preview */}
      {booking.paymentReceiptUrl && (
        <div className="relative w-32 h-32 rounded-lg overflow-hidden bg-slate-100">
          <Image src={booking.paymentReceiptUrl} alt={t("receiptAlt")} fill className="object-cover" sizes="128px" />
        </div>
      )}

      {/* Renter: upload/re-upload receipt (until confirmed) */}
      {view === "renter" && status !== "CONFIRMED" && (
        <>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/jpeg,image/jpg,image/png,image/webp"
            className="hidden"
            onChange={handleFileChange}
          />
          <Button
            size="sm"
            variant="outline"
            disabled={uploading}
            onClick={() => fileInputRef.current?.click()}
            className="inline-flex items-center gap-1.5"
          >
            {uploading ? <Loader2 size={14} className="animate-spin" /> : <Upload size={14} />}
            {status === "RECEIPT_SUBMITTED" ? t("reuploadReceipt") : t("uploadReceipt")}
          </Button>
        </>
      )}

      {/* Owner: confirm once a receipt is in */}
      {view === "owner" && status === "RECEIPT_SUBMITTED" && (
        <Button
          size="sm"
          loading={confirming}
          onClick={() => {
            if (confirm(t("confirmConfirm"))) confirmPayment();
          }}
        >
          {t("confirmReceived")}
        </Button>
      )}
    </div>
  );
}
