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
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ["my-bikes"] });
      toast.success(t("bikeListed"));
      // Send the owner straight to the photo-upload page instead of the
      // plain list — a bike with zero photos is much less likely to get
      // booked, and without this most owners wouldn't realize photos are
      // a separate step.
      // Eigentümer direkt zur Foto-Upload-Seite schicken statt zur reinen
      // Liste — ein Fahrrad ohne Fotos wird viel seltener gebucht, und ohne
      // dies würden die meisten Eigentümer nicht merken, dass Fotos ein
      // separater Schritt sind.
      router.push(`/${locale}/dashboard/bikes/${res.data.data.id}/photos`);
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
