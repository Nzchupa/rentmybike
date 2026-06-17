import Link from "next/link";
import Image from "next/image";
import { useLocale, useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { MapPin } from "lucide-react";
import { StarRating } from "@/components/ui/StarRating";
import { formatPrice } from "@/lib/utils";
import { reviewsApi } from "@/lib/api";
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

  return (
    <Link href={`/${locale}/bikes/${bike.id}`} className="group block">
      <div className="card overflow-hidden transition-shadow hover:shadow-md">
        {/* Photo */}
        <div className="relative h-48 bg-slate-100 overflow-hidden">
          {bike.primaryPhotoUrl ? (
            <Image
              src={bike.primaryPhotoUrl}
              alt={bike.title}
              fill
              sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 33vw"
              className="object-cover group-hover:scale-105 transition-transform duration-300"
            />
          ) : (
            <div className="flex h-full items-center justify-center text-slate-300">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <circle cx="5.5" cy="17.5" r="3.5"/>
                <circle cx="18.5" cy="17.5" r="3.5"/>
                <path d="M5.5 17.5L8 10l4 4 2-6h2.5"/>
                <path d="M15 10h3l1 2"/>
              </svg>
            </div>
          )}

          {/* Category badge */}
          <div className="absolute top-3 left-3 rounded-full bg-white/90 backdrop-blur px-2.5 py-1 text-xs font-medium text-slate-700 shadow">
            {tc(bike.category)}
          </div>
        </div>

        {/* Info */}
        <div className="p-4">
          <h3 className="font-semibold text-slate-900 mb-1 line-clamp-1">{bike.title}</h3>

          <div className="flex items-center gap-1 text-sm text-slate-500 mb-3">
            <MapPin size={14} className="shrink-0" />
            <span className="line-clamp-1">{bike.city}</span>
          </div>

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
