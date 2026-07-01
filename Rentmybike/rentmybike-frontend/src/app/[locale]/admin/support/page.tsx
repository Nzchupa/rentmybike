"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { Search, LifeBuoy } from "lucide-react";
import { supportApi } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { SupportTicketStatusBadge } from "@/components/ui/Badge";
import { formatDate } from "@/lib/utils";
import type { SupportTicketStatus, SupportCategory } from "@/types";

const CATEGORIES: SupportCategory[] = ["BOOKING", "PAYMENT", "ACCOUNT", "BIKE_LISTING", "OTHER"];
const STATUSES: SupportTicketStatus[] = ["OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"];

/**
 * Admin support inbox — every ticket across every user, searchable and
 * filterable by status/category. Mirrors AdminReportsPage's list/filter
 * layout; the actual reply/status-change workflow lives on the per-ticket
 * detail page since a thread needs more room than a list row.
 * Admin-Support-Posteingang — jedes Ticket aller Benutzer, durchsuchbar und
 * nach Status/Kategorie filterbar. Folgt dem Listen-/Filter-Layout von
 * AdminReportsPage; der eigentliche Antwort-/Statuswechsel-Workflow liegt
 * auf der Ticket-Detailseite, da ein Thread mehr Platz braucht als eine
 * Listenzeile.
 */
export default function AdminSupportPage() {
  const t = useTranslations("admin.support");
  const tCategory = useTranslations("support.categories");
  const tCommon = useTranslations("common");
  const locale = useLocale();

  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [status, setStatus] = useState<SupportTicketStatus | "">("");
  const [category, setCategory] = useState<SupportCategory | "">("");
  const [page, setPage] = useState(0);
  const PAGE_SIZE = 20;

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
  }, [debouncedSearch, status, category]);

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["admin-support-tickets", debouncedSearch, status, category, page],
    queryFn: () =>
      supportApi.adminList({
        status: status || undefined,
        category: category || undefined,
        search: debouncedSearch || undefined,
        page,
        size: PAGE_SIZE,
      }),
    select: (r) => r.data.data,
  });

  const tickets = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  return (
    <div className="space-y-6">
      <h1 className="section-title">{t("title")}</h1>

      <div className="flex flex-wrap gap-3">
        <div className="relative max-w-sm flex-1 min-w-[220px]">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            value={search}
            onChange={(e) => handleSearchChange(e.target.value)}
            placeholder={t("searchPlaceholder")}
            className="w-full h-10 pl-9 pr-3 rounded-xl border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 dark:placeholder:text-slate-500 text-sm outline-none focus:ring-2 focus:ring-brand-500"
          />
        </div>

        <select
          value={status}
          onChange={(e) => setStatus(e.target.value as SupportTicketStatus | "")}
          className="h-10 px-3 rounded-xl border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 text-sm outline-none focus:ring-2 focus:ring-brand-500"
        >
          <option value="">{t("allStatuses")}</option>
          {STATUSES.map((s) => (
            <option key={s} value={s}>{s}</option>
          ))}
        </select>

        <select
          value={category}
          onChange={(e) => setCategory(e.target.value as SupportCategory | "")}
          className="h-10 px-3 rounded-xl border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 text-sm outline-none focus:ring-2 focus:ring-brand-500"
        >
          <option value="">{t("allCategories")}</option>
          {CATEGORIES.map((c) => (
            <option key={c} value={c}>{tCategory(c)}</option>
          ))}
        </select>
      </div>

      {isLoading ? (
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="card h-20 animate-pulse bg-slate-100 dark:bg-slate-700" />
          ))}
        </div>
      ) : isError ? (
        <div className="card p-10 text-center text-red-600 dark:text-red-400">
          <p>{error instanceof Error ? error.message : t("loadError")}</p>
        </div>
      ) : tickets.length === 0 ? (
        <div className="card p-12 text-center text-slate-500 dark:text-slate-400 flex flex-col items-center gap-2">
          <LifeBuoy size={28} className="text-slate-300 dark:text-slate-600" />
          {t("noTicketsFound")}
        </div>
      ) : (
        <div className="space-y-3">
          {tickets.map((ticket) => (
            <Link
              key={ticket.id}
              href={`/${locale}/admin/support/${ticket.id}`}
              className="card block p-4 hover:bg-slate-50 dark:hover:bg-slate-700/50"
            >
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="text-xs font-semibold uppercase tracking-wide text-slate-400 dark:text-slate-500">
                      {tCategory(ticket.category)}
                    </span>
                    <SupportTicketStatusBadge status={ticket.status} />
                  </div>
                  <p className="font-medium text-slate-900 dark:text-slate-100 mt-1 truncate">{ticket.subject}</p>
                  {ticket.lastMessagePreview && (
                    <p className="text-sm text-slate-600 dark:text-slate-400 mt-0.5 truncate">{ticket.lastMessagePreview}</p>
                  )}
                  <p className="text-xs text-slate-400 dark:text-slate-500 mt-1">
                    {t("filedBy", { name: ticket.userName })} · {formatDate(ticket.createdAt, locale, "dd MMM yyyy HH:mm")}
                  </p>
                </div>
              </div>
            </Link>
          ))}

          {totalPages > 1 && (
            <div className="flex justify-center items-center gap-2 pt-2">
              <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                {tCommon("previous")}
              </Button>
              <span className="flex items-center px-4 text-sm text-slate-600 dark:text-slate-400">
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
