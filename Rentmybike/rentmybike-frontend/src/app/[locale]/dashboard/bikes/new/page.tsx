"use client";

import { useLocale, useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import { bikesApi } from "@/lib/api";
import { BikeForm, type BikeFormValues } from "@/components/bikes/BikeForm";

/**
 * Create a new bike listing page.
 * Seite zum Erstellen eines neuen Fahrrad-Inserats.
 */
export default function NewBikePage() {
  const t = useTranslations("dashboard.bikeForm");
  const locale = useLocale();
  const router = useRouter();
  const queryClient = useQueryClient();

  const { mutateAsync: createBike } = useMutation({
    mutationFn: bikesApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["my-bikes"] });
      toast.success("Bike listed! Awaiting review. / Fahrrad inseriert! Wartet auf Prüfung.");
      router.push(`/${locale}/dashboard/bikes`);
    },
    onError: (e: Error) => toast.error(e.message),
  });

  async function onSubmit(values: BikeFormValues) {
    await createBike({
      ...values,
      address: values.address ?? undefined,
    });
  }

  return (
    <div className="max-w-2xl">
      <h1 className="section-title mb-8">{t("createTitle")}</h1>
      <div className="card p-6">
        <BikeForm onSubmit={onSubmit} />
      </div>
    </div>
  );
}
