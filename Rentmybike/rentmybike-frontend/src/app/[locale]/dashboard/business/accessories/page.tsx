"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import toast from "react-hot-toast";
import { Plus, Pencil, Trash2, HardHat, Baby, Lock, type LucideIcon } from "lucide-react";
import { accessoriesApi } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { formatPrice } from "@/lib/utils";
import type { AccessoryResponse, AccessoryType, CreateAccessoryRequest } from "@/types";

const TYPES: AccessoryType[] = ["HELMET", "CHILD_SEAT", "LOCK"];

const TYPE_ICONS: Record<AccessoryType, LucideIcon> = {
  HELMET: HardHat,
  CHILD_SEAT: Baby,
  LOCK: Lock,
};

function makeAccessorySchema(t: (key: string) => string) {
  return z.object({
    type:          z.enum(["HELMET", "CHILD_SEAT", "LOCK"]),
    name:          z.string().min(2, t("dashboard.profile.validation.minTwoChars")).max(100),
    quantityTotal: z.coerce.number().int().min(1),
    pricePerDay:   z.coerce.number().min(0),
  });
}

type AccessoryFormValues = z.infer<ReturnType<typeof makeAccessorySchema>>;

function AccessoryForm({
  defaultValues,
  onSubmit,
  onCancel,
  submitLabel,
}: {
  defaultValues?: Partial<AccessoryFormValues>;
  onSubmit: (values: AccessoryFormValues) => Promise<void>;
  onCancel?: () => void;
  submitLabel: string;
}) {
  const t = useTranslations("business.accessories");
  const tRoot = useTranslations();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<AccessoryFormValues>({
    resolver: zodResolver(makeAccessorySchema(tRoot)),
    defaultValues: {
      type: "HELMET",
      name: "",
      quantityTotal: 1,
      pricePerDay: 0,
      ...defaultValues,
    },
  });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="label">{t("type")}</label>
          <select
            className="w-full h-10 px-3 rounded-xl border border-slate-300 bg-white text-sm outline-none focus:ring-2 focus:ring-brand-500"
            {...register("type")}
          >
            {TYPES.map((type) => (
              <option key={type} value={type}>{t(`types.${type}`)}</option>
            ))}
          </select>
        </div>
        <Input
          label={t("name")}
          error={errors.name?.message}
          {...register("name")}
        />
      </div>
      <div className="grid grid-cols-2 gap-4">
        <Input
          label={t("quantityTotal")}
          type="number"
          min={1}
          error={errors.quantityTotal?.message}
          {...register("quantityTotal")}
        />
        <Input
          label={t("pricePerDay")}
          type="number"
          min={0}
          step={0.5}
          error={errors.pricePerDay?.message}
          {...register("pricePerDay")}
        />
      </div>
      <div className="flex items-center gap-3">
        <Button type="submit" loading={isSubmitting}>{submitLabel}</Button>
        {onCancel && (
          <Button type="button" variant="ghost" onClick={onCancel}>
            Cancel
          </Button>
        )}
      </div>
    </form>
  );
}

/**
 * Accessories CRUD page — Stage 3 "Business accounts" / Additional feature
 * (helmets, child seats, locks renters can add to a booking).
 * Zubehör-CRUD-Seite — Stage 3 "Business-Konten".
 */
export default function AccessoriesPage() {
  const t = useTranslations("business.accessories");
  const queryClient = useQueryClient();
  const [showAddForm, setShowAddForm] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);

  const { data: accessories, isLoading } = useQuery({
    queryKey: ["my-accessories"],
    queryFn: () => accessoriesApi.getMine(),
    select: (r) => r.data.data,
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["my-accessories"] });

  const { mutateAsync: createAccessory } = useMutation({
    mutationFn: (data: CreateAccessoryRequest) => accessoriesApi.create(data),
    onSuccess: () => {
      invalidate();
      toast.success(t("added"));
      setShowAddForm(false);
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutateAsync: updateAccessory } = useMutation({
    mutationFn: ({ id, data }: { id: string; data: CreateAccessoryRequest }) =>
      accessoriesApi.update(id, data),
    onSuccess: () => {
      invalidate();
      toast.success(t("updated"));
      setEditingId(null);
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutate: deleteAccessory } = useMutation({
    mutationFn: (id: string) => accessoriesApi.delete(id),
    onSuccess: () => {
      invalidate();
      toast.success(t("deleted"));
    },
    onError: (e: Error) => toast.error(e.message),
  });

  return (
    <div className="max-w-2xl space-y-6">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="section-title">{t("title")}</h1>
          <p className="text-sm text-slate-500 mt-1">{t("subtitle")}</p>
        </div>
        {!showAddForm && (
          <Button onClick={() => setShowAddForm(true)}>
            <Plus size={16} />
            {t("addAccessory")}
          </Button>
        )}
      </div>

      {showAddForm && (
        <div className="card p-5">
          <AccessoryForm
            submitLabel={t("save")}
            onCancel={() => setShowAddForm(false)}
            onSubmit={async (values) => {
              await createAccessory(values);
            }}
          />
        </div>
      )}

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="card h-20 animate-pulse bg-slate-100" />
          ))}
        </div>
      ) : !accessories?.length ? (
        <p className="text-sm text-slate-500">{t("noAccessories")}</p>
      ) : (
        <div className="space-y-3">
          {accessories.map((accessory: AccessoryResponse) => {
            const Icon = TYPE_ICONS[accessory.type];
            if (editingId === accessory.id) {
              return (
                <div key={accessory.id} className="card p-5">
                  <AccessoryForm
                    submitLabel={t("save")}
                    defaultValues={{
                      type: accessory.type,
                      name: accessory.name,
                      quantityTotal: accessory.quantityTotal,
                      pricePerDay: accessory.pricePerDay,
                    }}
                    onCancel={() => setEditingId(null)}
                    onSubmit={async (values) => {
                      await updateAccessory({ id: accessory.id, data: values });
                    }}
                  />
                </div>
              );
            }
            return (
              <div key={accessory.id} className="card p-4 flex items-center justify-between gap-4">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-brand-50 flex items-center justify-center">
                    <Icon size={18} className="text-brand-600" />
                  </div>
                  <div>
                    <p className="font-medium text-slate-900">{accessory.name}</p>
                    <p className="text-sm text-slate-500">
                      {t(`types.${accessory.type}`)} · {accessory.quantityTotal} × {formatPrice(accessory.pricePerDay)}/day
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setEditingId(accessory.id)}
                    className="p-2 text-slate-400 hover:text-brand-600 transition-colors"
                    aria-label={t("edit")}
                  >
                    <Pencil size={16} />
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      if (confirm(t("deleteConfirm"))) deleteAccessory(accessory.id);
                    }}
                    className="p-2 text-slate-400 hover:text-red-500 transition-colors"
                    aria-label={t("delete")}
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
