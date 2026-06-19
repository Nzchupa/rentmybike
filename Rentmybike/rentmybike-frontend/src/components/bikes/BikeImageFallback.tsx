import { Bike } from "lucide-react";
import { cn } from "@/lib/utils";

interface BikeImageFallbackProps {
  className?: string;
  iconSize?: number;
}

/**
 * Shared placeholder shown when a bike has no uploaded photo.
 * Used so every bike image slot (card, my-bikes list, detail page) falls
 * back to the same icon instead of each screen inventing its own.
 *
 * Gemeinsamer Platzhalter, wenn ein Fahrrad kein hochgeladenes Foto hat.
 * Sorgt dafür, dass jeder Bild-Slot denselben Fallback verwendet.
 */
export function BikeImageFallback({ className, iconSize = 48 }: BikeImageFallbackProps) {
  return (
    <div
      className={cn(
        "flex h-full w-full items-center justify-center bg-slate-100 text-slate-300",
        className
      )}
    >
      <Bike size={iconSize} strokeWidth={1.5} />
    </div>
  );
}
