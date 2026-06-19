"use client";

import { useLocale, useTranslations } from "next-intl";
import { useQuery, useQueryClient, useMutation } from "@tanstack/react-query";
import Link from "next/link";
import { Bell, AlertCircle } from "lucide-react";
import { notificationsApi } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { NotificationIcon } from "@/components/notifications/NotificationIcon";
import { cn, formatDate } from "@/lib/utils";
import type { NotificationType } from "@/types";

// Admin notifications are read through the same shared /api/v1/notifications
// pipeline regular users use (see NotificationController) — there's no
// separate admin endpoint. The two admin-only types (ADMIN_NEW_PENDING_BIKE,
// ADMIN_NEW_REPORT) carry no bookingId/bikeId, so instead of a per-item deep
// link we route by type to the relevant queue page (bikes vs. moderation),
// mirroring how the Moderation Center links out rather than duplicating UI.
//
// Admin-Benachrichtigungen laufen über dieselbe gemeinsame
// /api/v1/notifications-Pipeline wie bei normalen Nutzern — es gibt keinen
// separaten Admin-Endpunkt. Die beiden admin-spezifischen Typen tragen keine
// bookingId/bikeId, daher leiten wir nach Typ zur passenden Warteschlange
// weiter (Fahrräder bzw. Moderation), analog zum Moderation-Center.
const ADMIN_TYPE_TARGETS: Partial<Record<NotificationType, string>> = {
  ADMIN_NEW_PENDING_BIKE: "/admin/bikes",
  ADMIN_NEW_REPORT: "/admin/moderation",
};

export default function AdminNotificationsPage() {
  const t = useTranslations("admin.notifications");
  const tCommon = useTranslations("notifications");
  const locale = useLocale();
  const queryClient = useQueryClient();

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["admin-notifications-all"],
    queryFn: () => notificationsApi.list(0, 50),
    select: (r) => r.data.data,
  });

  const invalidateAll = () => {
    queryClient.invalidateQueries({ queryKey: ["admin-notifications-all"] });
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
            {tCommon("markAllRead")}
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
        <EmptyState
          icon={AlertCircle}
          message={error instanceof Error ? error.message : tCommon("loadError")}
          variant="error"
        />
      ) : notifications.length === 0 ? (
        <EmptyState icon={Bell} message={t("empty")} />
      ) : (
        <div className="card divide-y divide-slate-100 overflow-hidden">
          {notifications.map((n) => {
            const adminTarget = ADMIN_TYPE_TARGETS[n.type];
            const href = adminTarget
              ? `/${locale}${adminTarget}`
              : `/${locale}/dashboard/bookings/${n.viewAsOwner === false ? "renter" : "owner"}`;
            return (
              <Link
                key={n.id}
                href={href}
                onClick={() => {
                  if (!n.read) markOneAsRead(n.id);
                }}
                className={cn(
                  "block px-5 py-4 hover:bg-slate-50",
                  !n.read && "bg-brand-50/40"
                )}
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-start gap-3 min-w-0">
                    <NotificationIcon type={n.type} />
                    <div className="min-w-0">
                      <p className="text-sm font-semibold text-slate-900">{n.title}</p>
                      <p className="text-sm text-slate-600 mt-1">{n.message}</p>
                    </div>
                  </div>
                  {!n.read && (
                    <span className="shrink-0 mt-1 w-2 h-2 rounded-full bg-brand-500" />
                  )}
                </div>
                <p className="text-xs text-slate-400 mt-2 ml-12">{formatDate(n.createdAt, locale)}</p>
              </Link>
            );
          })}
        </div>
      )}
    </div>
  );
}
