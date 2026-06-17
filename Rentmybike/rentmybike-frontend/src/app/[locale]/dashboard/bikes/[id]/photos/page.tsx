"use client";

import { use, useRef, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Star, Trash2, Upload } from "lucide-react";
import toast from "react-hot-toast";
import { bikesApi } from "@/lib/api";
import { Button } from "@/components/ui/Button";

interface BikePhotosPageProps {
  params: Promise<{ id: string }>;
}

const MAX_PHOTOS = 5;
const MAX_FILE_SIZE_MB = 10;
const ACCEPTED_TYPES = ["image/jpeg", "image/png", "image/webp"];

/**
 * Bike photo management page — upload up to 5 photos and delete existing ones.
 * Fahrrad-Fotoverwaltung — bis zu 5 Fotos hochladen und vorhandene löschen.
 *
 * Fills the gap where the dashboard "My Bikes" list already links to
 * /dashboard/bikes/[id]/photos and the API client already exposes
 * bikesApi.uploadPhoto / deletePhoto, but no page existed to call them from.
 *
 * Schließt die Lücke: Die "Meine Fahrräder"-Liste verlinkt bereits auf
 * /dashboard/bikes/[id]/photos und der API-Client bietet bereits
 * bikesApi.uploadPhoto / deletePhoto an, aber es gab keine Seite, die sie aufruft.
 */
export default function BikePhotosPage({ params }: BikePhotosPageProps) {
  const { id } = use(params);
  const t = useTranslations("dashboard.bikes");
  const tCommon = useTranslations("common");
  const locale = useLocale();
  const queryClient = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [uploadError, setUploadError] = useState<string | null>(null);

  const { data: bike, isLoading } = useQuery({
    queryKey: ["bike", id],
    queryFn: () => bikesApi.getById(id),
    select: (r) => r.data.data,
  });

  const { mutate: uploadPhoto, isPending: isUploading } = useMutation({
    mutationFn: (file: File) => bikesApi.uploadPhoto(id, file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["bike", id] });
      queryClient.invalidateQueries({ queryKey: ["my-bikes"] });
      setUploadError(null);
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutate: deletePhoto, isPending: isDeleting } = useMutation({
    mutationFn: (photoId: string) => bikesApi.deletePhoto(id, photoId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["bike", id] });
      queryClient.invalidateQueries({ queryKey: ["my-bikes"] });
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const photos = bike?.photos ?? [];
  const atLimit = photos.length >= MAX_PHOTOS;

  function handleFileSelect(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    e.target.value = ""; // allow re-selecting the same file later
    if (!file) return;

    if (atLimit) {
      setUploadError(t("maxPhotos"));
      return;
    }
    if (!ACCEPTED_TYPES.includes(file.type)) {
      setUploadError(
        "Only JPEG, PNG, or WebP images are allowed. / Nur JPEG-, PNG- oder WebP-Bilder sind erlaubt."
      );
      return;
    }
    if (file.size > MAX_FILE_SIZE_MB * 1024 * 1024) {
      setUploadError(
        `File must be smaller than ${MAX_FILE_SIZE_MB}MB. / Datei muss kleiner als ${MAX_FILE_SIZE_MB}MB sein.`
      );
      return;
    }

    setUploadError(null);
    uploadPhoto(file);
  }

  if (isLoading) {
    return <div className="animate-pulse h-96 rounded-2xl bg-slate-100" />;
  }
  if (!bike) return <p>Bike not found. / Fahrrad nicht gefunden.</p>;

  return (
    <div className="max-w-2xl space-y-6">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="sm" asChild>
          <Link href={`/${locale}/dashboard/bikes`}>
            <ArrowLeft size={16} />
          </Link>
        </Button>
        <h1 className="section-title">{t("photos")}</h1>
      </div>

      <div className="card p-6 space-y-4">
        <div className="flex items-center justify-between">
          <p className="text-sm text-slate-500">
            {bike.title} — {photos.length}/{MAX_PHOTOS}
          </p>
          <Button
            size="sm"
            onClick={() => fileInputRef.current?.click()}
            disabled={atLimit || isUploading}
            loading={isUploading}
          >
            <Upload size={16} />
            {t("addPhoto")}
          </Button>
          <input
            ref={fileInputRef}
            type="file"
            accept={ACCEPTED_TYPES.join(",")}
            className="hidden"
            onChange={handleFileSelect}
          />
        </div>

        {atLimit && (
          <p className="text-xs text-slate-500">{t("maxPhotos")}</p>
        )}
        {uploadError && (
          <p className="text-xs text-red-600">{uploadError}</p>
        )}

        {photos.length === 0 ? (
          <div className="rounded-xl border border-dashed border-slate-300 p-10 text-center text-slate-400">
            <Upload size={24} className="mx-auto mb-2" />
            <p className="text-sm">
              No photos yet — upload one to get started. / Noch keine Fotos — laden Sie eines hoch.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
            {photos.map((photo) => (
              <div
                key={photo.id}
                className="relative aspect-square rounded-xl overflow-hidden bg-slate-100 group"
              >
                <Image
                  src={photo.url}
                  alt={bike.title}
                  fill
                  className="object-cover"
                  sizes="200px"
                />
                {photo.primary && (
                  <span className="absolute top-1.5 left-1.5 inline-flex items-center gap-1 rounded-full bg-white/90 px-2 py-0.5 text-xs font-medium text-slate-700">
                    <Star size={12} className="fill-amber-400 text-amber-400" />
                  </span>
                )}
                <button
                  type="button"
                  onClick={() => deletePhoto(photo.id)}
                  disabled={isDeleting}
                  className="absolute bottom-1.5 right-1.5 inline-flex h-7 w-7 items-center justify-center rounded-full bg-white/90 text-red-500 opacity-0 transition-opacity group-hover:opacity-100 hover:bg-white disabled:opacity-50"
                  title={t("delete")}
                >
                  <Trash2 size={14} />
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      <Button variant="secondary" asChild>
        <Link href={`/${locale}/dashboard/bikes`}>{tCommon("back")}</Link>
      </Button>
    </div>
  );
}
