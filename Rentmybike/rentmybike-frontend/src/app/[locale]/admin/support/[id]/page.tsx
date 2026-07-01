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
import type { SupportTicketStatus } from "@/types";

interface AdminSupportTicketDetailPageProps {
  params: { id: string };
}

const NEXT_STATUS_ACTIONS: Record<SupportTicketStatus, SupportTicketStatus[]> = {
  OPEN: ["IN_PROGRESS", "RESOLVED", "CLOSED"],
  IN_PROGRESS: ["RESOLVED", "CLOSED"],
  RESOLVED: ["IN_PROGRESS", "CLOSED"],
  CLOSED: ["OPEN"],
};

/**
 * Admin view of a single support ticket — full thread, a reply box, and
 * buttons to move the ticket through its status workflow. Replying
 * automatically bumps OPEN -> IN_PROGRESS on the backend (see
 * SupportService.adminAddMessage), so the explicit status buttons here are
 * mainly for RESOLVED/CLOSED and manual re-opening.
 * Admin-Ansicht eines einzelnen Support-Tickets — vollständiger Verlauf,
 * Antwortfeld und Buttons zum Durchlaufen des Status-Workflows. Antworten
 * setzt OPEN automatisch auf IN_PROGRESS im Backend (siehe
 * SupportService.adminAddMessage), daher dienen die expliziten
 * Status-Buttons hier vor allem für RESOLVED/CLOSED und manuelles
 * Wiedereröffnen.
 */
export default function AdminSupportTicketDetailPage({ params }: AdminSupportTicketDetailPageProps) {
  const { id } = params;
  const t = useTranslations("admin.support");
  const tCategory = useTranslations("support.categories");
  const tStatus = useTranslations("support.status");
  const locale = useLocale();
  const queryClient = useQueryClient();
  const [reply, setReply] = useState("");
  const scrollRef = useRef<HTMLDivElement>(null);

  const { data: ticket, isLoading } = useQuery({
    queryKey: ["admin-support-ticket", id],
    queryFn: () => supportApi.adminGet(id),
    select: (r) => r.data.data,
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["admin-support-ticket", id] });
    queryClient.invalidateQueries({ queryKey: ["admin-support-tickets"] });
  };

  const { mutate: sendReply, isPending: sending } = useMutation({
    mutationFn: () => supportApi.adminAddMessage(id, { body: reply.trim() }),
    onSuccess: () => {
      setReply("");
      invalidate();
      toast.success(t("replySent"));
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutate: updateStatus, isPending: updatingStatus } = useMutation({
    mutationFn: (status: SupportTicketStatus) => supportApi.adminUpdateStatus(id, status),
    onSuccess: () => {
      invalidate();
      toast.success(t("statusUpdated"));
    },
    onError: (e: Error) => toast.error(e.message),
  });

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: "smooth" });
  }, [ticket?.messages]);

  if (isLoading) {
    return <div className="h-96 animate-pulse bg-slate-100 rounded-2xl" />;
  }
  if (!ticket) {
    return <p className="text-sm text-slate-500">{t("notFound")}</p>;
  }

  return (
    <div className="space-y-4">
      <Link
        href={`/${locale}/admin/support`}
        className="inline-flex items-center gap-1.5 text-sm text-slate-500 hover:text-slate-700"
      >
        <ArrowLeft size={15} />
        {t("backToList")}
      </Link>

      <div className="flex items-start justify-between gap-3 flex-wrap">
        <div>
          <div className="flex items-center gap-2 flex-wrap">
            <span className="text-xs font-semibold uppercase tracking-wide text-slate-400">
              {tCategory(ticket.category)}
            </span>
            <SupportTicketStatusBadge status={ticket.status} />
          </div>
          <h1 className="text-xl font-semibold text-slate-900 mt-1">{ticket.subject}</h1>
          <p className="text-sm text-slate-500 mt-0.5">
            {t("filedBy", { name: ticket.userName })} · {ticket.userEmail}
          </p>
        </div>

        <div className="flex gap-2 flex-wrap">
          {NEXT_STATUS_ACTIONS[ticket.status].map((next) => (
            <Button
              key={next}
              size="sm"
              variant={next === "CLOSED" ? "outline" : "secondary"}
              loading={updatingStatus}
              onClick={() => updateStatus(next)}
            >
              {t("moveTo", { status: tStatus(next) })}
            </Button>
          ))}
        </div>
      </div>

      <div className="card flex flex-col h-[28rem] overflow-hidden">
        <div ref={scrollRef} className="flex-1 overflow-y-auto p-4 space-y-3 bg-slate-50">
          {(ticket.messages ?? []).map((m) => (
            <div key={m.id} className={`flex flex-col ${m.fromAdmin ? "items-end" : "items-start"}`}>
              <div
                className={`max-w-[75%] rounded-2xl px-3.5 py-2 text-sm break-words whitespace-pre-wrap ${
                  m.fromAdmin
                    ? "bg-brand-600 text-white"
                    : "bg-white text-slate-700 border border-slate-200"
                }`}
              >
                {m.body}
              </div>
              <span className="text-[11px] text-slate-400 mt-1 px-1">
                {m.senderName} · {formatDate(m.createdAt, locale, "dd MMM yyyy HH:mm")}
              </span>
            </div>
          ))}
        </div>

        <div className="p-3 border-t border-slate-100 bg-white">
          {ticket.status === "CLOSED" ? (
            <p className="text-sm text-slate-500 text-center py-1.5">{t("closedNotice")}</p>
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
                className="flex-1 resize-none rounded-xl border border-slate-300 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-brand-500"
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
