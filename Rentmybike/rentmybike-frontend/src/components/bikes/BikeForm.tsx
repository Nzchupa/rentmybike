"use client";

import { useTranslations } from "next-intl";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Info } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import type { BikeResponse, BikeCategory } from "@/types";

const CATEGORIES: BikeCategory[] = [
  "CITY", "MOUNTAIN", "ROAD", "ELECTRIC", "HYBRID", "CARGO", "KIDS",
];

const bikeSchema = z.object({
  title:       z.string().min(5, "Min 5 chars / Min. 5 Zeichen").max(100),
  description: z.string().min(20, "Min 20 chars / Min. 20 Zeichen").max(3000),
  category:    z.enum(["CITY", "MOUNTAIN", "ROAD", "ELECTRIC", "HYBRID", "CARGO", "KIDS"]),
  pricePerDay: z.coerce.number().min(1, "Min €1"),
  city:        z.string().min(2, "City required / Stadt erforderlich").max(100),
  address:     z.string().max(255).optional(),
  available:   z.boolean(),
});

export type BikeFormValues = z.infer<typeof bikeSchema>;

interface BikeFormProps {
  defaultValues?: Partial<BikeFormValues>;
  existingBike?: BikeResponse;
  onSubmit: (values: BikeFormValues) => Promise<void>;
  isEditing?: boolean;
}

/**
 * Reusable bike create/edit form.
 * Wiederverwendbares Fahrrad-Erstellen/Bearbeiten-Formular.
 */
export function BikeForm({ defaultValues, existingBike, onSubmit, isEditing }: BikeFormProps) {
  const t = useTranslations("dashboard.bikeForm");
  const tc = useTranslations("bikes.categories");

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<BikeFormValues>({
    resolver: zodResolver(bikeSchema),
    defaultValues: {
      available: true,
      ...defaultValues,
    },
  });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
      <Input
        label={t("title")}
        placeholder={t("titlePlaceholder")}
        error={errors.title?.message}
        {...register("title")}
      />

      <div>
        <label className="label">{t("description")}</label>
        <textarea
          rows={5}
          placeholder={t("descriptionPlaceholder")}
          className={`w-full rounded-xl border px-3 py-2 text-sm resize-none outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500 ${
            errors.description ? "border-red-400" : "border-slate-300"
          }`}
          {...register("description")}
        />
        {errors.description && (
          <p className="field-error">{errors.description.message}</p>
        )}
      </div>

      <div>
        <label className="label">{t("category")}</label>
        <select
          className={`w-full h-10 px-3 rounded-xl border bg-white text-sm outline-none focus:ring-2 focus:ring-brand-500 ${
            errors.category ? "border-red-400" : "border-slate-300"
          }`}
          {...register("category")}
        >
          <option value="">Select category / Kategorie wählen</option>
          {CATEGORIES.map((cat) => (
            <option key={cat} value={cat}>{tc(cat)}</option>
          ))}
        </select>
        {errors.category && (
          <p className="field-error">{errors.category.message}</p>
        )}
      </div>

      <div className="grid grid-cols-2 gap-4">
        <Input
          label={t("pricePerDay")}
          type="number"
          min={1}
          step={0.5}
          placeholder="25"
          error={errors.pricePerDay?.message}
          {...register("pricePerDay")}
        />
        <Input
          label={t("city")}
          placeholder="Berlin"
          error={errors.city?.message}
          {...register("city")}
        />
      </div>

      <Input
        label={t("address")}
        placeholder="Alexanderplatz 1 (optional)"
        error={errors.address?.message}
        {...register("address")}
      />

      {isEditing && (
        <div className="flex items-center gap-3">
          <input
            type="checkbox"
            id="available"
            className="w-4 h-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500"
            {...register("available")}
          />
          <label htmlFor="available" className="text-sm font-medium text-slate-700">
            {t("available")}
          </label>
        </div>
      )}

      {/* Info banner */}
      <div className="flex items-start gap-2 rounded-xl bg-blue-50 p-3 text-sm text-blue-700">
        <Info size={16} className="shrink-0 mt-0.5" />
        <p>{isEditing ? t("hints.resetToApproval") : t("hints.pending")}</p>
      </div>

      <Button type="submit" loading={isSubmitting} className="w-full" size="lg">
        {t("submit")}
      </Button>
    </form>
  );
}
