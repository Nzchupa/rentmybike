import { CheckCircle2, Circle } from "lucide-react";
import { cn } from "@/lib/utils";

interface VerificationBadgeProps {
  verified: boolean;
  verifiedLabel: string;
  unverifiedLabel: string;
  className?: string;
}

/**
 * Small trust/verification pill — used wherever we show whether something
 * about a user has been verified (currently: email, via the real
 * `emailVerified` field from the backend).
 *
 * Note: "Phone Verified" and "Identity Verified" badges are NOT implemented
 * yet — the backend's UserProfileResponse / PublicUserResponse types don't
 * carry phoneVerified/identityVerified fields, so showing those badges now
 * would just be fake UI. Add the backend fields first, then reuse this
 * component for them.
 *
 * Kleine Vertrauens-/Verifizierungs-Pille. "Telefon verifiziert" und
 * "Identität verifiziert" sind noch NICHT implementiert, da die Backend-Typen
 * diese Felder nicht liefern.
 */
export function VerificationBadge({
  verified,
  verifiedLabel,
  unverifiedLabel,
  className,
}: VerificationBadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-medium",
        verified ? "bg-emerald-50 text-emerald-700" : "bg-slate-100 text-slate-500",
        className
      )}
    >
      {verified ? <CheckCircle2 size={14} /> : <Circle size={14} />}
      {verified ? verifiedLabel : unverifiedLabel}
    </span>
  );
}
