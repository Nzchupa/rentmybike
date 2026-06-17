"use client";

import { use } from "react";
import { useLocale, useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import { bikesApi } from "@/lib/api";
import { BikeForm, type BikeFormValues } from "@/components/bikes/BikeForm";

interface EditBikePageProps {
  params: Promise<{ id: string }>;
}

export default function EditBikePage({ params }: EditBikePageProps) {
  const { id } = use(params);
  const t = useTranslations("dashboard.bikeForm");
  const locale = useLocale();
  const router = useRouter();
  const queryClient = useQueryClient();

  const { data: bike, isLoading } = useQuery({
    queryKey: ["bike", id],
    queryFn: () => bikesApi.getById(id),
    select: (r) => r.data.data,
  });

  const { mutateAsync: updateBike } = useMutation({
    mutationFn: (values: BikeFormValues) => bikesApi.update(id, values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["my-bikes"] });
      queryClient.invalidateQueries({ queryKey: ["bike", id] });
      toast.success(t("bikeUpdated"));
      router.push(`/${locale}/dashboard/bikes`);
    },
    onError: (e: Error) => toast.error(e.message),
  });

  if (isLoading) {
    return <div className="animate-pulse h-96 rounded-2xl bg-slate-100" />;
  }
  if (!bike) return <p>{t("bikeNotFound")}</p>;

  return (
    <div className="max-w-2xl">
      <h1 className="section-title mb-8">{t("editTitle")}</h1>
      <div className="card p-6">
        <BikeForm
          isEditing
          existingBike={bike}
          defaultValues={{
            title:       bike.title,
            description: bike.description,
            category:    bike.category,
            pricePerDay: bike.pricePerDay,
            city:        bike.city,
            address:     bike.address ?? "",
            available:   bike.available,
          }}
          onSubmit={async (values) => { await updateBike(values); }}
        />
      </div>
    </div>
  );
}
