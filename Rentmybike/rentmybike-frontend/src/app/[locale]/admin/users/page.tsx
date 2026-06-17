"use client";

import { useEffect, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Search, ShieldBan, ShieldCheck, Trash2 } from "lucide-react";
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
  const queryClient = useQueryClient();

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

  const { data, isLoading } = useQuery({
    queryKey: ["admin-users", debouncedSearch],
    queryFn: () => adminApi.listUsers(debouncedSearch || undefined, 0, 50),
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

  const users = data?.content ?? [];

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
          className="w-full h-10 pl-9 pr-3 rounded-xl border border-slate-300 text-sm outline-none focus:ring-2 focus:ring-brand-500"
        />
      </div>

      {/* Table */}
      {isLoading ? (
        <div className="space-y-2">
          {[1, 2, 3, 4, 5].map((i) => (
            <div key={i} className="h-14 rounded-xl bg-slate-100 animate-pulse" />
          ))}
        </div>
      ) : (
        <div className="card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-slate-50 border-b border-slate-200">
                <tr>
                  <th className="text-left px-4 py-3 font-medium text-slate-600">{t("userColumn")}</th>
                  <th className="text-left px-4 py-3 font-medium text-slate-600">{t("role")}</th>
                  <th className="text-left px-4 py-3 font-medium text-slate-600">{t("status")}</th>
                  <th className="text-left px-4 py-3 font-medium text-slate-600">{t("joined")}</th>
                  <th className="text-right px-4 py-3 font-medium text-slate-600">{t("actions")}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {users.map((user) => (
                  <tr key={user.id} className="hover:bg-slate-50">
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <Avatar name={`${user.firstName} ${user.lastName}`} avatarUrl={user.avatarUrl} size="sm" />
                        <div>
                          <p className="font-medium text-slate-900">
                            {user.firstName} {user.lastName}
                          </p>
                          <p className="text-xs text-slate-500">{user.email}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <Badge variant={user.role === "ADMIN" ? "blue" : "gray"}>
                        {user.role}
                      </Badge>
                    </td>
                    <td className="px-4 py-3">
                      {user.banned ? (
                        <Badge variant="red">{t("banned")}</Badge>
                      ) : (
                        <Badge variant="green">{t("active")}</Badge>
                      )}
                    </td>
                    <td className="px-4 py-3 text-slate-500">
                      {formatDate(user.createdAt)}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-1">
                        {user.role !== "ADMIN" && (
                          <>
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
              <p className="text-center py-10 text-slate-500">{t("noUsersFound")}</p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
