"use client";

import { useState } from "react";
import Image from "next/image";
import { useTranslations } from "next-intl";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { CheckCircle, XCircle } from "lucide-react";
import toast from "react-hot-toast";
import { bikesApi } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { ApprovalStatusBadge } from "@/components/ui/Badge";
import { formatPrice } from "@/lib/utils";
import type { ApprovalStatus } from "@/types";

const STATUS_TABS: (ApprovalStatus | "ALL")[] = ["ALL", "PENDING", "APPROVED", "REJECTED"];

/**
 * Admin bike moderation page.
 * Admin-Fahrrad-Moderationsseite.
 */
export default function AdminBikesPage() {
  const t = useTranslations("admin.bikes");
  const [filter, setFilter] = useState<ApprovalStatus | undefined>(undefined);
  const [rejectingId, setRejectingId] = useState<string | null>(null);
  const [rejectReason, setRejectReason] = useState("");
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ["admin-bikes", filter],
    queryFn: () => bikesApi.adminList(filter ?? undefined, 0, 50),
    select: (r) => r.data.data,
  });

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: ["admin-bikes"] });

  const { mutate: approve, isPending: approving } = useMutation({
    mutationFn: bikesApi.adminApprove,
    onSuccess: () => { toast.success("Bike approved"); invalidate(); },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutate: reject, isPending: rejecting } = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) =>
      bikesApi.adminReject(id, reason),
    onSuccess: () => {
      toast.success("Bike rejected");
      setRejectingId(null);
      setRejectReason("");
      invalidate();
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const bikes = data?.content ?? [];

  return (
    <div className="space-y-6">
      <h1 className="section-title">{t("title")}</h1>

      {/* Status filter tabs */}
      <div className="flex flex-wrap gap-2">
        {STATUS_TABS.map((s) => (
          <Button
            key={s}
            size="sm"
            variant={
              (s === "ALL" && !filter) || s === filter ? "primary" : "outline"
            }
            onClick={() => setFilter(s === "ALL" ? undefined : (s as ApprovalStatus))}
          >
            {s}
          </Button>
        ))}
      </div>

      {/* Bike list */}
      {isLoading ? (
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="card h-28 animate-pulse bg-slate-100" />
          ))}
        </div>
      ) : bikes.length === 0 ? (
        <div className="card p-12 text-center text-slate-500">
          No bikes found. / Keine Fahrräder gefunden.
        </div>
      ) : (
        <div className="space-y-3">
          {bikes.map((bike) => (
            <div key={bike.id} className="card p-4">
              <div className="flex gap-4">
                {/* Thumbnail */}
                <div className="w-20 h-20 shrink-0 rounded-xl overflow-hidden bg-slate-100">
                  {bike.photos?.[0] ? (
                    <Image
                      src={bike.photos[0].url}
                      alt={bike.title}
                      width={80}
                      height={80}
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center text-slate-400 text-xs">
                      No photo
                    </div>
                  )}
                </div>

                {/* Info */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <p className="font-semibold text-slate-900 truncate">{bike.title}</p>
                      <p className="text-sm text-slate-500">
                        {bike.city} · {bike.category} · {formatPrice(bike.pricePerDay)}/day
                      </p>
                      <p className="text-xs text-slate-400 mt-0.5">
                        Owner: {bike.ownerName}
                      </p>
                    </div>
                    <ApprovalStatusBadge status={bike.approvalStatus} />
                  </div>

                  {/* Actions */}
                  {bike.approvalStatus === "PENDING" && (
                    <div className="flex gap-2 mt-3">
                      <Button
                        size="sm"
                        variant="primary"
                        loading={approving}
                        onClick={() => approve(bike.id)}
                        className="flex items-center gap-1.5"
                      >
                        <CheckCircle size={14} />
                        {t("approve")}
                      </Button>
                      <Button
                        size="sm"
                        variant="danger"
                        onClick={() => {
                          setRejectingId(bike.id);
                          setRejectReason("");
                        }}
                        className="flex items-center gap-1.5"
                      >
                        <XCircle size={14} />
                        {t("reject")}
                      </Button>
                    </div>
                  )}

                  {/* Reject reason inline form */}
                  {rejectingId === bike.id && (
                    <div className="mt-3 flex gap-2 items-center">
                      <input
                        value={rejectReason}
                        onChange={(e) => setRejectReason(e.target.value)}
                        placeholder={t("rejectReasonPlaceholder")}
                        className="flex-1 h-9 px-3 rounded-xl border border-slate-300 text-sm outline-none focus:ring-2 focus:ring-brand-500"
                      />
                      <Button
                        size="sm"
                        variant="danger"
                        loading={rejecting}
                        disabled={!rejectReason.trim()}
                        onClick={() =>
                          reject({ id: bike.id, reason: rejectReason.trim() })
                        }
                      >
                        Confirm reject
                      </Button>
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => setRejectingId(null)}
                      >
                        Cancel
                      </Button>
                    </div>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
