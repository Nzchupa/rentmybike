"use client";

import { useState, useCallback } from "react";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { bikesApi } from "@/lib/api";
import { BikeCard } from "@/components/bikes/BikeCard";
import { SearchBar, type SearchFilters } from "@/components/bikes/SearchBar";
import { Button } from "@/components/ui/Button";
import type { BikeCategory } from "@/types";

const PAGE_SIZE = 12;

/**
 * Public bike search / listing page.
 * Öffentliche Fahrrad-Such-/Listenseite.
 */
export default function BikesPage() {
  const t = useTranslations("bikes");
  const tc = useTranslations("common");

  const [filters, setFilters] = useState<SearchFilters>({
    city: "",
    category: "",
    model: "",
    minPrice: "",
    maxPrice: "",
  });
  const [page, setPage] = useState(0);

  // isError was previously never read here, so a failed search request (e.g. a
  // transient 500) rendered identically to "no bikes match these filters" — the
  // empty-state message — instead of surfacing the real problem. This mirrors
  // the same fix already applied to the admin users page.
  // isError wurde hier vorher nie ausgelesen, daher sah eine fehlgeschlagene
  // Suchanfrage (z. B. ein vorübergehender 500er) exakt so aus wie "keine
  // Fahrräder passen zu diesen Filtern" — die Leerzustand-Meldung — statt das
  // eigentliche Problem offenzulegen. Entspricht der bereits auf der
  // Admin-Benutzerseite angewendeten Korrektur.
  const { data, isLoading, isFetching, isError, error } = useQuery({
    queryKey: ["bikes", filters, page],
    queryFn: () =>
      bikesApi.search({
        city: filters.city || undefined,
        category: (filters.category as BikeCategory) || undefined,
        model: filters.model || undefined,
        minPrice: filters.minPrice ? Number(filters.minPrice) : undefined,
        maxPrice: filters.maxPrice ? Number(filters.maxPrice) : undefined,
        page,
        size: PAGE_SIZE,
      }),
    select: (res) => res.data.data,
  });

  const handleSearch = useCallback((f: SearchFilters) => {
    setFilters(f);
    setPage(0);
  }, []);

  const bikes = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  return (
    <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-10">
      {/* This used to render t("search.placeholder") here — that key is the
          SearchBar's city-input placeholder text ("City or location..."),
          not a page title, so the heading literally showed the words
          "City or location..." / "Stadt oder Ort...". Added a real
          bikes.title key instead. */}
      {/* Hier wurde vorher t("search.placeholder") gerendert — dieser
          Schlüssel ist der Platzhaltertext des Stadt-Eingabefelds der
          SearchBar ("Stadt oder Ort..."), keine Seitenüberschrift, daher
          zeigte die Überschrift wörtlich "Stadt oder Ort...". Stattdessen
          einen echten bikes.title-Schlüssel hinzugefügt. */}
      <h1 className="section-title mb-6">{t("title")}</h1>

      <SearchBar onSearch={handleSearch} />

      <div className="mt-8">
        {isLoading || isFetching ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {Array.from({ length: 8 }).map((_, i) => (
              <div key={i} className="card h-60 animate-pulse bg-slate-100 dark:bg-slate-700" />
            ))}
          </div>
        ) : isError ? (
          <div className="text-center py-20 text-red-600 dark:text-red-400">
            <p className="text-lg">
              {error instanceof Error ? error.message : tc("noResults")}
            </p>
          </div>
        ) : bikes.length === 0 ? (
          <div className="text-center py-20 text-slate-500 dark:text-slate-400">
            <p className="text-lg">{tc("noResults")}</p>
          </div>
        ) : (
          <>
            {/* "{count} bikes found" was hardcoded in English regardless of
                locale — the whole rest of the page is translated, so on the
                German site this line alone stayed in English. */}
            {/* "{count} bikes found" war unabhängig von der Sprache fest auf
                Englisch codiert — der Rest der Seite ist übersetzt, daher
                blieb diese Zeile auf der deutschen Seite allein auf
                Englisch. */}
            <p className="text-sm text-slate-500 dark:text-slate-400 mb-4">
              {t("resultsCount", { count: data?.totalElements ?? 0 })}
            </p>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
              {bikes.map((bike) => (
                <BikeCard key={bike.id} bike={bike} />
              ))}
            </div>

            {/* Pagination — "Previous"/"Next" were likewise hardcoded in
                English; common.previous/common.next already exist and are
                used the same way elsewhere (e.g. admin pages). */}
            {/* Paginierung — "Previous"/"Next" waren ebenfalls fest auf
                Englisch codiert; common.previous/common.next existieren
                bereits und werden andernorts (z. B. Admin-Seiten) genauso
                verwendet. */}
            {totalPages > 1 && (
              <div className="mt-10 flex justify-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page === 0}
                  onClick={() => setPage((p) => p - 1)}
                >
                  {tc("previous")}
                </Button>
                <span className="flex items-center px-4 text-sm text-slate-600 dark:text-slate-300">
                  {page + 1} / {totalPages}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage((p) => p + 1)}
                >
                  {tc("next")}
                </Button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
