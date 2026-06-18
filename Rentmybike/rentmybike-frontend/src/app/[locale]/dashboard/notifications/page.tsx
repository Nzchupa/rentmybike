"use client";

import { useLocale, useTranslations } from "next-intl";
import { useQuery, useQueryClient, useMutation } from "@tanstack/react-query";
import Link from "next/link";
import { notificationsApi } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { cn, formatDate } from "@/lib/utils";

/**
 * Full notification feed page — the "view all" destination from the navbar
 * bell dropdown. Part of Bug 5 ("rental requests" / notification surface).
 * Vollständige Benachrichtigungs-Feed-Seite — das "alle ansehen"-Ziel aus dem
 * Glocken-Dropdown im Navbar. Teil von Bug 5 ("Mietanfragen" /
 * Benachrichtigungsoberfläche).
 */
export default function NotificationsPage() {
  const t = useTranslations("notifications");
  const locale = useLocale();
  const queryClient = useQueryClient();

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["notifications-all"],
    queryFn: () => notificationsApi.list(0, 50),
    select: (r) => r.data.data,
  });

  const invalidateAll = () => {
    queryClient.invalidateQueries({ queryKey: ["notifications-all"] });
    queryClient.invalidateQueries({ queryKey: ["notifications-unread-count"] });
    queryClient.invalidateQueries({ queryKey: ["notifications-recent"] });
  };

  const { mutate: markAllAsRead } = useMutation({
    mutationFn: notificationsApi.markAllAsRead,
    onSuccess: invalidateAll,
  });

  const { mutate: markOneAsRead } = useMutation({
    mutationFn: notificationsApi.markAsRead,
    onSuccess: invalidateAll,
  });

  const notifications = data?.content ?? [];
  const hasUnread = notifications.some((n) => !n.read);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="section-title">{t("title")}</h1>
        {hasUnread && (
          <Button variant="outline" size="sm" onClick={() => markAllAsRead()}>
            {t("markAllRead")}
          </Button>
        )}
      </div>

      {isLoading ? (
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="card h-20 animate-pulse bg-slate-100" />
          ))}
        </div>
      ) : isError ? (
        <div className="card p-12 text-center text-red-600">
          <p>{error instanceof Error ? error.message : t("loadError")}</p>
        </div>
      ) : notifications.length === 0 ? (
        <div className="card p-12 text-center text-slate-500">
          <p>{t("empty")}</p>
        </div>
      ) : (
        <div className="card divide-y divide-slate-100 overflow-hidden">
          {notifications.map((n) => (
            <Link
              key={n.id}
              // See NotificationBell.tsx for why this can't be hardcoded to "owner" —
              // NEW_CHAT_MESSAGE notifications can be addressed to either side of a booking.
              href={`/${locale}/dashboard/bookings/${n.viewAsOwner === false ? "renter" : "owner"}`}
              onClick={() => {
                if (!n.read) markOneAsRead(n.id);
              }}
              className={cn(
                "block px-5 py-4 hover:bg-slate-50",
                !n.read && "bg-brand-50/40"
              )}
            >
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="text-sm font-semibold text-slate-900">{n.title}</p>
                  <p className="text-sm text-slate-600 mt-1">{n.message}</p>
                </div>
                {!n.read && (
                  <span className="shrink-0 mt-1 w-2 h-2 rounded-full bg-brand-500" />
                )}
              </div>
              <p className="text-xs text-slate-400 mt-2">{formatDate(n.createdAt, locale)}</p>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
