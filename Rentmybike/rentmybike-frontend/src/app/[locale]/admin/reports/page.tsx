"use client";

import { useEffect, useRef, useState } from "react";
import { useLocale, useTranslations } from "next-intl";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Search, Flag, Eye, CheckCircle, XCircle, AlertTriangle, Ban } from "lucide-react";
import toast from "react-hot-toast";
import { reportsApi } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { ReportStatusBadge } from "@/components/ui/Badge";
import { formatDate } from "@/lib/utils";
import type { ReportStatus, ReportTargetType } from "@/types";

const TARGET_TYPES: ReportTargetType[] = ["BIKE", "USER", "REVIEW"];

type PendingAction = { reportId: string; kind: "resolve" | "dismiss" | "warn" | "ban" } | null;

/**
 * Reports system UI — the full, searchable archive across *all* report
 * statuses (PENDING, UNDER_REVIEW, RESOLVED, DISMISSED), complementing the
 * Moderation Center's "open items only" triage inbox. This page is for
 * looking something up after the fact (e.g. "what happened to that report
 * about bike X") as well as acting on open reports found via search/filter —
 * the terminal actions reuse the same PendingAction pattern as the
 * Moderation Center so the two pages don't drift apart.
 *
 * Reports-System-UI — das vollständige, durchsuchbare Archiv über *alle*
 * Meldungsstatus (PENDING, UNDER_REVIEW, RESOLVED, DISMISSED), als Ergänzung
 * zum Moderation-Center, das nur offene Einträge zeigt. Diese Seite dient dem
 * nachträglichen Nachschlagen (z. B. "was ist mit der Meldung zu Fahrrad X
 * passiert") und erlaubt zugleich Aktionen auf offene, per Suche/Filter
 * gefundene Meldungen — die Endaktionen nutzen dasselbe PendingAction-Muster
 * wie das Moderation-Center, damit beide Seiten nicht auseinanderlaufen.
 */
export default function AdminReportsPage() {
  const t = useTranslations("admin.reports");
  const tModeration = useTranslations("admin.moderation");
  const tReason = useTranslations("admin.moderation.reasons");
  const tStatus = useTranslations("admin.moderation.status");
  const tCommon = useTranslations("common");
  const locale = useLocale();
  const queryClient = useQueryClient();

  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [status, setStatus] = useState<ReportStatus | "">("");
  const [targetType, setTargetType] = useState<ReportTargetType | "">("");
  const [page, setPage] = useState(0);
  const PAGE_SIZE = 20;
  const [pendingAction, setPendingAction] = useState<PendingAction>(null);
  const [actionNote, setActionNote] = useState("");

  // Same debounce-leak fix used on the audit log page — scope the timer to
  // this component instance via useRef and clear it on unmount.
  const debounceRef = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  useEffect(() => {
    return () => clearTimeout(debounceRef.current);
  }, []);

  const handleSearchChange = (val: string) => {
    setSearch(val);
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => setDebouncedSearch(val), 300);
  };

  useEffect(() => {
    setPage(0);
  }, [debouncedSearch, status, targetType]);

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["admin-reports-archive", debouncedSearch, status, targetType, page],
    queryFn: () =>
      reportsApi.adminList({
        status: status || undefined,
        targetType: targetType || undefined,
        search: debouncedSearch || undefined,
        page,
        size: PAGE_SIZE,
      }),
    select: (r) => r.data.data,
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["admin-reports-archive"] });

  const clearAction = () => {
    setPendingAction(null);
    setActionNote("");
  };

  const { mutate: review, isPending: reviewing } = useMutation({
    mutationFn: (id: string) => reportsApi.adminReview(id),
    onSuccess: () => { toast.success(tModeration("markedUnderReview")); invalidate(); },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutate: resolve, isPending: resolving } = useMutation({
    mutationFn: ({ id, note }: { id: string; note?: string }) => reportsApi.adminResolve(id, note),
    onSuccess: () => { toast.success(tModeration("reportResolved")); clearAction(); invalidate(); },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutate: dismiss, isPending: dismissing } = useMutation({
    mutationFn: ({ id, note }: { id: string; note?: string }) => reportsApi.adminDismiss(id, note),
    onSuccess: () => { toast.success(tModeration("reportDismissed")); clearAction(); invalidate(); },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutate: warn, isPending: warning } = useMutation({
    mutationFn: ({ id, note }: { id: string; note?: string }) => reportsApi.adminWarn(id, note),
    onSuccess: () => { toast.success(tModeration("userWarned")); clearAction(); invalidate(); },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutate: ban, isPending: banning } = useMutation({
    mutationFn: ({ id, note }: { id: string; note?: string }) => reportsApi.adminBan(id, note),
    onSuccess: () => { toast.success(tModeration("userBanned")); clearAction(); invalidate(); },
    onError: (e: Error) => toast.error(e.message),
  });

  const reports = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  const actionMutations = {
    resolve: { mutate: resolve, pending: resolving, label: tModeration("confirmResolve"), variant: "primary" as const },
    dismiss: { mutate: dismiss, pending: dismissing, label: tModeration("confirmDismiss"), variant: "outline" as const },
    warn:    { mutate: warn,    pending: warning,    label: tModeration("confirmWarn"),    variant: "outline" as const },
    ban:     { mutate: ban,     pending: banning,     label: tModeration("confirmBan"),     variant: "danger" as const },
  };

  // Open reports get full inline actions; resolved/dismissed ones are
  // read-only history — re-acting on them isn't something the backend
  // supports anyway (resolve/dismiss/warn/ban all assume an open report).
  const isOpen = (s: ReportStatus) => s === "PENDING" || s === "UNDER_REVIEW";

  return (
    <div className="space-y-6">
      <h1 className="section-title">{t("title")}</h1>

      {/* Filters */}
      <div className="flex flex-wrap gap-3">
        <div className="relative max-w-sm flex-1 min-w-[220px]">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            value={search}
            onChange={(e) => handleSearchChange(e.target.value)}
            placeholder={t("searchPlaceholder")}
            className="w-full h-10 pl-9 pr-3 rounded-xl border border-slate-300 text-sm outline-none focus:ring-2 focus:ring-brand-500"
          />
        </div>

        <select
          value={status}
          onChange={(e) => setStatus(e.target.value as ReportStatus | "")}
          className="h-10 px-3 rounded-xl border border-slate-300 bg-white text-sm outline-none focus:ring-2 focus:ring-brand-500"
        >
          <option value="">{t("allStatuses")}</option>
          {(["PENDING", "UNDER_REVIEW", "RESOLVED", "DISMISSED"] as ReportStatus[]).map((s) => (
            <option key={s} value={s}>{tStatus(s)}</option>
          ))}
        </select>

        <select
          value={targetType}
          onChange={(e) => setTargetType(e.target.value as ReportTargetType | "")}
          className="h-10 px-3 rounded-xl border border-slate-300 bg-white text-sm outline-none focus:ring-2 focus:ring-brand-500"
        >
          <option value="">{t("allTargets")}</option>
          {TARGET_TYPES.map((tt) => (
            <option key={tt} value={tt}>{tt}</option>
          ))}
        </select>
      </div>

      {/* Reports list */}
      {isLoading ? (
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="card h-24 animate-pulse bg-slate-100" />
          ))}
        </div>
      ) : isError ? (
        <div className="card p-10 text-center text-red-600">
          <p>{error instanceof Error ? error.message : t("loadError")}</p>
        </div>
      ) : reports.length === 0 ? (
        <div className="card p-12 text-center text-slate-500 flex flex-col items-center gap-2">
          <Flag size={28} className="text-slate-300" />
          {t("noReportsFound")}
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
                    {report.targetLabel ?? tModeration("unknownTarget")}
                  </p>
                  <p className="text-sm text-slate-600 mt-0.5">
                    {tReason(report.reason)}
                    {report.details && <span className="text-slate-400"> — {report.details}</span>}
                  </p>
                  <p className="text-xs text-slate-400 mt-1">
                    {tModeration("reportedBy", { name: report.reporterName })} · {formatDate(report.createdAt, locale, "dd MMM yyyy HH:mm")}
                  </p>
                  {report.resolvedByName && (
                    <p className="text-xs text-slate-400 mt-0.5">
                      {t("resolvedBy", { name: report.resolvedByName })}
                      {report.resolvedAt && ` · ${formatDate(report.resolvedAt, locale, "dd MMM yyyy HH:mm")}`}
                      {report.resolutionNote && <span> — {report.resolutionNote}</span>}
                    </p>
                  )}
                </div>
              </div>

              {/* Actions — only available on still-open reports */}
              {isOpen(report.status) && (
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
                      {tModeration("markUnderReview")}
                    </Button>
                  )}
                  <Button
                    size="sm"
                    variant="primary"
                    onClick={() => { setPendingAction({ reportId: report.id, kind: "resolve" }); setActionNote(""); }}
                    className="flex items-center gap-1.5"
                  >
                    <CheckCircle size={14} />
                    {tModeration("resolve")}
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => { setPendingAction({ reportId: report.id, kind: "dismiss" }); setActionNote(""); }}
                    className="flex items-center gap-1.5"
                  >
                    <XCircle size={14} />
                    {tModeration("dismiss")}
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => { setPendingAction({ reportId: report.id, kind: "warn" }); setActionNote(""); }}
                    className="flex items-center gap-1.5"
                  >
                    <AlertTriangle size={14} />
                    {tModeration("warn")}
                  </Button>
                  <Button
                    size="sm"
                    variant="danger"
                    onClick={() => { setPendingAction({ reportId: report.id, kind: "ban" }); setActionNote(""); }}
                    className="flex items-center gap-1.5"
                  >
                    <Ban size={14} />
                    {tModeration("ban")}
                  </Button>
                </div>
              )}

              {pendingAction?.reportId === report.id && (
                <div className="mt-3 flex gap-2 items-center">
                  <input
                    value={actionNote}
                    onChange={(e) => setActionNote(e.target.value)}
                    placeholder={tModeration("resolutionNotePlaceholder")}
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
              <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                {tCommon("previous")}
              </Button>
              <span className="flex items-center px-4 text-sm text-slate-600">
                {page + 1} / {totalPages}
              </span>
              <Button variant="outline" size="sm" disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)}>
                {tCommon("next")}
              </Button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
