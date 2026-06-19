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
    onError: (e: Error) => toast.error(e.message),
  });

  // Photos picked in the form's PhotoDropzone can't be uploaded until the
  // bike exists (the upload endpoint needs a bike ID), so they're held
  // client-side and sent here, right after creation succeeds, one request
  // per file. A failure on one photo doesn't block the others or the
  // overall flow — the owner still lands on the photos page afterwards and
  // can retry anything that didn't make it.
  //
  // In der PhotoDropzone des Formulars ausgewählte Fotos können erst
  // hochgeladen werden, wenn das Fahrrad existiert (der Upload-Endpunkt
  // benötigt eine Fahrrad-ID), daher werden sie clientseitig gehalten und
  // hier, direkt nach erfolgreicher Erstellung, mit je einer Anfrage pro
  // Datei gesendet. Ein Fehlschlag bei einem Foto blockiert weder die
  // anderen noch den Gesamtablauf — der Eigentümer landet danach trotzdem
  // auf der Fotoseite und kann alles wiederholen, was nicht angekommen ist.
  async function onSubmit(values: BikeFormValues, photos: File[]) {
    const res = await createBike({
      ...values,
      address: values.address ?? undefined,
    });
    const bikeId = res.data.data.id;
    queryClient.invalidateQueries({ queryKey: ["my-bikes"] });

    for (const file of photos) {
      try {
        await bikesApi.uploadPhoto(bikeId, file);
      } catch (e) {
        toast.error(e instanceof Error ? e.message : "Photo upload failed");
      }
    }
    if (photos.length > 0) {
      queryClient.invalidateQueries({ queryKey: ["bike", bikeId] });
    }

    toast.success(t("bikeListed"));
    // Send the owner to the photo-upload page rather than the plain list —
    // even with photos already attached, this is where they can review,
    // reorder, or fix anything that failed above.
    // Eigentümer zur Foto-Upload-Seite schicken statt zur reinen Liste —
    // selbst mit bereits angehängten Fotos ist dies die Stelle, an der sie
    // alles überprüfen, neu anordnen oder oben Fehlgeschlagenes
    // korrigieren können.
    router.push(`/${locale}/dashboard/bikes/${bikeId}/photos`);
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
