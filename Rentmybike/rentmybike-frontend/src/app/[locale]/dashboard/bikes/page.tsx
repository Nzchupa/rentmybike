"use client";

import Link from "next/link";
import Image from "next/image";
import { useLocale, useTranslations } from "next-intl";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Pencil, Trash2, Camera, Bike as BikeIcon, Eye } from "lucide-react";
import toast from "react-hot-toast";
import { bikesApi } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { ApprovalStatusBadge } from "@/components/ui/Badge";
import { BikeImageFallback } from "@/components/bikes/BikeImageFallback";
import { EmptyState } from "@/components/ui/EmptyState";
import { formatPrice, cn, optimizedImageUrl } from "@/lib/utils";
import type { BikeResponse } from "@/types";

interface BikeManageCardProps {
  bike: BikeResponse;
  locale: string;
}

// One card per bike — photo, status, a quick availability toggle, and the
// manage actions (photos/edit/delete). Previously this page rendered bikes
// as flat list rows, which buried the photo and made it hard to scan a
// catalog of more than a handful of bikes (a real pain point for BUSINESS
// accounts managing dozens of listings). The grid-of-cards layout mirrors
// the public BikeCard look so "my bikes" reads as a gallery, not a table.
//
// Eine Karte pro Fahrrad — Foto, Status, ein schneller
// Verfügbarkeits-Umschalter und die Verwaltungsaktionen
// (Fotos/Bearbeiten/Löschen). Vorher wurden Fahrräder als flache
// Listenzeilen dargestellt, was das Foto in den Hintergrund drängte und das
// Durchsuchen eines größeren Katalogs erschwerte (besonders relevant für
// BUSINESS-Konten mit vielen Inseraten). Das Karten-Raster orientiert sich
// am öffentlichen BikeCard-Look, sodass "Meine Fahrräder" wie eine Galerie
// wirkt statt wie eine Tabelle.
function BikeManageCard({ bike, locale }: BikeManageCardProps) {
  const t = useTranslations("dashboard.bikes");
  const queryClient = useQueryClient();

  const { mutate: deleteBike } = useMutation({
    mutationFn: bikesApi.delete,
    onSuccess: () => {
      toast.success(t("bikeDeleted"));
      queryClient.invalidateQueries({ queryKey: ["my-bikes"] });
    },
    onError: (e: Error) => toast.error(e.message),
  });

  // Quick availability toggle without leaving this page or opening the full
  // edit form. UpdateBikeRequest requires the whole payload (no PATCH
  // endpoint exists for a single field), so we resend the bike's current
  // fields with only `available` flipped.
  //
  // Schneller Verfügbarkeits-Umschalter, ohne diese Seite zu verlassen oder
  // das vollständige Bearbeitungsformular zu öffnen. UpdateBikeRequest
  // benötigt den gesamten Payload (es gibt keinen PATCH-Endpunkt für ein
  // einzelnes Feld), daher senden wir die aktuellen Felder erneut, nur
  // `available` wird umgekehrt.
  const { mutate: toggleAvailability, isPending: togglingAvailability } = useMutation({
    mutationFn: () =>
      bikesApi.update(bike.id, {
        title: bike.title,
        description: bike.description,
        category: bike.category,
        pricePerDay: bike.pricePerDay,
        city: bike.city,
        address: bike.address ?? undefined,
        latitude: bike.latitude ?? undefined,
        longitude: bike.longitude ?? undefined,
        available: !bike.available,
      }),
    onSuccess: () => {
      toast.success(t("availabilityUpdated"));
      queryClient.invalidateQueries({ queryKey: ["my-bikes"] });
    },
    onError: (e: Error) => toast.error(e.message),
  });

  return (
    <div className="card overflow-hidden flex flex-col">
      {/* Photo */}
      <Link
        href={`/${locale}/bikes/${bike.id}`}
        className="relative block h-44 bg-slate-100 overflow-hidden shrink-0"
      >
        {bike.primaryPhotoUrl ? (
          <Image
            src={optimizedImageUrl(bike.primaryPhotoUrl, 640)}
            alt={bike.title}
            fill
            sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 33vw"
            className="object-cover"
          />
        ) : (
          <BikeImageFallback iconSize={32} />
        )}

        <div className="absolute top-3 left-3">
          <ApprovalStatusBadge status={bike.approvalStatus} />
        </div>

        <div className="absolute bottom-3 left-3 flex items-center gap-1 rounded-full bg-white/90 backdrop-blur px-2.5 py-1 text-xs font-medium text-slate-700 shadow">
          <Eye size={12} />
          {t("viewsLabel", { count: bike.viewCount })}
        </div>
      </Link>

      {/* Info */}
      <div className="p-4 flex-1 flex flex-col gap-3">
        <div>
          <p className="font-semibold text-slate-900 line-clamp-1">{bike.title}</p>
          <p className="text-sm text-slate-500">
            {bike.city} · {formatPrice(bike.pricePerDay, locale)}{t("perDay")}
          </p>
        </div>

        {bike.approvalStatus === "REJECTED" && bike.rejectionReason && (
          <p className="text-xs text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-1.5 line-clamp-2">
            {bike.rejectionReason}
          </p>
        )}
        {bike.approvalStatus === "CHANGES_REQUESTED" && bike.rejectionReason && (
          <p className="text-xs text-blue-700 bg-blue-50 border border-blue-100 rounded-lg px-3 py-1.5 line-clamp-2">
            {bike.rejectionReason}
          </p>
        )}

        {/* Availability toggle */}
        <button
          type="button"
          disabled={togglingAvailability}
          onClick={() => toggleAvailability()}
          className={cn(
            "inline-flex items-center gap-1.5 self-start rounded-full px-3 py-1 text-xs font-medium transition-colors disabled:opacity-60",
            bike.available
              ? "bg-emerald-50 text-emerald-700 hover:bg-emerald-100"
              : "bg-slate-100 text-slate-500 hover:bg-slate-200"
          )}
        >
          <span
            className={cn(
              "h-2 w-2 rounded-full",
              bike.available ? "bg-emerald-500" : "bg-slate-400"
            )}
          />
          {bike.available ? t("available") : t("unavailable")}
        </button>

        {/* Actions */}
        <div className="mt-auto flex items-center justify-end gap-1 pt-2 border-t border-slate-100">
          <Button variant="ghost" size="sm" asChild title={t("photos")}>
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
    </div>
  );
}

/**
 * My Bikes dashboard page — card grid with photo, status, a quick
 * availability toggle, and links to edit/photos/delete/add.
 * Meine Fahrräder Dashboard-Seite — Karten-Raster mit Foto, Status, einem
 * schnellen Verfügbarkeits-Umschalter und Links zu
 * Bearbeiten/Fotos/Löschen/Hinzufügen.
 */
export default function MyBikesPage() {
  const t = useTranslations("dashboard.bikes");
  const locale = useLocale();

  const { data, isLoading } = useQuery({
    queryKey: ["my-bikes"],
    queryFn: () => bikesApi.getMyBikes(0, 50),
    select: (r) => r.data.data,
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
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {[1, 2, 3].map((i) => (
            <div key={i} className="card h-80 animate-pulse bg-slate-100" />
          ))}
        </div>
      ) : bikes.length === 0 ? (
        <EmptyState
          icon={BikeIcon}
          message={t("noBikes")}
          action={{ label: t("addBike"), href: `/${locale}/dashboard/bikes/new` }}
        />
      ) : (
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {bikes.map((bike) => (
            <BikeManageCard key={bike.id} bike={bike} locale={locale} />
          ))}
        </div>
      )}
    </div>
  );
}
