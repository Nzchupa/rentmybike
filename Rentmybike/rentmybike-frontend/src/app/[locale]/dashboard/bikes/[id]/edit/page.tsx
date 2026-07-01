"use client";

import { useLocale, useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import { bikesApi } from "@/lib/api";
import { BikeForm, type BikeFormValues } from "@/components/bikes/BikeForm";

interface EditBikePageProps {
  // Next.js 14 (this project's version) passes params synchronously, not as
  // a Promise — see the matching comment in bikes/[id]/page.tsx.
  // Next.js 14 (Version dieses Projekts) übergibt params synchron, nicht
  // als Promise — siehe entsprechenden Kommentar in bikes/[id]/page.tsx.
  params: { id: string };
}

export default function EditBikePage({ params }: EditBikePageProps) {
  const { id } = params;
  const t = useTranslations("dashboard.bikeForm");
  const locale = useLocale();
  const router = useRouter();
  const queryClient = useQueryClient();

  // Owner-scoped fetch — bikesApi.getById() hits the PUBLIC endpoint, which
  // only returns APPROVED bikes (BikeService.getPublicBike). A bike that's
  // still PENDING (e.g. right after creation, or after a previous edit reset
  // it to PENDING) would 404 here, so the owner could never load their own
  // bike to edit it. getOwnerById() hits /api/v1/bikes/{id}/owner, which
  // returns the bike for any approval status as long as the caller owns it.
  //
  // Eigentümer-spezifischer Abruf — bikesApi.getById() ruft den ÖFFENTLICHEN
  // Endpunkt auf, der nur APPROVED-Fahrräder zurückgibt (BikeService.
  // getPublicBike). Ein Fahrrad, das noch PENDING ist (z. B. direkt nach der
  // Erstellung oder weil eine vorherige Bearbeitung es auf PENDING
  // zurückgesetzt hat), würde hier mit 404 antworten, sodass der Eigentümer
  // sein eigenes Fahrrad nie zum Bearbeiten laden könnte. getOwnerById() ruft
  // /api/v1/bikes/{id}/owner auf, das das Fahrrad für jeden Genehmigungsstatus
  // zurückgibt, solange der Aufrufer der Eigentümer ist.
  const { data: bike, isLoading } = useQuery({
    queryKey: ["bike", id, "owner"],
    queryFn: () => bikesApi.getOwnerById(id),
    select: (r) => r.data.data,
  });

  const { mutateAsync: updateBike } = useMutation({
    mutationFn: (values: BikeFormValues) => bikesApi.update(id, values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["my-bikes"] });
      queryClient.invalidateQueries({ queryKey: ["bike", id] });
      // Only CHANGES_REQUESTED bikes actually go back for review on save
      // (see BikeService.updateBike) — a normal edit to an already-APPROVED
      // bike stays live, so it gets the plain "updated" toast instead of
      // falsely claiming it was sent back for review.
      // Nur CHANGES_REQUESTED-Fahrräder gehen beim Speichern tatsächlich
      // zurück zur Prüfung (siehe BikeService.updateBike) — eine normale
      // Bearbeitung eines bereits genehmigten Fahrrads bleibt live und
      // erhält daher den einfachen "aktualisiert"-Toast, statt fälschlich
      // zu behaupten, es sei zur Prüfung zurückgeschickt worden.
      toast.success(
        bike?.approvalStatus === "CHANGES_REQUESTED"
          ? t("bikeUpdated")
          : t("bikeUpdatedLive")
      );
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
            title:         bike.title,
            description:   bike.description,
            model:         bike.model ?? "",
            category:      bike.category,
            pricePerDay:   bike.pricePerDay,
            depositAmount: bike.depositAmount ?? undefined,
            city:          bike.city,
            address:       bike.address ?? "",
            available:     bike.available,
          }}
          onSubmit={async (values) => { await updateBike(values); }}
        />
      </div>
    </div>
  );
}
