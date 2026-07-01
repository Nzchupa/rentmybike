"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Send } from "lucide-react";
import toast from "react-hot-toast";
import { supportApi } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { SupportTicketStatusBadge } from "@/components/ui/Badge";
import { formatDate } from "@/lib/utils";

interface SupportTicketDetailPageProps {
  // Next.js 14 (this project's version) passes params synchronously — see
  // the matching comment in dashboard/bikes/[id]/edit/page.tsx.
  params: { id: string };
}

/**
 * A single support ticket's thread — messages from the user and admin
 * replies in one chronological view, plus a reply box. Mirrors ChatPanel's
 * bubble layout (mine vs. other) but polls over plain REST rather than
 * STOMP, since support replies aren't expected to be sub-second real-time.
 * Der Thread eines einzelnen Support-Tickets — Nachrichten des Benutzers und
 * Admin-Antworten in einer chronologischen Ansicht, plus Antwortfeld. Folgt
 * dem Sprechblasen-Layout von ChatPanel (eigene vs. andere), aber pollt über
 * einfaches REST statt STOMP, da Support-Antworten keine Sekundenbruchteile-
 * Echtzeit erfordern.
 */
export default function SupportTicketDetailPage({ params }: SupportTicketDetailPageProps) {
  const { id } = params;
  const t = useTranslations("dashboard.support");
  const tCategory = useTranslations("support.categories");
  const locale = useLocale();
  const queryClient = useQueryClient();
  const [reply, setReply] = useState("");
  const scrollRef = useRef<HTMLDivElement>(null);

  const { data: ticket, isLoading } = useQuery({
    queryKey: ["my-support-ticket", id],
    queryFn: () => supportApi.getMine(id),
    select: (r) => r.data.data,
    // Light polling so a reply from support shows up without a manual
    // refresh, without the complexity of a dedicated WebSocket channel just
    // for this. / Leichtes Polling, damit eine Support-Antwort ohne
    // manuelles Neuladen erscheint, ohne die Komplexität eines eigenen
    // WebSocket-Kanals nur dafür.
    refetchInterval: 15_000,
  });

  const { mutate: sendReply, isPending: sending } = useMutation({
    mutationFn: () => supportApi.addMyMessage(id, { body: reply.trim() }),
    onSuccess: () => {
      setReply("");
      queryClient.invalidateQueries({ queryKey: ["my-support-ticket", id] });
      queryClient.invalidateQueries({ queryKey: ["my-support-tickets"] });
    },
    onError: (e: Error) => toast.error(e.message),
  });

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: "smooth" });
  }, [ticket?.messages]);

  if (isLoading) {
    return <div className="h-96 animate-pulse bg-slate-100 dark:bg-slate-700 rounded-2xl" />;
  }
  if (!ticket) {
    return <p className="text-sm text-slate-500 dark:text-slate-400">{t("notFound")}</p>;
  }

  const closed = ticket.status === "CLOSED";

  return (
    <div className="space-y-4">
      <Link
        href={`/${locale}/dashboard/support`}
        className="inline-flex items-center gap-1.5 text-sm text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200"
      >
        <ArrowLeft size={15} />
        {t("backToList")}
      </Link>

      <div className="flex items-start justify-between gap-3 flex-wrap">
        <div>
          <div className="flex items-center gap-2 flex-wrap">
            <span className="text-xs font-semibold uppercase tracking-wide text-slate-400 dark:text-slate-500">
              {tCategory(ticket.category)}
            </span>
            <SupportTicketStatusBadge status={ticket.status} />
          </div>
          <h1 className="text-xl font-semibold text-slate-900 dark:text-slate-100 mt-1">{ticket.subject}</h1>
        </div>
      </div>

      <div className="card flex flex-col h-[28rem] overflow-hidden">
        <div ref={scrollRef} className="flex-1 overflow-y-auto p-4 space-y-3 bg-slate-50 dark:bg-slate-900/40">
          {(ticket.messages ?? []).map((m) => (
            <div key={m.id} className={`flex flex-col ${m.fromAdmin ? "items-start" : "items-end"}`}>
              <div
                className={`max-w-[75%] rounded-2xl px-3.5 py-2 text-sm break-words whitespace-pre-wrap ${
                  m.fromAdmin
                    ? "bg-white dark:bg-slate-700 text-slate-700 dark:text-slate-100 border border-slate-200 dark:border-slate-600"
                    : "bg-brand-600 text-white"
                }`}
              >
                {m.body}
              </div>
              <span className="text-[11px] text-slate-400 dark:text-slate-500 mt-1 px-1">
                {m.fromAdmin ? t("supportTeam") : m.senderName} · {formatDate(m.createdAt, locale, "dd MMM yyyy HH:mm")}
              </span>
            </div>
          ))}
        </div>

        <div className="p-3 border-t border-slate-100 dark:border-slate-700 bg-white dark:bg-slate-800">
          {closed ? (
            <p className="text-sm text-slate-500 dark:text-slate-400 text-center py-1.5">{t("closedNotice")}</p>
          ) : (
            <div className="flex items-center gap-2">
              <textarea
                value={reply}
                onChange={(e) => setReply(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter" && !e.shiftKey) {
                    e.preventDefault();
                    if (reply.trim()) sendReply();
                  }
                }}
                placeholder={t("replyPlaceholder")}
                rows={1}
                maxLength={4000}
                className="flex-1 resize-none rounded-xl border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 dark:placeholder:text-slate-500 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-brand-500"
              />
              <Button
                size="sm"
                disabled={!reply.trim() || sending}
                loading={sending}
                onClick={() => sendReply()}
                className="shrink-0"
                aria-label={t("send")}
              >
                <Send size={15} />
              </Button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
