import { cn } from "@/lib/utils";
import type { BookingStatus, ApprovalStatus } from "@/types";

type BadgeVariant = "green" | "yellow" | "red" | "blue" | "gray";

interface BadgeProps {
  children: React.ReactNode;
  variant?: BadgeVariant;
  className?: string;
}

const variantClasses: Record<BadgeVariant, string> = {
  green:  "bg-green-100 text-green-800",
  yellow: "bg-yellow-100 text-yellow-800",
  red:    "bg-red-100 text-red-800",
  blue:   "bg-blue-100 text-blue-800",
  gray:   "bg-slate-100 text-slate-700",
};

export function Badge({ children, variant = "gray", className }: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium",
        variantClasses[variant],
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

export function BookingStatusBadge({ status }: { status: BookingStatus }) {
  return <Badge variant={bookingStatusVariant[status]}>{status}</Badge>;
}

const approvalStatusVariant: Record<ApprovalStatus, BadgeVariant> = {
  PENDING:  "yellow",
  APPROVED: "green",
  REJECTED: "red",
};

export function ApprovalStatusBadge({ status }: { status: ApprovalStatus }) {
  return <Badge variant={approvalStatusVariant[status]}>{status}</Badge>;
}
