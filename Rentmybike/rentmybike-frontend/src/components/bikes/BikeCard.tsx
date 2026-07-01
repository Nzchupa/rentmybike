import { useState } from "react";
import Link from "next/link";
import Image from "next/image";
import { useLocale, useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { MapPin, ShieldCheck } from "lucide-react";
import { StarRating } from "@/components/ui/StarRating";
import { formatPrice, cn, optimizedImageUrl } from "@/lib/utils";
import { reviewsApi } from "@/lib/api";
import { FavoriteButton } from "@/components/bikes/FavoriteButton";
import { BikeImageFallback } from "@/components/bikes/BikeImageFallback";
import type { BikeResponse } from "@/types";

interface BikeCardProps {
  bike: BikeResponse;
}

/**
 * Bike listing card — used on search results and home page.
 * Fahrrad-Listenkarte — verwendet auf Suchergebnissen und Startseite.
 */
export function BikeCard({ bike }: BikeCardProps) {
  const locale = useLocale();
  const t = useTranslations("bikes.card");
  const tc = useTranslations("bikes.categories");

  // Previously StarRating below was hardcoded to 0 for every card, even
  // though GET /api/v1/reviews/bike/{id}/rating already existed and was
  // used elsewhere — every bike looked unrated regardless of real reviews.
  //
  // Vorher war die StarRating unten für jede Karte fest auf 0 gesetzt,
  // obwohl GET /api/v1/reviews/bike/{id}/rating bereits existierte und an
  // anderer Stelle verwendet wurde — jedes Fahrrad wirkte unbewertet,
  // unabhängig von echten Bewertungen.
  const { data: rating } = useQuery({
    queryKey: ["bike-rating", bike.id],
    queryFn: () => reviewsApi.getBikeRating(bike.id),
    select: (r) => r.data.data,
    staleTime: 5 * 60 * 1000,
  });

  // Track image load state so we can show a skeleton while the photo loads
  // instead of a flash of empty grey, and fall back cleanly on a load error
  // (e.g. broken/expired upload URL) rather than leaving a dead <img>.
  const [imageLoaded, setImageLoaded] = useState(false);
  const [imageErrored, setImageErrored] = useState(false);
  const hasPhoto = Boolean(bike.primaryPhotoUrl) && !imageErrored;

  return (
    <Link href={`/${locale}/bikes/${bike.id}`} className="group block">
      <div className="card overflow-hidden transition-shadow hover:shadow-md">
        {/* Photo — fixed aspect ratio (h-48) kept consistent across every card */}
        <div className="relative h-48 bg-slate-100 overflow-hidden">
          {hasPhoto ? (
            <>
              {!imageLoaded && (
                <div className="absolute inset-0 animate-pulse bg-slate-200" />
              )}
              <Image
                src={optimizedImageUrl(bike.primaryPhotoUrl!, 640)}
                alt={bike.title}
                fill
                sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 33vw"
                className={cn(
                  "object-cover transition-all duration-300 group-hover:scale-105",
                  imageLoaded ? "opacity-100" : "opacity-0"
                )}
                onLoad={() => setImageLoaded(true)}
                onError={() => setImageErrored(true)}
              />
            </>
          ) : (
            <BikeImageFallback />
          )}

          {/* Category badge */}
          <div className="absolute top-3 left-3 rounded-full bg-white/90 backdrop-blur px-2.5 py-1 text-xs font-medium text-slate-700 shadow">
            {tc(bike.category)}
          </div>

          {/* Favorite toggle */}
          <FavoriteButton bikeId={bike.id} className="absolute top-3 right-3" />
        </div>

        {/* Info */}
        <div className="p-4">
          <h3 className="font-semibold text-slate-900 mb-1 line-clamp-1">{bike.title}</h3>
          {bike.model && (
            <p className="text-xs text-slate-500 mb-1 line-clamp-1">{bike.model}</p>
          )}

          <div className="flex items-center gap-1 text-sm text-slate-500 mb-2">
            <MapPin size={14} className="shrink-0" />
            <span className="line-clamp-1">{bike.city}</span>
          </div>

          {/* Owner-verified-business badge — bike.ownerBusinessVerified is
              now actually populated by the backend (see BikeService.
              toBikeResponse); this was already read on the bike detail page
              but never surfaced on the card itself. */}
          {bike.ownerBusinessVerified && (
            <div className="flex items-center gap-1 text-xs font-medium text-emerald-700 mb-2">
              <ShieldCheck size={12} className="shrink-0" />
              {t("verifiedShop")}
            </div>
          )}

          <div className="flex items-center justify-between">
            <div>
              <span className="text-lg font-bold text-slate-900">
                {formatPrice(bike.pricePerDay, locale)}
              </span>
              <span className="text-sm text-slate-500"> {t("perDay")}</span>
            </div>

            <StarRating rating={rating?.averageRating ?? 0} size="sm" />
          </div>
        </div>
      </div>
    </Link>
  );
}
