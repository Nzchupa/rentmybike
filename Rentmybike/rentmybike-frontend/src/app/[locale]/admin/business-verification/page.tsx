"use client";

import { useState } from "react";
import { useTranslations, useLocale } from "next-intl";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Search, Briefcase, ShieldCheck, ShieldQuestion, BadgeCheck, BadgeX } from "lucide-react";
import toast from "react-hot-toast";
import { adminApi } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { Avatar } from "@/components/ui/Avatar";
import { Badge } from "@/components/ui/Badge";
import { EmptyState } from "@/components/ui/EmptyState";
import { formatDate } from "@/lib/utils";

type StatusTab = "PENDING" | "VERIFIED" | "ALL";

const TABS: StatusTab[] = ["PENDING", "VERIFIED", "ALL"];

// A large single fetch rather than true server-side pagination — the
// /admin/users endpoint has no role filter, so a "BUSINESS only" view has
// to filter client-side over whatever page it's given anyway. For the
// realistic scale of this platform, one larger page covers every business
// account without needing a second backend parameter just for this page.
//
// Ein einzelner großer Abruf statt echter serverseitiger Paginierung — der
// /admin/users-Endpunkt hat keinen Rollenfilter, daher muss eine reine
// BUSINESS-Ansicht ohnehin clientseitig über die gelieferte Seite filtern.
// Für den realistischen Umfang dieser Plattform deckt eine größere Seite
// alle Geschäftskonten ab, ohne einen zweiten Backend-Parameter nur für
// diese Seite einzuführen.
const FETCH_SIZE = 200;

/**
 * Business Verification Center — a dedicated triage view for admins to
 * review BUSINESS-role accounts and grant/revoke the "verified business"
 * badge. Verify/unverify already existed inline in the generic Admin User
 * Management table (admin/users), but that table mixes every account role
 * together and has no way to see "who's still waiting on verification" at
 * a glance — exactly the same gap the Moderation Center closed for bike
 * approvals + reports. This page surfaces pending/verified counts up front
 * and a focused, business-only list with full account context (bikes,
 * bookings, joined date) right next to the verify action.
 *
 * Business-Verifizierungs-Center — eine eigene Triage-Ansicht für Admins zur
 * Prüfung von BUSINESS-Konten und zum Vergeben/Entziehen des
 * "Verifiziertes Unternehmen"-Abzeichens. Verifizieren/Entverifizieren gab
 * es bereits inline in der allgemeinen Admin-Benutzerverwaltung
 * (admin/users), aber diese Tabelle vermischt alle Kontorollen und zeigt
 * nicht auf einen Blick, "wer noch auf Verifizierung wartet" — genau die
 * Lücke, die das Moderation-Center für Fahrrad-Genehmigungen und Meldungen
 * geschlossen hat. Diese Seite zeigt Pending-/Verifiziert-Zähler und eine
 * fokussierte, reine Unternehmensliste mit vollem Kontext direkt neben der
 * Verifizierungs-Aktion.
 */
export default function BusinessVerificationCenterPage() {
  const t = useTranslations("admin.businessVerification");
  const tUsers = useTranslations("admin.users");
  const locale = useLocale();
  const queryClient = useQueryClient();

  const [search, setSearch] = useState("");
  const [tab, setTab] = useState<StatusTab>("PENDING");

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["admin-business-verification"],
    queryFn: () => adminApi.listUsers(undefined, 0, FETCH_SIZE),
    select: (r) => r.data.data.content.filter((u) => u.role === "BUSINESS"),
  });

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: ["admin-business-verification"] });

  const { mutate: verifyBusiness, isPending: verifying } = useMutation({
    mutationFn: adminApi.verifyBusiness,
    onSuccess: () => { toast.success(tUsers("businessVerified")); invalidate(); },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutate: unverifyBusiness, isPending: unverifying } = useMutation({
    mutationFn: adminApi.unverifyBusiness,
    onSuccess: () => { toast.success(tUsers("businessUnverified")); invalidate(); },
    onError: (e: Error) => toast.error(e.message),
  });

  const businesses = data ?? [];
  const pendingCount = businesses.filter((u) => !u.businessVerified).length;
  const verifiedCount = businesses.filter((u) => u.businessVerified).length;

  const searchLower = search.trim().toLowerCase();
  const filtered = businesses.filter((u) => {
    if (tab === "PENDING" && u.businessVerified) return false;
    if (tab === "VERIFIED" && !u.businessVerified) return false;
    if (!searchLower) return true;
    const haystack = `${u.businessName ?? ""} ${u.firstName} ${u.lastName} ${u.email}`.toLowerCase();
    return haystack.includes(searchLower);
  });

  return (
    <div className="space-y-6">
      <h1 className="section-title">{t("title")}</h1>
      <p className="text-sm text-slate-500 max-w-2xl">{t("subtitle")}</p>

      {/* Summary stats */}
      <div className="grid sm:grid-cols-2 gap-4">
        <div className="card p-5 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-amber-100 flex items-center justify-center">
            <ShieldQuestion size={18} className="text-amber-600" />
          </div>
          <div>
            <p className="text-sm font-medium text-slate-500">{t("statPending")}</p>
            <p className="text-2xl font-bold text-slate-900">{isLoading ? "—" : pendingCount}</p>
          </div>
        </div>
        <div className="card p-5 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-emerald-100 flex items-center justify-center">
            <ShieldCheck size={18} className="text-emerald-600" />
          </div>
          <div>
            <p className="text-sm font-medium text-slate-500">{t("statVerified")}</p>
            <p className="text-2xl font-bold text-slate-900">{isLoading ? "—" : verifiedCount}</p>
          </div>
        </div>
      </div>

      {/* Tabs + search */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap gap-2">
          {TABS.map((s) => (
            <Button
              key={s}
              size="sm"
              variant={s === tab ? "primary" : "outline"}
              onClick={() => setTab(s)}
            >
              {t(`tab.${s}`)}
            </Button>
          ))}
        </div>
        <div className="relative w-full sm:w-64">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder={t("searchPlaceholder")}
            className="w-full h-10 pl-9 pr-3 rounded-xl border border-slate-300 text-sm outline-none focus:ring-2 focus:ring-brand-500"
          />
        </div>
      </div>

      {/* List */}
      {isLoading ? (
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="card h-24 animate-pulse bg-slate-100" />
          ))}
        </div>
      ) : isError ? (
        <EmptyState
          icon={ShieldQuestion}
          message={error instanceof Error ? error.message : t("loadError")}
          variant="error"
        />
      ) : filtered.length === 0 ? (
        <EmptyState icon={Briefcase} message={t("noResults")} />
      ) : (
        <div className="space-y-3">
          {filtered.map((u) => (
            <div key={u.id} className="card p-4 flex items-center justify-between gap-4 flex-wrap">
              <div className="flex items-center gap-3 min-w-0">
                <Avatar avatarUrl={u.avatarUrl} name={`${u.firstName} ${u.lastName}`} size="md" />
                <div className="min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <p className="font-medium text-slate-900 truncate">
                      {u.businessName || `${u.firstName} ${u.lastName}`}
                    </p>
                    <Badge variant={u.businessVerified ? "green" : "yellow"}>
                      {u.businessVerified ? tUsers("businessVerifiedBadge") : tUsers("businessPendingBadge")}
                    </Badge>
                  </div>
                  <p className="text-sm text-slate-500 truncate">{u.email}</p>
                  <p className="text-xs text-slate-400 mt-0.5">
                    {t("joinedLabel", { date: formatDate(u.createdAt, locale, "dd MMM yyyy") })}
                    {" · "}
                    {tUsers("bikesCount", { count: u.bikeCount })}
                    {" · "}
                    {tUsers("bookingsCount", { count: u.bookingCount })}
                  </p>
                </div>
              </div>

              <div>
                {u.businessVerified ? (
                  <Button
                    size="sm"
                    variant="outline"
                    loading={unverifying}
                    onClick={() => unverifyBusiness(u.id)}
                    className="flex items-center gap-1.5"
                  >
                    <BadgeX size={14} />
                    {tUsers("unverifyBusiness")}
                  </Button>
                ) : (
                  <Button
                    size="sm"
                    variant="primary"
                    loading={verifying}
                    onClick={() => verifyBusiness(u.id)}
                    className="flex items-center gap-1.5"
                  >
                    <BadgeCheck size={14} />
                    {tUsers("verifyBusiness")}
                  </Button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
