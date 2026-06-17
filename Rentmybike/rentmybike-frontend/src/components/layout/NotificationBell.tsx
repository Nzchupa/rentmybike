"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import { useQuery, useQueryClient, useMutation } from "@tanstack/react-query";
import { Bell } from "lucide-react";
import { notificationsApi } from "@/lib/api";
import { cn, formatDate } from "@/lib/utils";

/**
 * Bell icon with unread-notification badge, shown in the navbar for
 * authenticated users — the in-app half of Bug 5 ("owner gets no
 * notification when a renter requests their bike").
 * Glocken-Icon mit Ungelesen-Badge, im Navbar für angemeldete Benutzer
 * angezeigt — die In-App-Hälfte von Bug 5 ("Eigentümer erhält keine
 * Benachrichtigung, wenn ein Mieter sein Fahrrad anfragt").
 *
 * <p>Polls the unread count every 30s — simple and good enough for an MVP;
 * a websocket/SSE push would avoid the polling delay in production.
 * <p>Fragt den Ungelesen-Zähler alle 30s ab — einfach und für ein MVP
 * ausreichend; ein Websocket/SSE-Push würde die Polling-Verzögerung in
 * Produktion vermeiden.
 */
export function NotificationBell() {
  const t = useTranslations("notifications");
  const locale = useLocale();
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  const { data: unreadCount } = useQuery({
    queryKey: ["notifications-unread-count"],
    queryFn: () => notificationsApi.getUnreadCount(),
    select: (r) => r.data.data.unreadCount,
    refetchInterval: 30_000,
  });

  const { data: notifications } = useQuery({
    queryKey: ["notifications-recent"],
    queryFn: () => notificationsApi.list(0, 8),
    select: (r) => r.data.data.content,
    enabled: open,
  });

  const { mutate: markAllAsRead } = useMutation({
    mutationFn: notificationsApi.markAllAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["notifications-unread-count"] });
      queryClient.invalidateQueries({ queryKey: ["notifications-recent"] });
    },
  });

  const { mutate: markOneAsRead } = useMutation({
    mutationFn: notificationsApi.markAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["notifications-unread-count"] });
      queryClient.invalidateQueries({ queryKey: ["notifications-recent"] });
    },
  });

  // Close the dropdown on outside click.
  // Dropdown bei Klick außerhalb schließen.
  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const count = unreadCount ?? 0;
  const list = notifications ?? [];

  return (
    <div className="relative" ref={containerRef}>
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        className="relative p-2 rounded-lg text-slate-600 hover:bg-slate-100 hover:text-slate-900"
        aria-label={t("bellLabel")}
      >
        <Bell size={20} />
        {count > 0 && (
          <span className="absolute top-0.5 right-0.5 flex items-center justify-center min-w-[16px] h-4 px-1 rounded-full bg-red-500 text-white text-[10px] font-semibold leading-none">
            {count > 9 ? "9+" : count}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 mt-2 w-80 max-w-[90vw] bg-white border border-slate-200 rounded-xl shadow-lg z-50 overflow-hidden">
          <div className="flex items-center justify-between px-4 py-3 border-b border-slate-100">
            <span className="text-sm font-semibold text-slate-900">{t("title")}</span>
            {count > 0 && (
              <button
                type="button"
                onClick={() => markAllAsRead()}
                className="text-xs font-medium text-brand-600 hover:text-brand-700"
              >
                {t("markAllRead")}
              </button>
            )}
          </div>

          <div className="max-h-96 overflow-y-auto">
            {list.length === 0 ? (
              <p className="text-center py-8 text-sm text-slate-500">{t("empty")}</p>
            ) : (
              list.map((n) => (
                <Link
                  key={n.id}
                  href={`/${locale}/dashboard/bookings/owner`}
                  onClick={() => {
                    if (!n.read) markOneAsRead(n.id);
                    setOpen(false);
                  }}
                  className={cn(
                    "block px-4 py-3 border-b border-slate-50 last:border-0 hover:bg-slate-50",
                    !n.read && "bg-brand-50/50"
                  )}
                >
                  <p className="text-sm font-medium text-slate-900">{n.title}</p>
                  <p className="text-xs text-slate-600 mt-0.5 line-clamp-2">{n.message}</p>
                  <p className="text-[11px] text-slate-400 mt-1">{formatDate(n.createdAt, locale)}</p>
                </Link>
              ))
            )}
          </div>

          <Link
            href={`/${locale}/dashboard/notifications`}
            onClick={() => setOpen(false)}
            className="block text-center py-2.5 text-xs font-medium text-brand-600 hover:bg-slate-50 border-t border-slate-100"
          >
            {t("viewAll")}
          </Link>
        </div>
      )}
    </div>
  );
}
