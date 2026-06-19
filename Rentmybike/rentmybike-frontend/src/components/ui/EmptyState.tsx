import Link from "next/link";
import type { LucideIcon } from "lucide-react";
import { Button } from "@/components/ui/Button";

interface EmptyStateAction {
  label: string;
  href: string;
}

interface EmptyStateProps {
  icon: LucideIcon;
  message: string;
  action?: EmptyStateAction;
  variant?: "default" | "error";
}

/**
 * Shared empty/error-state card — icon + friendly message + optional CTA.
 * Replaces the old bare "You haven't booked anything yet." text blocks
 * that appeared unfinished across bookings, favorites, my-bikes, etc.
 *
 * Gemeinsame Leer-/Fehlerzustand-Karte — Icon + freundliche Nachricht +
 * optionaler CTA. Ersetzt die alten reinen Textblöcke.
 */
export function EmptyState({ icon: Icon, message, action, variant = "default" }: EmptyStateProps) {
  const isError = variant === "error";

  return (
    <div className="card flex flex-col items-center gap-3 px-6 py-14 text-center">
      <div
        className={
          isError
            ? "flex h-14 w-14 items-center justify-center rounded-full bg-red-50 text-red-400"
            : "flex h-14 w-14 items-center justify-center rounded-full bg-slate-100 text-slate-400"
        }
      >
        <Icon size={28} strokeWidth={1.5} />
      </div>

      <p className={isError ? "max-w-sm text-sm text-red-600" : "max-w-sm text-sm text-slate-500"}>
        {message}
      </p>

      {action && (
        <Button asChild className="mt-1">
          <Link href={action.href}>{action.label}</Link>
        </Button>
      )}
    </div>
  );
}
