"use client";

import { useTranslations } from "next-intl";
import { Search, SlidersHorizontal } from "lucide-react";
import { useForm } from "react-hook-form";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import type { BikeCategory } from "@/types";

export interface SearchFilters {
  city: string;
  category: BikeCategory | "";
  model: string;
  minPrice: string;
  maxPrice: string;
}

interface SearchBarProps {
  defaultValues?: Partial<SearchFilters>;
  onSearch: (filters: SearchFilters) => void;
}

const CATEGORIES: BikeCategory[] = [
  "CITY", "MOUNTAIN", "ROAD", "ELECTRIC", "HYBRID", "CARGO", "KIDS",
];

/**
 * Bike search bar with category + price filters.
 * Fahrrad-Suchleiste mit Kategorie- und Preisfiltern.
 */
export function SearchBar({ defaultValues, onSearch }: SearchBarProps) {
  const t = useTranslations("bikes.search");
  const tc = useTranslations("bikes.categories");

  const { register, handleSubmit, reset } = useForm<SearchFilters>({
    defaultValues: {
      city: "",
      category: "",
      model: "",
      minPrice: "",
      maxPrice: "",
      ...defaultValues,
    },
  });

  return (
    <form
      onSubmit={handleSubmit(onSearch)}
      className="card p-4 md:p-6"
    >
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
        {/* City search */}
        <div className="relative lg:col-span-1">
          <Search
            size={16}
            className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none"
          />
          <input
            className="w-full h-10 pl-9 pr-3 rounded-xl border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 dark:placeholder:text-slate-500 text-sm outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500"
            placeholder={t("placeholder")}
            {...register("city")}
          />
        </div>

        {/* Model / brand search */}
        <input
          className="h-10 w-full px-3 rounded-xl border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 dark:placeholder:text-slate-500 text-sm outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500"
          placeholder={t("modelPlaceholder")}
          {...register("model")}
        />

        {/* Category */}
        <select
          className="h-10 px-3 rounded-xl border border-slate-300 dark:border-slate-600 text-sm bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 outline-none focus:ring-2 focus:ring-brand-500"
          {...register("category")}
        >
          <option value="">{t("allCategories")}</option>
          {CATEGORIES.map((cat) => (
            <option key={cat} value={cat}>
              {tc(cat)}
            </option>
          ))}
        </select>

        {/* Price range */}
        <div className="flex items-center gap-2">
          <input
            type="number"
            min={0}
            className="h-10 w-full px-3 rounded-xl border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 dark:placeholder:text-slate-500 text-sm outline-none focus:ring-2 focus:ring-brand-500"
            placeholder={t("minPrice")}
            {...register("minPrice")}
          />
          <span className="text-slate-400 text-sm shrink-0">–</span>
          <input
            type="number"
            min={0}
            className="h-10 w-full px-3 rounded-xl border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 dark:placeholder:text-slate-500 text-sm outline-none focus:ring-2 focus:ring-brand-500"
            placeholder={t("maxPrice")}
            {...register("maxPrice")}
          />
        </div>

        {/* Submit */}
        <div className="flex gap-2">
          <Button type="submit" className="flex-1">
            <Search size={16} />
            {t("placeholder").split(" ")[0]}
          </Button>
          <Button
            type="button"
            variant="outline"
            onClick={() => {
              reset();
              onSearch({ city: "", category: "", model: "", minPrice: "", maxPrice: "" });
            }}
          >
            <SlidersHorizontal size={16} />
          </Button>
        </div>
      </div>
    </form>
  );
}
