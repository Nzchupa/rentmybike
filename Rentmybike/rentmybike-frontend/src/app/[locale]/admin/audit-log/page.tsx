"use client";

import { useEffect, useRef, useState } from "react";
import { useLocale, useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { Search, ScrollText } from "lucide-react";
import { adminApi } from "@/lib/api";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { formatDate } from "@/lib/utils";
import type { AuditAction } from "@/types";

const ACTIONS: AuditAction[] = [
  "USER_REGISTERED",
  "USER_BANNED",
  "USER_UNBANNED",
  "USER_SUSPENDED",
  "USER_UNSUSPENDED",
  "USER_DELETED",
  "USER_PROMOTED_TO_BUSINESS",
  "USER_PROMOTED_TO_ADMIN",
  "USER_VERIFIED_EMAIL",
  "BIKE_APPROVED",
  "BIKE_REJECTED",
  "BIKE_CHANGES_REQUESTED",
  "BUSINESS_VERIFIED",
  "BUSINESS_UNVERIFIED",
  "BOOKING_CANCELLED",
];

const TARGET_TYPES = ["USER", "BIKE", "BOOKING"];

// Badge color groups actions by how "severe" they are — destructive account
// actions read red, positive/granting actions read green, everything else
// gray. Purely visual triage so a long log isn't a wall of identical chips.
// Badge-Farben gruppieren Aktionen nach Schweregrad — destruktive
// Kontoaktionen rot, positive/gewährende Aktionen grün, alles andere grau.
// Rein visuelle Triage, damit ein langes Log nicht aus lauter identischen
// Chips besteht.
const actionVariant: Record<AuditAction, "green" | "yellow" | "red" | "blue" | "gray"> = {
  USER_REGISTERED: "gray",
  USER_BANNED: "red",
  USER_UNBANNED: "green",
  USER_SUSPENDED: "yellow",
  USER_UNSUSPENDED: "green",
  USER_DELETED: "red",
  USER_PROMOTED_TO_BUSINESS: "blue",
  USER_PROMOTED_TO_ADMIN: "blue",
  USER_VERIFIED_EMAIL: "green",
  BIKE_APPROVED: "green",
  BIKE_REJECTED: "red",
  BIKE_CHANGES_REQUESTED: "yellow",
  BUSINESS_VERIFIED: "green",
  BUSINESS_UNVERIFIED: "gray",
  BOOKING_CANCELLED: "yellow",
};

/**
 * Admin audit log page — paginated, filterable browse over admin/moderation/
 * account events. Backend support for this (AuditLogResponse, adminApi.getAuditLog)
 * already existed; this page is the first UI to actually surface it.
 * Admin-Audit-Log-Seite — paginiertes, filterbares Durchsuchen von
 * Admin-/Moderations-/Kontoereignissen. Die Backend-Unterstützung dafür
 * existierte bereits; diese Seite ist die erste UI, die sie tatsächlich anzeigt.
 */
export default function AdminAuditLogPage() {
  const t = useTranslations("admin.auditLog");
  const tAction = useTranslations("admin.auditLog.actions");
  const locale = useLocale();

  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [action, setAction] = useState<AuditAction | "">("");
  const [targetType, setTargetType] = useState<string>("");
  const [page, setPage] = useState(0);
  const PAGE_SIZE = 25;

  // Same debounce-leak fix as the admin users page — scope the timer to this
  // component instance via useRef and clear it on unmount.
  const debounceRef = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  useEffect(() => {
    return () => clearTimeout(debounceRef.current);
  }, []);

  const handleSearchChange = (val: string) => {
    setSearch(val);
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => setDebouncedSearch(val), 300);
  };

  // Any filter change invalidates the current page number — otherwise a
  // narrowed filter could leave `page` pointing past the new last page.
  useEffect(() => {
    setPage(0);
  }, [debouncedSearch, action, targetType]);

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["admin-audit-log", debouncedSearch, action, targetType, page],
    queryFn: () =>
      adminApi.getAuditLog({
        search: debouncedSearch || undefined,
        action: action || undefined,
        targetType: targetType || undefined,
        page,
        size: PAGE_SIZE,
      }),
    select: (r) => r.data.data,
  });

  const entries = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  return (
    <div className="space-y-6">
      <h1 className="section-title">{t("title")}</h1>

      {/* Filters */}
      <div className="flex flex-wrap gap-3">
        <div className="relative max-w-sm flex-1 min-w-[220px]">
          <Search
            size={16}
            className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
          />
          <input
            value={search}
            onChange={(e) => handleSearchChange(e.target.value)}
            placeholder={t("searchPlaceholder")}
            className="w-full h-10 pl-9 pr-3 rounded-xl border border-slate-300 text-sm outline-none focus:ring-2 focus:ring-brand-500"
          />
        </div>

        <select
          value={action}
          onChange={(e) => setAction(e.target.value as AuditAction | "")}
          className="h-10 px-3 rounded-xl border border-slate-300 bg-white text-sm outline-none focus:ring-2 focus:ring-brand-500"
        >
          <option value="">{t("allActions")}</option>
          {ACTIONS.map((a) => (
            <option key={a} value={a}>{tAction(a)}</option>
          ))}
        </select>

        <select
          value={targetType}
          onChange={(e) => setTargetType(e.target.value)}
          className="h-10 px-3 rounded-xl border border-slate-300 bg-white text-sm outline-none focus:ring-2 focus:ring-brand-500"
        >
          <option value="">{t("allTargets")}</option>
          {TARGET_TYPES.map((tt) => (
            <option key={tt} value={tt}>{tt}</option>
          ))}
        </select>
      </div>

      {/* Table */}
      {isLoading ? (
        <div className="space-y-2">
          {[1, 2, 3, 4, 5].map((i) => (
            <div key={i} className="h-12 rounded-xl bg-slate-100 animate-pulse" />
          ))}
        </div>
      ) : isError ? (
        <div className="card p-10 text-center text-red-600">
          <p>{error instanceof Error ? error.message : t("loadError")}</p>
        </div>
      ) : entries.length === 0 ? (
        <div className="card p-12 text-center text-slate-500 flex flex-col items-center gap-2">
          <ScrollText size={28} className="text-slate-300" />
          {t("noEntriesFound")}
        </div>
      ) : (
        <div className="card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-slate-50 border-b border-slate-200">
                <tr>
                  <th className="text-left px-4 py-3 font-medium text-slate-600">{t("time")}</th>
                  <th className="text-left px-4 py-3 font-medium text-slate-600">{t("actor")}</th>
                  <th className="text-left px-4 py-3 font-medium text-slate-600">{t("action")}</th>
                  <th className="text-left px-4 py-3 font-medium text-slate-600">{t("target")}</th>
                  <th className="text-left px-4 py-3 font-medium text-slate-600">{t("details")}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {entries.map((entry) => (
                  <tr key={entry.id} className="hover:bg-slate-50">
                    <td className="px-4 py-3 text-slate-500 whitespace-nowrap">
                      {formatDate(entry.createdAt, locale, "dd MMM yyyy HH:mm")}
                    </td>
                    <td className="px-4 py-3 text-slate-700">
                      {entry.actorName ?? t("systemActor")}
                    </td>
                    <td className="px-4 py-3">
                      <Badge variant={actionVariant[entry.action]}>{tAction(entry.action)}</Badge>
                    </td>
                    <td className="px-4 py-3 text-slate-500">
                      {entry.targetType}
                      {entry.targetId && (
                        <span className="text-slate-400"> · {entry.targetId.slice(0, 8)}</span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-slate-500 max-w-xs truncate" title={entry.details ?? undefined}>
                      {entry.details ?? "—"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex justify-center items-center gap-2 py-4 border-t border-slate-100">
              <Button
                variant="outline"
                size="sm"
                disabled={page === 0}
                onClick={() => setPage((p) => p - 1)}
              >
                {t("previous")}
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
                {t("next")}
              </Button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
