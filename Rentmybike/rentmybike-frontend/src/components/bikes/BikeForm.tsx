"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Info } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { PhotoDropzone } from "@/components/bikes/PhotoDropzone";
import type { BikeResponse, BikeCategory } from "@/types";

const CATEGORIES: BikeCategory[] = [
  "CITY", "MOUNTAIN", "ROAD", "ELECTRIC", "HYBRID", "CARGO", "KIDS",
];

// Validation messages are resolved at submit-time via the translation
// function passed into makeBikeSchema(), so the error text follows the
// active locale instead of always showing both languages at once.
//
// Validierungsmeldungen werden zur Absendezeit über die in makeBikeSchema()
// übergebene Übersetzungsfunktion aufgelöst, sodass der Fehlertext der
// aktiven Sprache folgt, anstatt immer beide Sprachen gleichzeitig zu zeigen.
function makeBikeSchema(t: (key: string) => string) {
  return z.object({
    title:       z.string().min(5, t("validation.titleMin")).max(100),
    description: z.string().min(20, t("validation.descriptionMin")).max(3000),
    model:       z.string().max(150).optional(),
    category:    z.enum(["CITY", "MOUNTAIN", "ROAD", "ELECTRIC", "HYBRID", "CARGO", "KIDS"]),
    pricePerDay: z.coerce.number().min(1, t("validation.priceMin")),
    // Optional refundable deposit — coerced from the (possibly empty) text
    // input; an empty string coerces to NaN rather than 0, so it's mapped to
    // undefined first so "no deposit" round-trips cleanly instead of failing
    // min(0) validation.
    // Optionale rückzahlbare Kaution — aus dem (möglicherweise leeren)
    // Text-Input umgewandelt; ein leerer String wird zu NaN statt 0, daher
    // wird er zuerst auf undefined abgebildet, damit "keine Kaution" sauber
    // durchläuft, statt an der min(0)-Validierung zu scheitern.
    depositAmount: z.preprocess(
      (val) => (val === "" || val === undefined ? undefined : Number(val)),
      z.number().min(0, t("validation.depositMin")).optional()
    ),
    city:        z.string().min(2, t("validation.cityRequired")).max(100),
    address:     z.string().max(255).optional(),
    available:   z.boolean(),
  });
}

export type BikeFormValues = z.infer<ReturnType<typeof makeBikeSchema>>;

interface BikeFormProps {
  defaultValues?: Partial<BikeFormValues>;
  existingBike?: BikeResponse;
  // `photos` is only populated when creating a new bike (see PhotoDropzone
  // below) — editing reuses the existing dedicated photo management page,
  // so the second argument is simply unused there.
  // `photos` wird nur beim Erstellen eines neuen Fahrrads befüllt (siehe
  // PhotoDropzone unten) — die Bearbeitung nutzt weiterhin die bestehende,
  // dedizierte Fotoverwaltungsseite, daher bleibt das zweite Argument dort
  // ungenutzt.
  onSubmit: (values: BikeFormValues, photos: File[]) => Promise<void>;
  isEditing?: boolean;
}

/**
 * Reusable bike create/edit form.
 * Wiederverwendbares Fahrrad-Erstellen/Bearbeiten-Formular.
 */
export function BikeForm({ defaultValues, existingBike, onSubmit, isEditing }: BikeFormProps) {
  const t = useTranslations("dashboard.bikeForm");
  const tc = useTranslations("bikes.categories");
  const bikeSchema = makeBikeSchema(t);
  const [photos, setPhotos] = useState<File[]>([]);

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
    <form
      onSubmit={handleSubmit((values) => onSubmit(values, photos))}
      className="space-y-5"
    >
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

      <Input
        label={t("model")}
        placeholder={t("modelPlaceholder")}
        error={errors.model?.message}
        {...register("model")}
      />

      <div>
        <label className="label">{t("category")}</label>
        <select
          className={`w-full h-10 px-3 rounded-xl border bg-white text-sm outline-none focus:ring-2 focus:ring-brand-500 ${
            errors.category ? "border-red-400" : "border-slate-300"
          }`}
          {...register("category")}
        >
          <option value="">{t("selectCategory")}</option>
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
          placeholder={t("pricePlaceholder")}
          error={errors.pricePerDay?.message}
          {...register("pricePerDay")}
        />
        <Input
          label={t("city")}
          placeholder={t("cityPlaceholder")}
          error={errors.city?.message}
          {...register("city")}
        />
      </div>

      <Input
        label={t("address")}
        placeholder={t("addressPlaceholder")}
        error={errors.address?.message}
        {...register("address")}
      />

      <div>
        <Input
          label={t("depositAmount")}
          type="number"
          min={0}
          step={0.5}
          placeholder={t("depositAmountPlaceholder")}
          error={errors.depositAmount?.message}
          {...register("depositAmount")}
        />
        <p className="mt-1.5 text-xs text-slate-500">{t("depositAmountHint")}</p>
      </div>

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

      {!isEditing && (
        <div>
          <label className="label">{t("photos")}</label>
          <PhotoDropzone files={photos} onChange={setPhotos} disabled={isSubmitting} />
          <p className="mt-1.5 text-xs text-slate-500">{t("photosHint")}</p>
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
