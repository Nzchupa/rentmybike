import { Calendar, MessageCircle, Bell } from "lucide-react";
import { cn } from "@/lib/utils";
import type { NotificationType } from "@/types";

interface NotificationIconProps {
  type: NotificationType;
  className?: string;
}

// Only two notification types exist in the backend today (see
// NotificationType in types/index.ts) — booking requests and chat messages.
// A "default" bell is kept as a fallback so a future type added on the
// backend doesn't render blank here.
// Es gibt derzeit nur zwei Benachrichtigungstypen im Backend — Buchungs-
// anfragen und Chat-Nachrichten. Eine Standard-Glocke dient als Fallback,
// falls künftig ein neuer Typ im Backend hinzukommt.
const ICONS: Record<NotificationType, typeof Calendar> = {
  NEW_BOOKING_REQUEST: Calendar,
  NEW_CHAT_MESSAGE: MessageCircle,
};

const COLOR_CLASSES: Record<NotificationType, string> = {
  NEW_BOOKING_REQUEST: "bg-brand-100 text-brand-700",
  NEW_CHAT_MESSAGE: "bg-blue-100 text-blue-700",
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
