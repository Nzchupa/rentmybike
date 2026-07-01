"use client";

import { useLocale, useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useForm, useFieldArray } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import toast from "react-hot-toast";
import { Plus, Trash2 } from "lucide-react";
import { businessApi } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import type { BikeCategory } from "@/types";

const CATEGORIES: BikeCategory[] = [
  "CITY", "MOUNTAIN", "ROAD", "ELECTRIC", "HYBRID", "CARGO", "KIDS",
];

function makeBulkSchema(t: (key: string) => string, tb: (key: string) => string) {
  return z.object({
    bikes: z
      .array(
        z.object({
          title:       z.string().min(5, t("validation.titleMin")).max(100),
          description: z.string().min(20, t("validation.descriptionMin")).max(3000),
          model:       z.string().max(150).optional(),
          category:    z.enum(["CITY", "MOUNTAIN", "ROAD", "ELECTRIC", "HYBRID", "CARGO", "KIDS"]),
          pricePerDay: z.coerce.number().min(1, t("validation.priceMin")),
          city:        z.string().min(2, t("validation.cityRequired")).max(100),
          address:     z.string().max(255).optional(),
        })
      )
      .min(1)
      .max(50, tb("bulkAdd.maxReached")),
  });
}

type BulkFormValues = z.infer<ReturnType<typeof makeBulkSchema>>;

const emptyRow = { title: "", description: "", model: "", category: "CITY" as BikeCategory, pricePerDay: 0, city: "", address: "" };

/**
 * Bulk-add bikes page — Stage 3 "Business accounts" / Additional feature.
 * Seite zum massenhaften Hinzufügen von Fahrrädern — Stage 3 "Business-Konten".
 */
export default function BulkAddBikesPage() {
  const t = useTranslations("dashboard.bikeForm");
  const tc = useTranslations("bikes.categories");
  const tb = useTranslations("business");
  const locale = useLocale();
  const router = useRouter();
  const queryClient = useQueryClient();

  const {
    register,
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<BulkFormValues>({
    resolver: zodResolver(makeBulkSchema(t, tb)),
    defaultValues: { bikes: [emptyRow] },
  });

  const { fields, append, remove } = useFieldArray({ control, name: "bikes" });

  const { mutateAsync: bulkCreate } = useMutation({
    mutationFn: businessApi.bulkCreateBikes,
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ["my-bikes"] });
      toast.success(tb("bulkAdd.success", { count: res.data.data.length }));
      router.push(`/${locale}/dashboard/bikes`);
    },
    onError: (e: Error) => toast.error(e.message),
  });

  async function onSubmit(values: BulkFormValues) {
    await bulkCreate({
      bikes: values.bikes.map((b) => ({ ...b, address: b.address || undefined, model: b.model || undefined })),
    });
  }

  return (
    <div className="max-w-3xl space-y-6">
      <div>
        <h1 className="section-title">{tb("bulkAdd.title")}</h1>
        <p className="text-sm text-slate-500 mt-1">{tb("bulkAdd.subtitle")}</p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
        {fields.map((field, index) => (
          <div key={field.id} className="card p-5 space-y-4 relative">
            {fields.length > 1 && (
              <button
                type="button"
                onClick={() => remove(index)}
                className="absolute top-4 right-4 text-slate-400 hover:text-red-500 transition-colors"
                aria-label={tb("bulkAdd.removeRow")}
              >
                <Trash2 size={16} />
              </button>
            )}

            <Input
              label={t("title")}
              placeholder={t("titlePlaceholder")}
              error={errors.bikes?.[index]?.title?.message}
              {...register(`bikes.${index}.title`)}
            />

            <div>
              <label className="label">{t("description")}</label>
              <textarea
                rows={3}
                placeholder={t("descriptionPlaceholder")}
                className={`w-full rounded-xl border px-3 py-2 text-sm resize-none outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500 ${
                  errors.bikes?.[index]?.description ? "border-red-400" : "border-slate-300"
                }`}
                {...register(`bikes.${index}.description`)}
              />
              {errors.bikes?.[index]?.description && (
                <p className="field-error">{errors.bikes[index]?.description?.message}</p>
              )}
            </div>

            <Input
              label={t("model")}
              placeholder={t("modelPlaceholder")}
              error={errors.bikes?.[index]?.model?.message}
              {...register(`bikes.${index}.model`)}
            />

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="label">{t("category")}</label>
                <select
                  className="w-full h-10 px-3 rounded-xl border border-slate-300 bg-white text-sm outline-none focus:ring-2 focus:ring-brand-500"
                  {...register(`bikes.${index}.category`)}
                >
                  {CATEGORIES.map((cat) => (
                    <option key={cat} value={cat}>{tc(cat)}</option>
                  ))}
                </select>
              </div>
              <Input
                label={t("pricePerDay")}
                type="number"
                min={1}
                step={0.5}
                placeholder={t("pricePlaceholder")}
                error={errors.bikes?.[index]?.pricePerDay?.message}
                {...register(`bikes.${index}.pricePerDay`)}
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <Input
                label={t("city")}
                placeholder={t("cityPlaceholder")}
                error={errors.bikes?.[index]?.city?.message}
                {...register(`bikes.${index}.city`)}
              />
              <Input
                label={t("address")}
                placeholder={t("addressPlaceholder")}
                error={errors.bikes?.[index]?.address?.message}
                {...register(`bikes.${index}.address`)}
              />
            </div>
          </div>
        ))}

        <div className="flex items-center justify-between flex-wrap gap-3">
          <Button
            type="button"
            variant="secondary"
            onClick={() => fields.length < 50 && append(emptyRow)}
          >
            <Plus size={16} />
            {tb("bulkAdd.addRow")}
          </Button>
          <Button type="submit" loading={isSubmitting}>
            {tb("bulkAdd.submit")}
          </Button>
        </div>
      </form>
    </div>
  );
}
