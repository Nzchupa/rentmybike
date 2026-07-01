"use client";

import { useTranslations } from "next-intl";
import { cn } from "@/lib/utils";
import type { BookingStatus, ApprovalStatus, ReportStatus, SupportTicketStatus } from "@/types";

type BadgeVariant = "green" | "yellow" | "red" | "blue" | "gray";
type BadgeSize = "sm" | "md";

interface BadgeProps {
  children: React.ReactNode;
  variant?: BadgeVariant;
  size?: BadgeSize;
  className?: string;
}

const variantClasses: Record<BadgeVariant, string> = {
  green:  "bg-green-100 text-green-800 dark:bg-green-900/40 dark:text-green-300",
  yellow: "bg-yellow-100 text-yellow-800 dark:bg-yellow-900/40 dark:text-yellow-300",
  red:    "bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-300",
  blue:   "bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-300",
  gray:   "bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300",
};

// "md" gives booking status badges more visual weight on dashboard cards,
// where the status is often the most important thing on the card (spec
// item #9). "sm" stays the default for inline/dense contexts.
// "md" verleiht Buchungsstatus-Badges auf Dashboard-Karten mehr visuelles
// Gewicht, da der Status dort oft das Wichtigste ist. "sm" bleibt der
// Standard für kompakte Kontexte.
const sizeClasses: Record<BadgeSize, string> = {
  sm: "px-2.5 py-0.5 text-xs",
  md: "px-3 py-1 text-sm",
};

export function Badge({ children, variant = "gray", size = "sm", className }: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full font-medium",
        variantClasses[variant],
        sizeClasses[size],
        className
      )}
    >
      {children}
    </span>
  );
}

// ── Domain-specific badge helpers ─────────────────────────────────────────────

const bookingStatusVariant: Record<BookingStatus, BadgeVariant> = {
  PENDING:   "yellow",
  ACCEPTED:  "blue",
  REJECTED:  "red",
  CANCELLED: "gray",
  COMPLETED: "green",
};

export function BookingStatusBadge({ status, size }: { status: BookingStatus; size?: BadgeSize }) {
  // Previously rendered the raw enum value (e.g. "PENDING") regardless of
  // locale. Vorher wurde der rohe Enum-Wert (z. B. "PENDING") unabhängig
  // von der Sprache angezeigt.
  const t = useTranslations("booking.status");
  return <Badge variant={bookingStatusVariant[status]} size={size}>{t(status)}</Badge>;
}

const approvalStatusVariant: Record<ApprovalStatus, BadgeVariant> = {
  PENDING:           "yellow",
  APPROVED:          "green",
  REJECTED:          "red",
  CHANGES_REQUESTED: "blue",
};

export function ApprovalStatusBadge({ status }: { status: ApprovalStatus }) {
  // Previously rendered the raw enum value (e.g. "APPROVED") regardless of
  // locale. Vorher wurde der rohe Enum-Wert (z. B. "APPROVED") unabhängig
  // von der Sprache angezeigt.
  const t = useTranslations("bikes.approvalStatus");
  return <Badge variant={approvalStatusVariant[status]}>{t(status)}</Badge>;
}

const reportStatusVariant: Record<ReportStatus, BadgeVariant> = {
  PENDING:      "yellow",
  UNDER_REVIEW: "blue",
  RESOLVED:     "green",
  DISMISSED:    "gray",
};

export function ReportStatusBadge({ status }: { status: ReportStatus }) {
  const t = useTranslations("admin.moderation.status");
  return <Badge variant={reportStatusVariant[status]}>{t(status)}</Badge>;
}

const supportStatusVariant: Record<SupportTicketStatus, BadgeVariant> = {
  OPEN:        "yellow",
  IN_PROGRESS: "blue",
  RESOLVED:    "green",
  CLOSED:      "gray",
};

export function SupportTicketStatusBadge({ status }: { status: SupportTicketStatus }) {
  const t = useTranslations("support.status");
  return <Badge variant={supportStatusVariant[status]}>{t(status)}</Badge>;
}
