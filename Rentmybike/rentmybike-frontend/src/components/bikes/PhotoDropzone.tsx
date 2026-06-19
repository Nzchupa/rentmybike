"use client";

import { useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { ImagePlus, X } from "lucide-react";

const MAX_PHOTOS = 5;
const MAX_FILE_SIZE_MB = 10;
const ACCEPTED_TYPES = ["image/jpeg", "image/png", "image/webp"];

interface PhotoDropzoneProps {
  files: File[];
  onChange: (files: File[]) => void;
  disabled?: boolean;
}

/**
 * Drag-and-drop (+ click-to-browse) photo picker used on the "create bike"
 * form. Files are kept client-side only — the parent form uploads them
 * (via bikesApi.uploadPhoto) once the bike itself has been created, since
 * the upload endpoint needs a bike ID that doesn't exist yet at this point.
 *
 * Drag-and-Drop- (+ Klick-zum-Durchsuchen-) Fotoauswahl für das
 * "Fahrrad erstellen"-Formular. Dateien werden nur clientseitig gehalten —
 * das übergeordnete Formular lädt sie hoch (via bikesApi.uploadPhoto),
 * sobald das Fahrrad selbst erstellt wurde, da der Upload-Endpunkt eine
 * Fahrrad-ID benötigt, die zu diesem Zeitpunkt noch nicht existiert.
 */
export function PhotoDropzone({ files, onChange, disabled }: PhotoDropzoneProps) {
  const t = useTranslations("dashboard.bikeForm.dropzone");
  const inputRef = useRef<HTMLInputElement>(null);
  const [isDragOver, setIsDragOver] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function addFiles(incoming: FileList | File[]) {
    setError(null);
    const incomingArr = Array.from(incoming);
    const accepted: File[] = [];

    for (const file of incomingArr) {
      if (files.length + accepted.length >= MAX_PHOTOS) {
        setError(t("tooMany", { max: MAX_PHOTOS }));
        break;
      }
      if (!ACCEPTED_TYPES.includes(file.type)) {
        setError(t("invalidType"));
        continue;
      }
      if (file.size > MAX_FILE_SIZE_MB * 1024 * 1024) {
        setError(t("tooLarge", { max: MAX_FILE_SIZE_MB }));
        continue;
      }
      accepted.push(file);
    }

    if (accepted.length > 0) {
      onChange([...files, ...accepted]);
    }
  }

  function handleInputChange(e: React.ChangeEvent<HTMLInputElement>) {
    if (e.target.files && e.target.files.length > 0) {
      addFiles(e.target.files);
    }
    e.target.value = ""; // allow re-selecting the same file later
  }

  function handleDrop(e: React.DragEvent<HTMLDivElement>) {
    e.preventDefault();
    setIsDragOver(false);
    if (disabled) return;
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      addFiles(e.dataTransfer.files);
    }
  }

  function removeFile(index: number) {
    onChange(files.filter((_, i) => i !== index));
  }

  const atLimit = files.length >= MAX_PHOTOS;

  return (
    <div className="space-y-3">
      <div
        role="button"
        tabIndex={0}
        onClick={() => !disabled && !atLimit && inputRef.current?.click()}
        onKeyDown={(e) => {
          if ((e.key === "Enter" || e.key === " ") && !disabled && !atLimit) {
            inputRef.current?.click();
          }
        }}
        onDragOver={(e) => {
          e.preventDefault();
          if (!disabled && !atLimit) setIsDragOver(true);
        }}
        onDragLeave={() => setIsDragOver(false)}
        onDrop={handleDrop}
        className={`flex flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed p-6 text-center text-sm transition-colors cursor-pointer ${
          disabled || atLimit
            ? "cursor-not-allowed opacity-60 border-slate-200"
            : isDragOver
            ? "border-brand-500 bg-brand-50"
            : "border-slate-300 hover:border-brand-400 hover:bg-slate-50"
        }`}
      >
        <ImagePlus size={22} className="text-slate-400" />
        <p className="text-slate-500">{t("cta")}</p>
        <span className="text-xs font-medium text-brand-600 underline">{t("browse")}</span>
        <input
          ref={inputRef}
          type="file"
          accept={ACCEPTED_TYPES.join(",")}
          multiple
          className="hidden"
          disabled={disabled || atLimit}
          onChange={handleInputChange}
        />
      </div>

      {error && <p className="field-error">{error}</p>}

      {files.length > 0 && (
        <div className="grid grid-cols-3 sm:grid-cols-5 gap-3">
          {files.map((file, index) => {
            const previewUrl = URL.createObjectURL(file);
            return (
              <div
                key={`${file.name}-${index}`}
                className="relative aspect-square rounded-xl overflow-hidden bg-slate-100 group"
              >
                {/* eslint-disable-next-line @next/next/no-img-element -- transient blob: URL, next/image doesn't support it */}
                <img
                  src={previewUrl}
                  alt={file.name}
                  className="h-full w-full object-cover"
                  onLoad={(e) => URL.revokeObjectURL((e.target as HTMLImageElement).src)}
                />
                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation();
                    removeFile(index);
                  }}
                  disabled={disabled}
                  className="absolute top-1 right-1 inline-flex h-6 w-6 items-center justify-center rounded-full bg-white/90 text-red-500 opacity-0 transition-opacity group-hover:opacity-100 hover:bg-white disabled:opacity-50"
                  title={t("remove")}
                >
                  <X size={14} />
                </button>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
