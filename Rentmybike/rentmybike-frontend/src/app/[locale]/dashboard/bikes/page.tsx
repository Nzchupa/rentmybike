"use client";

import Link from "next/link";
import Image from "next/image";
import { useLocale, useTranslations } from "next-intl";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Pencil, Trash2, Camera, Bike as BikeIcon } from "lucide-react";
import toast from "react-hot-toast";
import { bikesApi } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { ApprovalStatusBadge } from "@/components/ui/Badge";
import { BikeImageFallback } from "@/components/bikes/BikeImageFallback";
import { EmptyState } from "@/components/ui/EmptyState";
import { formatPrice } from "@/lib/utils";

/**
 * My Bikes dashboard page — list, delete, navigate to edit/add.
 * Meine Fahrräder Dashboard-Seite — Liste, Löschen, zu Bearbeiten/Hinzufügen navigieren.
 */
export default function MyBikesPage() {
  const t = useTranslations("dashboard.bikes");
  const tCommon = useTranslations("common");
  const locale = useLocale();
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ["my-bikes"],
    queryFn: () => bikesApi.getMyBikes(0, 50),
    select: (r) => r.data.data,
  });

  const { mutate: deleteBike } = useMutation({
    mutationFn: bikesApi.delete,
    onSuccess: () => {
      toast.success(t("bikeDeleted"));
      queryClient.invalidateQueries({ queryKey: ["my-bikes"] });
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const bikes = data?.content ?? [];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="section-title">{t("title")}</h1>
        <Button asChild>
          <Link href={`/${locale}/dashboard/bikes/new`}>
            <Plus size={16} />
            {t("addBike")}
          </Link>
        </Button>
      </div>

      {isLoading ? (
        <div className="space-y-4">
          {[1, 2, 3].map((i) => (
            <div key={i} className="card h-24 animate-pulse bg-slate-100" />
          ))}
        </div>
      ) : bikes.length === 0 ? (
        <EmptyState
          icon={BikeIcon}
          message={t("noBikes")}
          action={{ label: t("addBike"), href: `/${locale}/dashboard/bikes/new` }}
        />
      ) : (
        <div className="space-y-3">
          {bikes.map((bike) => (
            <div key={bike.id} className="card p-4 flex items-center gap-4">
              {/* Photo + Info — link to the bike's public listing page */}
              <Link
                href={`/${locale}/bikes/${bike.id}`}
                className="flex items-center gap-4 flex-1 min-w-0 hover:opacity-80"
              >
                <div className="relative w-16 h-16 rounded-xl overflow-hidden bg-slate-100 shrink-0">
                  {bike.primaryPhotoUrl ? (
                    <Image
                      src={bike.primaryPhotoUrl}
                      alt={bike.title}
                      fill
                      className="object-cover"
                      sizes="64px"
                    />
                  ) : (
                    <BikeImageFallback iconSize={24} />
                  )}
                </div>

                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-0.5">
                    <span className="font-semibold text-slate-900 line-clamp-1">
                      {bike.title}
                    </span>
                    <ApprovalStatusBadge status={bike.approvalStatus} />
                  </div>
                  <p className="text-sm text-slate-500">
                    {bike.city} · {formatPrice(bike.pricePerDay)}{tCommon("perDay")}
                  </p>
                  {bike.approvalStatus === "REJECTED" && bike.rejectionReason && (
                    <p className="text-xs text-red-600 mt-1 line-clamp-1">
                      ✗ {bike.rejectionReason}
                    </p>
                  )}
                </div>
              </Link>

              {/* Actions */}
              <div className="flex items-center gap-2 shrink-0">
                <Button
                  variant="ghost"
                  size="sm"
                  asChild
                  title={t("photos")}
                >
                  <Link href={`/${locale}/dashboard/bikes/${bike.id}/photos`}>
                    <Camera size={16} />
                  </Link>
                </Button>
                <Button variant="ghost" size="sm" asChild title={t("edit")}>
                  <Link href={`/${locale}/dashboard/bikes/${bike.id}/edit`}>
                    <Pencil size={16} />
                  </Link>
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  className="text-red-500 hover:text-red-600 hover:bg-red-50"
                  title={t("delete")}
                  onClick={() => {
                    if (confirm(t("deleteConfirm"))) deleteBike(bike.id);
                  }}
                >
                  <Trash2 size={16} />
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
