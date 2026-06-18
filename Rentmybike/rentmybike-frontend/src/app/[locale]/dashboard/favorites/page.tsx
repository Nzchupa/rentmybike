"use client";

import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { Heart } from "lucide-react";
import { favoritesApi } from "@/lib/api";
import { BikeCard } from "@/components/bikes/BikeCard";

/**
 * My Favorites dashboard page — bikes the current user has bookmarked.
 * Meine Favoriten Dashboard-Seite — vom aktuellen Benutzer favorisierte Fahrräder.
 */
export default function MyFavoritesPage() {
  const t = useTranslations("dashboard.favorites");

  const { data, isLoading } = useQuery({
    queryKey: ["my-favorites"],
    queryFn: () => favoritesApi.list(0, 50),
    select: (r) => r.data.data,
  });

  const bikes = data?.content ?? [];

  return (
    <div className="space-y-6">
      <h1 className="section-title">{t("title")}</h1>

      {isLoading ? (
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {[1, 2, 3].map((i) => (
            <div key={i} className="card h-72 animate-pulse bg-slate-100" />
          ))}
        </div>
      ) : bikes.length === 0 ? (
        <div className="card p-12 text-center text-slate-500">
          <Heart size={32} className="mx-auto mb-3 text-slate-300" />
          <p>{t("empty")}</p>
        </div>
      ) : (
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {bikes.map((bike) => (
            <BikeCard key={bike.id} bike={bike} />
          ))}
        </div>
      )}
    </div>
  );
}
