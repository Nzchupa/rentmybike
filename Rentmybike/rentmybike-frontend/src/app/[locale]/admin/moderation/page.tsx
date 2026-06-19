"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Flag,
  Bike,
  Eye,
  CheckCircle,
  XCircle,
  AlertTriangle,
  Ban,
} from "lucide-react";
import toast from "react-hot-toast";
import { adminApi, reportsApi } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { ReportStatusBadge } from "@/components/ui/Badge";
import { formatDate } from "@/lib/utils";
import type { ReportStatus } from "@/types";

const STATUS_TABS: ReportStatus[] = ["PENDING", "UNDER_REVIEW"];

type PendingAction = { reportId: string; kind: "resolve" | "dismiss" | "warn" | "ban" } | null;

/**
 * Moderation Center — a single triage inbox combining the two things on the
 * platform that need an admin's attention: pending bike approvals (already
 * manageable in full at /admin/bikes, so this page only links out to it with
 * a live count) and open user-filed content reports (PENDING/UNDER_REVIEW),
 * which get full inline actions here. Deliberately scoped to *open* reports
 * only — a full searchable archive across all statuses belongs to the
 * separate "Reports system UI" task, to avoid building the same table twice.
 *
 * Moderation-Center — ein einziger Triage-Eingang, der die zwei Dinge
 * zusammenfasst, die die Aufmerksamkeit eines Admins benötigen: ausstehende
 * Fahrrad-Genehmigungen (bereits vollständig unter /admin/bikes verwaltbar,
 * daher hier nur ein Link mit Live-Zähler) und offene, von Benutzern
 * eingereichte Inhalts-Meldungen (PENDING/UNDER_REVIEW), die hier vollständige
 * Inline-Aktionen erhalten. Bewusst auf *offene* Meldungen beschränkt — ein
 * vollständig durchsuchbares Archiv über alle Status gehört zur separaten
 * Aufgabe "Reports-System-UI", um nicht dieselbe Tabelle zweimal zu bauen.
 */
export default function ModerationCenterPage() {
  const t = useTranslations("admin.moderation");
  const tReason = useTranslations("admin.moderation.reasons");
  const tCommon = useTranslations("common");
  const locale = useLocale();
  const queryClient = useQueryClient();

  const [statusFilter, setStatusFilter] = useState<ReportStatus>("PENDING");
  const [page, setPage] = useState(0);
  const PAGE_SIZE = 20;
  const [pendingAction, setPendingAction] = useState<PendingAction>(null);
  const [actionNote, setActionNote] = useState("");

  useEffect(() => {
    setPage(0);
  }, [statusFilter]);

  const { data: stats } = useQuery({
    queryKey: ["admin-stats"],
    queryFn: () => adminApi.getStats(),
    select: (r) => r.data.data,
  });

  const { data, isLoading } = useQuery({
    queryKey: ["admin-reports", statusFilter, page],
    queryFn: () => reportsApi.adminList({ status: statusFilter, page, size: PAGE_SIZE }),
    select: (r) => r.data.data,
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["admin-reports"] });

  const clearAction = () => {
    setPendingAction(null);
    setActionNote("");
  };

  const { mutate: review, isPending: reviewing } = useMutation({
    mutationFn: (id: string) => reportsApi.adminReview(id),
    onSuccess: () => { toast.success(t("markedUnderReview")); invalidate(); },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutate: resolve, isPending: resolving } = useMutation({
    mutationFn: ({ id, note }: { id: string; note?: string }) => reportsApi.adminResolve(id, note),
    onSuccess: () => { toast.success(t("reportResolved")); clearAction(); invalidate(); },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutate: dismiss, isPending: dismissing } = useMutation({
    mutationFn: ({ id, note }: { id: string; note?: string }) => reportsApi.adminDismiss(id, note),
    onSuccess: () => { toast.success(t("reportDismissed")); clearAction(); invalidate(); },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutate: warn, isPending: warning } = useMutation({
    mutationFn: ({ id, note }: { id: string; note?: string }) => reportsApi.adminWarn(id, note),
    onSuccess: () => { toast.success(t("userWarned")); clearAction(); invalidate(); },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutate: ban, isPending: banning } = useMutation({
    mutationFn: ({ id, note }: { id: string; note?: string }) => reportsApi.adminBan(id, note),
    onSuccess: () => { toast.success(t("userBanned")); clearAction(); invalidate(); },
    onError: (e: Error) => toast.error(e.message),
  });

  const reports = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  const actionMutations = {
    resolve: { mutate: resolve, pending: resolving, label: t("confirmResolve"), variant: "primary" as const },
    dismiss: { mutate: dismiss, pending: dismissing, label: t("confirmDismiss"), variant: "outline" as const },
    warn:    { mutate: warn,    pending: warning,    label: t("confirmWarn"),    variant: "outline" as const },
    ban:     { mutate: ban,     pending: banning,     label: t("confirmBan"),     variant: "danger" as const },
  };

  return (
    <div className="space-y-6">
      <h1 className="section-title">{t("title")}</h1>

      {/* Summary cards — pending bike approvals links out to the existing
          dedicated page rather than duplicating its approve/reject UI here. */}
      <div className="grid sm:grid-cols-2 gap-4">
        <Link href="/admin/bikes" className="card p-5 flex items-center justify-between hover:border-brand-300 transition-colors">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-amber-100 flex items-center justify-center">
              <Bike size={18} className="text-amber-600" />
            </div>
            <div>
              <p className="text-sm font-medium text-slate-500">{t("pendingBikeApprovals")}</p>
              <p className="text-2xl font-bold text-slate-900">{stats?.pendingBikes ?? "—"}</p>
            </div>
          </div>
          <Eye size={16} className="text-slate-400" />
        </Link>

        <div className="card p-5 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-red-100 flex items-center justify-center">
            <Flag size={18} className="text-red-600" />
          </div>
          <div>
            <p className="text-sm font-medium text-slate-500">{t("openReports")}</p>
            <p className="text-2xl font-bold text-slate-900">
              {data ? data.totalElements : "—"}
            </p>
          </div>
        </div>
      </div>

      {/* Report status tabs */}
      <div className="flex flex-wrap gap-2">
        {STATUS_TABS.map((s) => (
          <Button
            key={s}
            size="sm"
            variant={s === statusFilter ? "primary" : "outline"}
            onClick={() => setStatusFilter(s)}
          >
            {t(`status.${s}`)}
          </Button>
        ))}
      </div>

      {/* Reports list */}
      {isLoading ? (
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="card h-24 animate-pulse bg-slate-100" />
          ))}
        </div>
      ) : reports.length === 0 ? (
        <div className="card p-12 text-center text-slate-500 flex flex-col items-center gap-2">
          <CheckCircle size={28} className="text-green-300" />
          {t("noOpenReports")}
        </div>
      ) : (
        <div className="space-y-3">
          {reports.map((report) => (
            <div key={report.id} className="card p-4">
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="text-xs font-semibold uppercase tracking-wide text-slate-400">
                      {report.targetType}
                    </span>
                    <ReportStatusBadge status={report.status} />
                  </div>
                  <p className="font-medium text-slate-900 mt-1 truncate">
                    {report.targetLabel ?? t("unknownTarget")}
                  </p>
                  <p className="text-sm text-slate-600 mt-0.5">
                    {tReason(report.reason)}
                    {report.details && <span className="text-slate-400"> — {report.details}</span>}
                  </p>
                  <p className="text-xs text-slate-400 mt-1">
                    {t("reportedBy", { name: report.reporterName })} · {formatDate(report.createdAt, locale, "dd MMM yyyy HH:mm")}
                  </p>
                </div>
              </div>

              {/* Quick actions */}
              <div className="flex gap-2 mt-3 flex-wrap">
                {report.status === "PENDING" && (
                  <Button
                    size="sm"
                    variant="outline"
                    loading={reviewing}
                    onClick={() => review(report.id)}
                    className="flex items-center gap-1.5"
                  >
                    <Eye size={14} />
                    {t("markUnderReview")}
                  </Button>
                )}
                <Button
                  size="sm"
                  variant="primary"
                  onClick={() => { setPendingAction({ reportId: report.id, kind: "resolve" }); setActionNote(""); }}
                  className="flex items-center gap-1.5"
                >
                  <CheckCircle size={14} />
                  {t("resolve")}
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => { setPendingAction({ reportId: report.id, kind: "dismiss" }); setActionNote(""); }}
                  className="flex items-center gap-1.5"
                >
                  <XCircle size={14} />
                  {t("dismiss")}
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => { setPendingAction({ reportId: report.id, kind: "warn" }); setActionNote(""); }}
                  className="flex items-center gap-1.5"
                >
                  <AlertTriangle size={14} />
                  {t("warn")}
                </Button>
                <Button
                  size="sm"
                  variant="danger"
                  onClick={() => { setPendingAction({ reportId: report.id, kind: "ban" }); setActionNote(""); }}
                  className="flex items-center gap-1.5"
                >
                  <Ban size={14} />
                  {t("ban")}
                </Button>
              </div>

              {/* Shared resolution-note inline form — all four terminal
                  actions accept the same optional free-text note, so one
                  form covers all of them based on pendingAction.kind. */}
              {pendingAction?.reportId === report.id && (
                <div className="mt-3 flex gap-2 items-center">
                  <input
                    value={actionNote}
                    onChange={(e) => setActionNote(e.target.value)}
                    placeholder={t("resolutionNotePlaceholder")}
                    className="flex-1 h-9 px-3 rounded-xl border border-slate-300 text-sm outline-none focus:ring-2 focus:ring-brand-500"
                  />
                  <Button
                    size="sm"
                    variant={actionMutations[pendingAction.kind].variant}
                    loading={actionMutations[pendingAction.kind].pending}
                    onClick={() =>
                      actionMutations[pendingAction.kind].mutate({
                        id: report.id,
                        note: actionNote.trim() || undefined,
                      })
                    }
                  >
                    {actionMutations[pendingAction.kind].label}
                  </Button>
                  <Button size="sm" variant="ghost" onClick={clearAction}>
                    {tCommon("cancel")}
                  </Button>
                </div>
              )}
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
