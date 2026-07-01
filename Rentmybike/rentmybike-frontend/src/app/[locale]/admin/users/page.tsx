"use client";

import { useEffect, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Search,
  ShieldBan,
  ShieldCheck,
  Trash2,
  BadgeCheck,
  BadgeX,
  PauseCircle,
  PlayCircle,
  Briefcase,
  Crown,
} from "lucide-react";
import toast from "react-hot-toast";
import { adminApi } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { Avatar } from "@/components/ui/Avatar";
import { Badge } from "@/components/ui/Badge";
import { formatDate } from "@/lib/utils";

/**
 * Admin user management page.
 * Admin-Benutzerverwaltungsseite.
 */
export default function AdminUsersPage() {
  const t = useTranslations("admin.users");
  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [page, setPage] = useState(0);
  const PAGE_SIZE = 20;
  const queryClient = useQueryClient();

  // Reset to the first page whenever the search term actually changes —
  // otherwise a search narrowing the result set could leave `page` pointing
  // past the new last page, rendering an empty table with no obvious cause.
  // Zurück zur ersten Seite bei jeder Suchänderung — sonst könnte `page`
  // nach einer Eingrenzung der Ergebnisse über die neue letzte Seite
  // hinauszeigen und eine leere Tabelle ohne ersichtlichen Grund anzeigen.
  useEffect(() => {
    setPage(0);
  }, [debouncedSearch]);

  // Debounce search — a timer stashed on `window` leaked across remounts
  // (e.g. fast nav away + back) and was never cleared on unmount, so a
  // pending setDebouncedSearch could fire after the component was gone.
  // useRef scopes the timer to this component instance and the cleanup
  // effect clears it on unmount.
  //
  // Debounce-Suche — ein auf `window` abgelegter Timer überlebte Remounts
  // (z. B. schnelle Navigation weg und zurück) und wurde beim Unmount nie
  // gelöscht, sodass ein ausstehendes setDebouncedSearch nach dem
  // Verschwinden der Komponente noch feuern konnte. useRef bindet den Timer
  // an diese Komponenteninstanz, und der Cleanup-Effect löscht ihn beim Unmount.
  const debounceRef = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);

  useEffect(() => {
    return () => clearTimeout(debounceRef.current);
  }, []);

  const handleSearchChange = (val: string) => {
    setSearch(val);
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => setDebouncedSearch(val), 300);
  };

  // isError was previously never read here, so any failed request (auth/CORS/
  // cookie-domain problem in production, or a transient 500) rendered exactly
  // like "there are simply no users" — the empty-state message — instead of
  // surfacing the real problem. Showing the error explicitly turns a silent,
  // hard-to-diagnose "Benutzerverwaltung shows no data" report into something
  // actionable.
  // isError wurde hier vorher nie ausgelesen, daher sah jede fehlgeschlagene
  // Anfrage (Auth-/CORS-/Cookie-Domain-Problem in Produktion oder ein
  // vorübergehender 500er) exakt so aus wie "es gibt einfach keine
  // Benutzer" — die Leerzustand-Meldung — statt das eigentliche Problem
  // offenzulegen. Den Fehler explizit anzuzeigen macht aus einem stillen,
  // schwer diagnostizierbaren "Benutzerverwaltung zeigt keine Daten"-Bericht
  // etwas Handlungsfähiges.
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["admin-users", debouncedSearch, page],
    queryFn: () => adminApi.listUsers(debouncedSearch || undefined, page, PAGE_SIZE),
    select: (r) => r.data.data,
  });

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: ["admin-users"] });

  const { mutate: ban } = useMutation({
    mutationFn: adminApi.banUser,
    onSuccess: () => { toast.success(t("userBanned")); invalidate(); },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutate: unban } = useMutation({
    mutationFn: adminApi.unbanUser,
    onSuccess: () => { toast.success(t("userUnbanned")); invalidate(); },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutate: deleteUser } = useMutation({
    mutationFn: adminApi.deleteUser,
    onSuccess: () => { toast.success(t("userDeleted")); invalidate(); },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutate: verifyBusiness } = useMutation({
    mutationFn: adminApi.verifyBusiness,
    onSuccess: () => { toast.success(t("businessVerified")); invalidate(); },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutate: unverifyBusiness } = useMutation({
    mutationFn: adminApi.unverifyBusiness,
    onSuccess: () => { toast.success(t("businessUnverified")); invalidate(); },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutate: suspend } = useMutation({
    mutationFn: adminApi.suspendUser,
    onSuccess: () => { toast.success(t("userSuspended")); invalidate(); },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutate: unsuspend } = useMutation({
    mutationFn: adminApi.unsuspendUser,
    onSuccess: () => { toast.success(t("userUnsuspended")); invalidate(); },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutate: promoteToBusiness } = useMutation({
    mutationFn: adminApi.promoteToBusiness,
    onSuccess: () => { toast.success(t("userPromotedBusiness")); invalidate(); },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutate: promoteToAdmin } = useMutation({
    mutationFn: adminApi.promoteToAdmin,
    onSuccess: () => { toast.success(t("userPromotedAdmin")); invalidate(); },
    onError: (e: Error) => toast.error(e.message),
  });

  const users = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  return (
    <div className="space-y-6">
      <h1 className="section-title">{t("title")}</h1>

      {/* Search */}
      <div className="relative max-w-sm">
        <Search
          size={16}
          className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
        />
        <input
          value={search}
          onChange={(e) => handleSearchChange(e.target.value)}
          placeholder={t("searchPlaceholder")}
          className="w-full h-10 pl-9 pr-3 rounded-xl border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 dark:placeholder:text-slate-500 text-sm outline-none focus:ring-2 focus:ring-brand-500"
        />
      </div>

      {/* Table */}
      {isLoading ? (
        <div className="space-y-2">
          {[1, 2, 3, 4, 5].map((i) => (
            <div key={i} className="h-14 rounded-xl bg-slate-100 dark:bg-slate-700 animate-pulse" />
          ))}
        </div>
      ) : isError ? (
        <div className="card p-10 text-center text-red-600 dark:text-red-400">
          <p>{error instanceof Error ? error.message : t("loadError")}</p>
        </div>
      ) : (
        <div className="card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-slate-50 dark:bg-slate-700/50 border-b border-slate-200 dark:border-slate-700">
                <tr>
                  <th className="text-left px-4 py-3 font-medium text-slate-600 dark:text-slate-300">{t("userColumn")}</th>
                  <th className="text-left px-4 py-3 font-medium text-slate-600 dark:text-slate-300">{t("role")}</th>
                  <th className="text-left px-4 py-3 font-medium text-slate-600 dark:text-slate-300">{t("status")}</th>
                  <th className="text-left px-4 py-3 font-medium text-slate-600 dark:text-slate-300">{t("activity")}</th>
                  <th className="text-left px-4 py-3 font-medium text-slate-600 dark:text-slate-300">{t("joined")}</th>
                  <th className="text-right px-4 py-3 font-medium text-slate-600 dark:text-slate-300">{t("actions")}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-700">
                {users.map((user) => (
                  <tr key={user.id} className="hover:bg-slate-50 dark:hover:bg-slate-700/50">
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <Avatar name={`${user.firstName} ${user.lastName}`} avatarUrl={user.avatarUrl} size="sm" />
                        <div>
                          <p className="font-medium text-slate-900 dark:text-slate-100">
                            {user.firstName} {user.lastName}
                          </p>
                          <p className="text-xs text-slate-500 dark:text-slate-400">{user.email}</p>
                          {user.businessName && (
                            <p className="text-xs text-slate-400 dark:text-slate-500">{user.businessName}</p>
                          )}
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-1.5 flex-wrap">
                        <Badge variant={user.role === "ADMIN" ? "blue" : "gray"}>
                          {user.role}
                        </Badge>
                        {user.role === "BUSINESS" && (
                          <Badge variant={user.businessVerified ? "green" : "yellow"}>
                            {user.businessVerified ? t("businessVerifiedBadge") : t("businessPendingBadge")}
                          </Badge>
                        )}
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      {/* Banned and suspended are distinct states on the DTO
                          (bannedAt vs suspendedAt) — previously collapsed
                          into the same "not active" bucket, hiding which
                          one actually applied. Banned takes visual priority
                          since it's the more severe state. */}
                      {user.banned ? (
                        <Badge variant="red">{t("banned")}</Badge>
                      ) : user.suspendedAt ? (
                        <Badge variant="yellow">{t("suspended")}</Badge>
                      ) : (
                        <Badge variant="green">{t("active")}</Badge>
                      )}
                    </td>
                    <td className="px-4 py-3 text-slate-500 dark:text-slate-400 text-xs">
                      <p>{t("bikesCount", { count: user.bikeCount })}</p>
                      <p>{t("bookingsCount", { count: user.bookingCount })}</p>
                      {user.lastActivityAt && (
                        <p className="text-slate-400 dark:text-slate-500">{formatDate(user.lastActivityAt)}</p>
                      )}
                    </td>
                    <td className="px-4 py-3 text-slate-500 dark:text-slate-400">
                      {formatDate(user.createdAt)}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-1">
                        {user.role === "BUSINESS" && (
                          user.businessVerified ? (
                            <Button
                              size="sm"
                              variant="ghost"
                              title={t("unverifyBusiness")}
                              onClick={() => unverifyBusiness(user.id)}
                            >
                              <BadgeX size={15} className="text-slate-500 dark:text-slate-400" />
                            </Button>
                          ) : (
                            <Button
                              size="sm"
                              variant="ghost"
                              title={t("verifyBusiness")}
                              onClick={() => verifyBusiness(user.id)}
                            >
                              <BadgeCheck size={15} className="text-emerald-600" />
                            </Button>
                          )
                        )}
                        {user.role !== "ADMIN" && (
                          <>
                            {user.suspendedAt ? (
                              <Button
                                size="sm"
                                variant="ghost"
                                title={t("unsuspend")}
                                onClick={() => unsuspend(user.id)}
                              >
                                <PlayCircle size={15} className="text-green-600" />
                              </Button>
                            ) : (
                              <Button
                                size="sm"
                                variant="ghost"
                                title={t("suspend")}
                                onClick={() => {
                                  if (confirm(t("suspendConfirm"))) suspend(user.id);
                                }}
                              >
                                <PauseCircle size={15} className="text-amber-600" />
                              </Button>
                            )}
                            {user.banned ? (
                              <Button
                                size="sm"
                                variant="ghost"
                                title={t("unban")}
                                onClick={() => unban(user.id)}
                              >
                                <ShieldCheck size={15} className="text-green-600" />
                              </Button>
                            ) : (
                              <Button
                                size="sm"
                                variant="ghost"
                                title={t("ban")}
                                onClick={() => {
                                  if (confirm(t("banConfirm"))) ban(user.id);
                                }}
                              >
                                <ShieldBan size={15} className="text-amber-600" />
                              </Button>
                            )}
                            {user.role === "USER" && (
                              <Button
                                size="sm"
                                variant="ghost"
                                title={t("promoteToBusiness")}
                                onClick={() => {
                                  if (confirm(t("promoteToBusinessConfirm"))) promoteToBusiness(user.id);
                                }}
                              >
                                <Briefcase size={15} className="text-blue-600" />
                              </Button>
                            )}
                            <Button
                              size="sm"
                              variant="ghost"
                              title={t("promoteToAdmin")}
                              onClick={() => {
                                if (confirm(t("promoteToAdminConfirm"))) promoteToAdmin(user.id);
                              }}
                            >
                              <Crown size={15} className="text-purple-600" />
                            </Button>
                            <Button
                              size="sm"
                              variant="ghost"
                              title={t("delete")}
                              onClick={() => {
                                if (confirm(t("deleteConfirm"))) deleteUser(user.id);
                              }}
                            >
                              <Trash2 size={15} className="text-red-500" />
                            </Button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            {users.length === 0 && (
              <p className="text-center py-10 text-slate-500 dark:text-slate-400">{t("noUsersFound")}</p>
            )}
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex justify-center items-center gap-2 py-4 border-t border-slate-100 dark:border-slate-700">
              <Button
                variant="outline"
                size="sm"
                disabled={page === 0}
                onClick={() => setPage((p) => p - 1)}
              >
                {t("previous")}
              </Button>
              <span className="flex items-center px-4 text-sm text-slate-600 dark:text-slate-400">
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
