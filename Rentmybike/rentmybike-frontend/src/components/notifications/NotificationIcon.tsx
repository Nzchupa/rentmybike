import { Calendar, MessageCircle, Bike, Flag, Bell, LifeBuoy, Banknote, Star } from "lucide-react";
import { cn } from "@/lib/utils";
import type { NotificationType } from "@/types";

interface NotificationIconProps {
  type: NotificationType;
  className?: string;
}

// Notification types that exist in the backend today (see NotificationType in
// types/index.ts): user-facing (booking requests, chat messages, support
// ticket replies) and admin-only fan-out types (new pending bike, new
// report, new support ticket — see task #32). A "default" bell is kept as a
// fallback so a future type added on the backend doesn't render blank here.
// Es gibt derzeit mehrere Benachrichtigungstypen im Backend: nutzerseitig
// (Buchungsanfragen, Chat-Nachrichten, Support-Ticket-Antworten) und nur für
// Admins (neues ausstehendes Fahrrad, neue Meldung, neues Support-Ticket —
// siehe Aufgabe #32). Eine Standard-Glocke dient als Fallback, falls künftig
// ein neuer Typ hinzukommt.
const ICONS: Record<NotificationType, typeof Calendar> = {
  NEW_BOOKING_REQUEST: Calendar,
  NEW_CHAT_MESSAGE: MessageCircle,
  ADMIN_NEW_PENDING_BIKE: Bike,
  ADMIN_NEW_REPORT: Flag,
  ADMIN_NEW_SUPPORT_TICKET: LifeBuoy,
  SUPPORT_TICKET_REPLY: LifeBuoy,
  PAYMENT_RECEIPT_SUBMITTED: Banknote,
  PAYMENT_CONFIRMED: Banknote,
  REVIEW_AVAILABLE: Star,
};

const COLOR_CLASSES: Record<NotificationType, string> = {
  NEW_BOOKING_REQUEST: "bg-brand-100 text-brand-700",
  NEW_CHAT_MESSAGE: "bg-blue-100 text-blue-700",
  ADMIN_NEW_PENDING_BIKE: "bg-amber-100 text-amber-700",
  ADMIN_NEW_REPORT: "bg-red-100 text-red-700",
  ADMIN_NEW_SUPPORT_TICKET: "bg-purple-100 text-purple-700",
  SUPPORT_TICKET_REPLY: "bg-purple-100 text-purple-700",
  PAYMENT_RECEIPT_SUBMITTED: "bg-emerald-100 text-emerald-700",
  PAYMENT_CONFIRMED: "bg-emerald-100 text-emerald-700",
  REVIEW_AVAILABLE: "bg-yellow-100 text-yellow-700",
};

export function NotificationIcon({ type, className }: NotificationIconProps) {
  const Icon = ICONS[type] ?? Bell;
  const colorClass = COLOR_CLASSES[type] ?? "bg-slate-100 text-slate-600";

  return (
    <span
      className={cn(
        "shrink-0 w-9 h-9 rounded-full flex items-center justify-center",
        colorClass,
        className
      )}
    >
      <Icon size={16} />
    </span>
  );
}
