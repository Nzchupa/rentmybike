"use client";

import { useState } from "react";
import { useLocale, useTranslations } from "next-intl";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, Download, Loader2 } from "lucide-react";
import toast from "react-hot-toast";
import { contractApi } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { formatDate } from "@/lib/utils";

interface ContractPanelProps {
  bookingId: string;
}

/**
 * Rental contract viewer for a single ACCEPTED+ booking — shows the fully
 * rendered German-law contract text, a per-role "I accept" click-to-accept
 * button, and a PDF download.
 * Mietvertrags-Ansicht für eine einzelne ACCEPTED+ Buchung — zeigt den
 * vollständig gerenderten Vertragstext nach deutschem Recht, eine
 * rollenspezifische "Ich akzeptiere"-Klick-Zustimmungs-Schaltfläche und
 * einen PDF-Download.
 *
 * <p>The legal text itself is always German (see ContractService's Javadoc
 * for why), but the surrounding chrome (buttons, status labels) is
 * translated like the rest of the app.
 * <p>Der Rechtstext selbst ist immer Deutsch (siehe das Javadoc von
 * ContractService für die Begründung), aber die umgebende Oberfläche
 * (Schaltflächen, Statustexte) ist wie der Rest der App übersetzt.
 */
export function ContractPanel({ bookingId }: ContractPanelProps) {
  const t = useTranslations("booking.contract");
  const locale = useLocale();
  const queryClient = useQueryClient();
  const [downloading, setDownloading] = useState(false);

  const { data: contract, isLoading } = useQuery({
    queryKey: ["contract", bookingId],
    queryFn: () => contractApi.get(bookingId),
    select: (r) => r.data.data,
  });

  const { mutate: accept, isPending: accepting } = useMutation({
    mutationFn: () => contractApi.accept(bookingId),
    onSuccess: () => {
      toast.success(t("acceptedToast"));
      queryClient.invalidateQueries({ queryKey: ["contract", bookingId] });
    },
    onError: (e: Error) => toast.error(e.message),
  });

  async function handleDownload() {
    setDownloading(true);
    try {
      const res = await contractApi.downloadPdf(bookingId);
      const blob = new Blob([res.data], { type: "application/pdf" });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `mietvertrag-${bookingId}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch {
      toast.error(t("downloadFailed"));
    } finally {
      setDownloading(false);
    }
  }

  if (isLoading) {
    return <div className="h-16 animate-pulse bg-slate-100 rounded-xl" />;
  }

  if (!contract) return null;

  return (
    <div className="space-y-4">
      {/* Acceptance status */}
      <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl bg-slate-50 px-3 py-2.5">
        <div className="text-sm">
          {contract.fullyAccepted ? (
            <span className="inline-flex items-center gap-1.5 text-emerald-700 font-medium">
              <CheckCircle2 size={15} />
              {t("bothAccepted")}
            </span>
          ) : contract.acceptedByMe ? (
            <span className="text-slate-600">{t("waitingForOther")}</span>
          ) : (
            <span className="text-slate-600">{t("notYetAccepted")}</span>
          )}
        </div>

        <div className="flex gap-2">
          {!contract.acceptedByMe && (
            <Button
              size="sm"
              loading={accepting}
              onClick={() => {
                if (confirm(t("acceptConfirm"))) accept();
              }}
            >
              {t("accept")}
            </Button>
          )}
          <Button
            size="sm"
            variant="outline"
            disabled={downloading}
            onClick={handleDownload}
            className="inline-flex items-center gap-1.5"
          >
            {downloading ? <Loader2 size={14} className="animate-spin" /> : <Download size={14} />}
            {t("download")}
          </Button>
        </div>
      </div>

      {/* Payment method */}
      <p className="text-sm text-slate-600">
        <span className="font-medium text-slate-700">{t("paymentMethodLabel")}: </span>
        {t(`paymentMethod.${contract.paymentMethod}`)}
      </p>

      {/* Full legal text */}
      <div className="space-y-3 max-h-96 overflow-y-auto rounded-xl border border-slate-100 p-4">
        {contract.sections.map((section) => (
          <div key={section.title}>
            <h4 className="text-sm font-semibold text-slate-800">{section.title}</h4>
            <p className="text-sm text-slate-600 mt-0.5 leading-relaxed">{section.body}</p>
          </div>
        ))}
      </div>

      {/* Signature record */}
      <div className="grid sm:grid-cols-2 gap-2 text-xs text-slate-500">
        <p>
          {t("ownerAccepted")}:{" "}
          {contract.ownerAcceptedAt
            ? formatDate(contract.ownerAcceptedAt, locale, "dd MMM yyyy HH:mm")
            : t("pending")}
        </p>
        <p>
          {t("renterAccepted")}:{" "}
          {contract.renterAcceptedAt
            ? formatDate(contract.renterAcceptedAt, locale, "dd MMM yyyy HH:mm")
            : t("pending")}
        </p>
      </div>
    </div>
  );
}
