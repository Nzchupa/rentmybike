"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import { useTranslations } from "next-intl";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { CheckCircle, XCircle, MessageSquareWarning, Eye } from "lucide-react";
import toast from "react-hot-toast";
import { bikesApi } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { ApprovalStatusBadge } from "@/components/ui/Badge";
import { formatPrice, optimizedImageUrl } from "@/lib/utils";
import type { ApprovalStatus } from "@/types";

const STATUS_TABS: (ApprovalStatus | "ALL")[] = ["ALL", "PENDING", "APPROVED", "REJECTED", "CHANGES_REQUESTED"];

const STATUS_TAB_KEYS: Record<ApprovalStatus | "ALL", string> = {
  ALL: "filter.all",
  PENDING: "filter.pending",
  APPROVED: "filter.approved",
  REJECTED: "filter.rejected",
  CHANGES_REQUESTED: "filter.changesRequested",
};

// Two inline-form actions share the same reason-input UI (reject and request
// changes), so a single "pending action" tuple replaces what would otherwise
// be two near-duplicate sets of state. / Zwei Inline-Formular-Aktionen
// (Ablehnen und Änderungen anfordern) teilen sich dieselbe
// Begründungs-Eingabe-UI, daher ersetzt ein einzelnes "ausstehende
// Aktion"-Tupel zwei sonst nahezu doppelte State-Sätze.
type PendingAction = { bikeId: string; kind: "reject" | "requestChanges" } | null;

/**
 * Admin bike moderation page.
 * Admin-Fahrrad-Moderationsseite.
 */
export default function AdminBikesPage() {
  const t = useTranslations("admin.bikes");
  const tCommon = useTranslations("common");
  const tc = useTranslations("bikes.categories");
  const [filter, setFilter] = useState<ApprovalStatus | undefined>(undefined);
  const [page, setPage] = useState(0);
  const PAGE_SIZE = 20;
  const [pendingAction, setPendingAction] = useState<PendingAction>(null);
  const [actionReason, setActionReason] = useState("");
  const queryClient = useQueryClient();

  // Reset to page 0 whenever the status filter changes — otherwise switching
  // tabs while on, say, page 3 of "Pending" could land on an out-of-range
  // page for "Rejected" and silently show nothing.
  // Zurück zu Seite 0 bei jedem Filterwechsel — sonst könnte ein Wechsel der
  // Tabs auf Seite 3 von "Ausstehend" bei "Abgelehnt" außerhalb des
  // gültigen Bereichs landen und stillschweigend nichts anzeigen.
  useEffect(() => {
    setPage(0);
  }, [filter]);

  const { data, isLoading } = useQuery({
    queryKey: ["admin-bikes", filter, page],
    queryFn: () => bikesApi.adminList(filter ?? undefined, page, PAGE_SIZE),
    select: (r) => r.data.data,
  });

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: ["admin-bikes"] });

  const { mutate: approve, isPending: approving } = useMutation({
    mutationFn: bikesApi.adminApprove,
    onSuccess: () => { toast.success(t("bikeApproved")); invalidate(); },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutate: reject, isPending: rejecting } = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) =>
      bikesApi.adminReject(id, reason),
    onSuccess: () => {
      toast.success(t("bikeRejected"));
      setPendingAction(null);
      setActionReason("");
      invalidate();
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutate: requestChanges, isPending: requestingChanges } = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) =>
      bikesApi.adminRequestChanges(id, reason),
    onSuccess: () => {
      toast.success(t("changesRequested"));
      setPendingAction(null);
      setActionReason("");
      invalidate();
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const bikes = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  return (
    <div className="space-y-6">
      <h1 className="section-title">{t("title")}</h1>

      {/* Status filter tabs */}
      <div className="flex flex-wrap gap-2">
        {STATUS_TABS.map((s) => (
          <Button
            key={s}
            size="sm"
            variant={
              (s === "ALL" && !filter) || s === filter ? "primary" : "outline"
            }
            onClick={() => setFilter(s === "ALL" ? undefined : (s as ApprovalStatus))}
          >
            {t(STATUS_TAB_KEYS[s])}
          </Button>
        ))}
      </div>

      {/* Bike list */}
      {isLoading ? (
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="card h-28 animate-pulse bg-slate-100" />
          ))}
        </div>
      ) : bikes.length === 0 ? (
        <div className="card p-12 text-center text-slate-500">
          {t("noBikesFound")}
        </div>
      ) : (
        <div className="space-y-3">
          {bikes.map((bike) => (
            <div key={bike.id} className="card p-4">
              <div className="flex gap-4">
                {/* Thumbnail */}
                <div className="w-20 h-20 shrink-0 rounded-xl overflow-hidden bg-slate-100">
                  {bike.photos?.[0] ? (
                    <Image
                      src={optimizedImageUrl(bike.photos[0].url, 160)}
                      alt={bike.title}
                      width={80}
                      height={80}
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center text-slate-400 text-xs">
                      {t("noPhoto")}
                    </div>
                  )}
                </div>

                {/* Info */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <p className="font-semibold text-slate-900 truncate">{bike.title}</p>
                      {bike.model && (
                        <p className="text-xs text-slate-500 truncate">{bike.model}</p>
                      )}
                      <p className="text-sm text-slate-500">
                        {bike.city} · {tc(bike.category)} · {formatPrice(bike.pricePerDay)}{t("perDay")}
                      </p>
                      <p className="text-xs text-slate-400 mt-0.5">
                        {t("ownerLabel", { name: bike.ownerName })}
                      </p>
                      <p className="text-xs text-slate-400 mt-0.5 flex items-center gap-1">
                        <Eye size={12} /> {t("viewsLabel", { count: bike.viewCount })}
                      </p>
                    </div>
                    <ApprovalStatusBadge status={bike.approvalStatus} />
                  </div>

                  {/* Rejection/changes-requested feedback — previously stored
                      but never displayed anywhere on this page, so an admin
                      reviewing a REJECTED or CHANGES_REQUESTED bike had no
                      way to see why without checking the database directly. */}
                  {bike.rejectionReason && (
                    <p className="text-xs text-slate-600 bg-slate-50 border border-slate-200 rounded-lg px-3 py-1.5 mt-2">
                      <span className="font-medium">{t("reasonLabel")}: </span>
                      {bike.rejectionReason}
                    </p>
                  )}

                  {/* Actions */}
                  {(bike.approvalStatus === "PENDING" || bike.approvalStatus === "CHANGES_REQUESTED") && (
                    <div className="flex gap-2 mt-3">
                      <Button
                        size="sm"
                        variant="primary"
                        loading={approving}
                        onClick={() => approve(bike.id)}
                        className="flex items-center gap-1.5"
                      >
                        <CheckCircle size={14} />
                        {t("approve")}
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => {
                          setPendingAction({ bikeId: bike.id, kind: "requestChanges" });
                          setActionReason("");
                        }}
                        className="flex items-center gap-1.5"
                      >
                        <MessageSquareWarning size={14} />
                        {t("requestChanges")}
                      </Button>
                      <Button
                        size="sm"
                        variant="danger"
                        onClick={() => {
                          setPendingAction({ bikeId: bike.id, kind: "reject" });
                          setActionReason("");
                        }}
                        className="flex items-center gap-1.5"
                      >
                        <XCircle size={14} />
                        {t("reject")}
                      </Button>
                    </div>
                  )}

                  {/* Reason inline form — shared between reject and request-changes,
                      the two actions that both need a mandatory free-text reason. */}
                  {pendingAction?.bikeId === bike.id && (
                    <div className="mt-3 flex gap-2 items-center">
                      <input
                        value={actionReason}
                        onChange={(e) => setActionReason(e.target.value)}
                        placeholder={
                          pendingAction.kind === "reject"
                            ? t("rejectReasonPlaceholder")
                            : t("requestChangesReasonPlaceholder")
                        }
                        className="flex-1 h-9 px-3 rounded-xl border border-slate-300 text-sm outline-none focus:ring-2 focus:ring-brand-500"
                      />
                      <Button
                        size="sm"
                        variant={pendingAction.kind === "reject" ? "danger" : "primary"}
                        loading={pendingAction.kind === "reject" ? rejecting : requestingChanges}
                        disabled={!actionReason.trim()}
                        onClick={() =>
                          pendingAction.kind === "reject"
                            ? reject({ id: bike.id, reason: actionReason.trim() })
                            : requestChanges({ id: bike.id, reason: actionReason.trim() })
                        }
                      >
                        {pendingAction.kind === "reject" ? t("confirmReject") : t("confirmRequestChanges")}
                      </Button>
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => setPendingAction(null)}
                      >
                        {tCommon("cancel")}
                      </Button>
                    </div>
                  )}
                </div>
              </div>
            </div>
          ))}

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex justify-center items-center gap-2 pt-2">
              <Button
                variant="outline"
                size="sm"
                disabled={page === 0}
                onClick={() => setPage((p) => p - 1)}
              >
                {tCommon("previous")}
              </Button>
              <span className="flex items-center px-4 text-sm text-slate-600">
                {page + 1} / {totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
              >
                {tCommon("next")}
              </Button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
