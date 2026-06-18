"use client";

import { useRef } from "react";
import Image from "next/image";
import { useTranslations } from "next-intl";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Camera, Trash2, Loader2 } from "lucide-react";
import toast from "react-hot-toast";
import { bookingPhotosApi } from "@/lib/api";
import { useAuthStore } from "@/store/auth.store";
import type { BookingPhotoPhase, BookingPhotoResponse } from "@/types";

interface BookingPhotosPanelProps {
  bookingId: string;
}

const PHASES: BookingPhotoPhase[] = ["BEFORE", "AFTER"];

/**
 * Before/after condition photo gallery + uploader for a single booking.
 * Vorher/Nachher-Zustandsfoto-Galerie + Uploader für eine einzelne Buchung.
 *
 * <p>Both the renter and the owner of the booking can upload to either
 * phase, and can delete only the photos they uploaded themselves.
 * <p>Sowohl Mieter als auch Eigentümer der Buchung können in beide Phasen
 * hochladen und nur ihre eigenen hochgeladenen Fotos löschen.
 */
export function BookingPhotosPanel({ bookingId }: BookingPhotosPanelProps) {
  const t = useTranslations("booking.photos");
  const { user } = useAuthStore();
  const queryClient = useQueryClient();
  const fileInputRefs = {
    BEFORE: useRef<HTMLInputElement>(null),
    AFTER: useRef<HTMLInputElement>(null),
  };

  const { data, isLoading } = useQuery({
    queryKey: ["booking-photos", bookingId],
    queryFn: () => bookingPhotosApi.list(bookingId),
    select: (r) => r.data.data,
  });

  const { mutate: upload, isPending: uploading } = useMutation({
    mutationFn: ({ file, phase }: { file: File; phase: BookingPhotoPhase }) =>
      bookingPhotosApi.upload(bookingId, file, phase),
    onSuccess: () => {
      toast.success(t("uploaded"));
      queryClient.invalidateQueries({ queryKey: ["booking-photos", bookingId] });
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutate: remove } = useMutation({
    mutationFn: (photoId: string) => bookingPhotosApi.remove(bookingId, photoId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["booking-photos", bookingId] });
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const photos = data ?? [];

  function photosFor(phase: BookingPhotoPhase): BookingPhotoResponse[] {
    return photos.filter((p) => p.phase === phase);
  }

  function handleFileChange(phase: BookingPhotoPhase, e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (file) upload({ file, phase });
    e.target.value = "";
  }

  if (isLoading) {
    return <div className="h-16 animate-pulse bg-slate-100 rounded-xl" />;
  }

  return (
    <div className="space-y-4">
      {PHASES.map((phase) => (
        <div key={phase}>
          <div className="flex items-center justify-between mb-2">
            <h4 className="text-sm font-medium text-slate-700">
              {t(phase === "BEFORE" ? "before" : "after")}
            </h4>
            <button
              type="button"
              disabled={uploading}
              onClick={() => fileInputRefs[phase].current?.click()}
              className="inline-flex items-center gap-1 text-xs font-medium text-brand-600 hover:text-brand-700 disabled:opacity-50"
            >
              {uploading ? <Loader2 size={14} className="animate-spin" /> : <Camera size={14} />}
              {t("addPhoto")}
            </button>
            <input
              ref={fileInputRefs[phase]}
              type="file"
              accept="image/jpeg,image/jpg,image/png,image/webp"
              className="hidden"
              onChange={(e) => handleFileChange(phase, e)}
            />
          </div>

          {photosFor(phase).length === 0 ? (
            <p className="text-xs text-slate-400">{t("noPhotos")}</p>
          ) : (
            <div className="flex gap-2 overflow-x-auto pb-1">
              {photosFor(phase).map((photo) => (
                <div key={photo.id} className="relative shrink-0 w-20 h-20 rounded-lg overflow-hidden bg-slate-100 group">
                  <Image src={photo.photoUrl} alt="" fill className="object-cover" sizes="80px" />
                  {photo.uploaderId === user?.id && (
                    <button
                      type="button"
                      onClick={() => remove(photo.id)}
                      className="absolute top-0.5 right-0.5 bg-black/60 text-white rounded-full p-1 opacity-0 group-hover:opacity-100 transition"
                      aria-label={t("delete")}
                    >
                      <Trash2 size={11} />
                    </button>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      ))}
    </div>
  );
}
