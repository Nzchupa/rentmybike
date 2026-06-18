"use client";

import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { bikesApi } from "@/lib/api";
import { BikeCard } from "@/components/bikes/BikeCard";

/**
 * Shows a handful of real, currently-listed bikes on the homepage.
 * Previously visitors had no way to see any actual bikes without
 * navigating away from the homepage first — this was the biggest
 * gap for conversion.
 *
 * Zeigt einige echte, aktuell gelistete Fahrräder auf der Startseite.
 */
export function PopularBikes() {
  const t = useTranslations("home.popularBikes");
  const locale = useLocale();

  const { data, isLoading } = useQuery({
    queryKey: ["home-popular-bikes"],
    queryFn: () => bikesApi.search({ page: 0, size: 6 }),
    select: (r) => r.data.data.content,
    staleTime: 60 * 1000,
  });

  return (
    <section className="py-20 bg-white">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex items-end justify-between flex-wrap gap-4 mb-10">
          <div>
            <h2 className="text-3xl font-bold text-slate-900 mb-2">{t("title")}</h2>
            <p className="text-slate-600">{t("subtitle")}</p>
          </div>
          <Link href={`/${locale}/bikes`} className="text-brand-600 font-medium hover:text-brand-700 transition-colors">
            {t("viewAll")} →
          </Link>
        </div>

        {isLoading ? (
          <div className="grid md:grid-cols-3 gap-6">
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="card h-72 animate-pulse bg-slate-100" />
            ))}
          </div>
        ) : !data?.length ? (
          <p className="text-slate-500">{t("empty")}</p>
        ) : (
          <div className="grid md:grid-cols-3 gap-6">
            {data.slice(0, 6).map((bike) => (
              <BikeCard key={bike.id} bike={bike} />
            ))}
          </div>
        )}
      </div>
    </section>
  );
}
