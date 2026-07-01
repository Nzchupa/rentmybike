"use client";

import { useLocale, useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { Heart } from "lucide-react";
import { favoritesApi } from "@/lib/api";
import { BikeCard } from "@/components/bikes/BikeCard";
import { EmptyState } from "@/components/ui/EmptyState";

/**
 * My Favorites dashboard page — bikes the current user has bookmarked.
 * Meine Favoriten Dashboard-Seite — vom aktuellen Benutzer favorisierte Fahrräder.
 */
export default function MyFavoritesPage() {
  const t = useTranslations("dashboard.favorites");
  const tDash = useTranslations("dashboard");
  const locale = useLocale();

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
            <div key={i} className="card h-72 animate-pulse bg-slate-100 dark:bg-slate-700" />
          ))}
        </div>
      ) : bikes.length === 0 ? (
        <EmptyState
          icon={Heart}
          message={t("empty")}
          action={{ label: tDash("browseBikes"), href: `/${locale}/bikes` }}
        />
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
