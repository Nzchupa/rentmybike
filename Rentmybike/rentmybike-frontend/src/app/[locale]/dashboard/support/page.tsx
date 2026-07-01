"use client";

import { useState } from "react";
import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { LifeBuoy, Plus, AlertCircle } from "lucide-react";
import toast from "react-hot-toast";
import { supportApi } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { EmptyState } from "@/components/ui/EmptyState";
import { SupportTicketStatusBadge } from "@/components/ui/Badge";
import { formatDate } from "@/lib/utils";
import type { SupportCategory } from "@/types";

const CATEGORIES: SupportCategory[] = ["BOOKING", "PAYMENT", "ACCOUNT", "BIKE_LISTING", "OTHER"];

/**
 * User-facing support ticket list — replaces "email the single developer"
 * (see the contact page) with a tracked help-desk thread. Opening a new
 * ticket creates the ticket and its first message together in one call.
 * Nutzerseitige Support-Ticket-Liste — löst "E-Mail an den einzelnen
 * Entwickler" (siehe Kontaktseite) durch einen nachverfolgbaren
 * Support-Thread ab. Ein neues Ticket erstellt Ticket und erste Nachricht
 * gemeinsam in einem Aufruf.
 */
export default function SupportPage() {
  const t = useTranslations("dashboard.support");
  const tCategory = useTranslations("support.categories");
  const tCommon = useTranslations("common");
  const locale = useLocale();
  const queryClient = useQueryClient();

  const [showForm, setShowForm] = useState(false);
  const [subject, setSubject] = useState("");
  const [category, setCategory] = useState<SupportCategory>("OTHER");
  const [message, setMessage] = useState("");

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["my-support-tickets"],
    queryFn: () => supportApi.listMine(0, 50),
    select: (r) => r.data.data,
  });

  const { mutate: createTicket, isPending: creating } = useMutation({
    mutationFn: () => supportApi.create({ subject: subject.trim(), category, message: message.trim() }),
    onSuccess: () => {
      toast.success(t("ticketCreated"));
      queryClient.invalidateQueries({ queryKey: ["my-support-tickets"] });
      setShowForm(false);
      setSubject("");
      setCategory("OTHER");
      setMessage("");
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const tickets = data?.content ?? [];
  const canSubmit = subject.trim().length > 0 && message.trim().length > 0;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="section-title">{t("title")}</h1>
        {!showForm && (
          <Button size="sm" onClick={() => setShowForm(true)} className="flex items-center gap-1.5">
            <Plus size={16} />
            {t("newTicket")}
          </Button>
        )}
      </div>

      {showForm && (
        <div className="card p-5 space-y-4">
          <Input
            label={t("subjectLabel")}
            value={subject}
            onChange={(e) => setSubject(e.target.value)}
            placeholder={t("subjectPlaceholder")}
            maxLength={200}
          />

          <div className="w-full">
            <label className="label">{t("categoryLabel")}</label>
            <select
              value={category}
              onChange={(e) => setCategory(e.target.value as SupportCategory)}
              className="w-full h-10 px-3 rounded-xl border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 text-sm outline-none focus:ring-2 focus:ring-brand-500"
            >
              {CATEGORIES.map((c) => (
                <option key={c} value={c}>{tCategory(c)}</option>
              ))}
            </select>
          </div>

          <div className="w-full">
            <label className="label">{t("messageLabel")}</label>
            <textarea
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              placeholder={t("messagePlaceholder")}
              rows={5}
              maxLength={4000}
              className="w-full rounded-xl border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 dark:placeholder:text-slate-500 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>

          <div className="flex gap-2">
            <Button loading={creating} disabled={!canSubmit} onClick={() => createTicket()}>
              {t("submit")}
            </Button>
            <Button variant="ghost" onClick={() => setShowForm(false)}>
              {tCommon("cancel")}
            </Button>
          </div>
        </div>
      )}

      {isLoading ? (
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="card h-20 animate-pulse bg-slate-100 dark:bg-slate-700" />
          ))}
        </div>
      ) : isError ? (
        <EmptyState icon={AlertCircle} message={error instanceof Error ? error.message : t("loadError")} variant="error" />
      ) : tickets.length === 0 && !showForm ? (
        <EmptyState icon={LifeBuoy} message={t("empty")} />
      ) : (
        <div className="card divide-y divide-slate-100 dark:divide-slate-700 overflow-hidden">
          {tickets.map((ticket) => (
            <Link
              key={ticket.id}
              href={`/${locale}/dashboard/support/${ticket.id}`}
              className="block px-5 py-4 hover:bg-slate-50 dark:hover:bg-slate-700/50"
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
                    <p className="text-sm text-slate-500 dark:text-slate-400 mt-0.5 truncate">{ticket.lastMessagePreview}</p>
                  )}
                </div>
                {ticket.lastMessageAt && (
                  <span className="shrink-0 text-xs text-slate-400 dark:text-slate-500">
                    {formatDate(ticket.lastMessageAt, locale)}
                  </span>
                )}
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
