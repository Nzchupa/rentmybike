import Link from "next/link";
import { ShieldCheck, Clock } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { cn } from "@/lib/utils";

interface BusinessVerificationCardProps {
  verified: boolean;
  locale: string;
  verifiedLabel: string;
  pendingLabel: string;
  pendingNotice: string;
  goToDashboardLabel: string;
}

/**
 * Dedicated business-verification status card for the Profile page —
 * replaces the old inline pill + paragraph with a clearer, self-contained
 * card (icon, status, explanation, CTA) so verification status doesn't get
 * lost next to the rest of the business-upgrade form.
 *
 * Eigenständige Business-Verifizierungs-Karte für die Profilseite — ersetzt
 * die alte Pille + Absatz durch eine klarere Karte (Icon, Status,
 * Erklärung, CTA).
 */
export function BusinessVerificationCard({
  verified,
  locale,
  verifiedLabel,
  pendingLabel,
  pendingNotice,
  goToDashboardLabel,
}: BusinessVerificationCardProps) {
  return (
    <div className="flex items-start gap-4 rounded-2xl border border-slate-200 p-4">
      <div
        className={cn(
          "flex h-11 w-11 shrink-0 items-center justify-center rounded-full",
          verified ? "bg-emerald-50 text-emerald-600" : "bg-amber-50 text-amber-600"
        )}
      >
        {verified ? <ShieldCheck size={22} /> : <Clock size={22} />}
      </div>
      <div className="flex-1 min-w-0 space-y-2">
        <span
          className={cn(
            "inline-flex items-center px-3 py-1 rounded-full text-xs font-medium",
            verified ? "bg-emerald-50 text-emerald-700" : "bg-amber-50 text-amber-700"
          )}
        >
          {verified ? verifiedLabel : pendingLabel}
        </span>
        {!verified && <p className="text-sm text-slate-500">{pendingNotice}</p>}
        <div>
          <Link href={`/${locale}/dashboard/business`}>
            <Button type="button" size="sm">
              {goToDashboardLabel}
            </Button>
          </Link>
        </div>
      </div>
    </div>
  );
}
