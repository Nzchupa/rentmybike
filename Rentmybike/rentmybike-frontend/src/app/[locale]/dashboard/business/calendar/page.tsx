"use client";

import { useMemo, useState } from "react";
import { useLocale, useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import {
  addMonths,
  eachDayOfInterval,
  endOfMonth,
  endOfWeek,
  format,
  isSameDay,
  isSameMonth,
  isWithinInterval,
  parseISO,
  startOfMonth,
  startOfWeek,
  subMonths,
} from "date-fns";
import { enUS, de } from "date-fns/locale";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { businessApi } from "@/lib/api";
import { cn, formatPrice } from "@/lib/utils";
import { BookingStatusBadge } from "@/components/ui/Badge";
import type { BookingResponse, BookingStatus } from "@/types";

// Same status → color mapping as the shared Badge component, just as solid
// dots instead of pill backgrounds, so the calendar grid stays legible at
// a glance (spec item #5 — color-coded booking status visualization).
// Gleiche Status-Farbzuordnung wie die Badge-Komponente, hier als Punkte
// statt Pillen, damit das Kalenderraster auf einen Blick lesbar bleibt.
const statusDotClass: Record<BookingStatus, string> = {
  PENDING: "bg-yellow-500",
  ACCEPTED: "bg-blue-500",
  REJECTED: "bg-red-500",
  CANCELLED: "bg-slate-400",
  COMPLETED: "bg-green-500",
};

const LEGEND_STATUSES: BookingStatus[] = ["PENDING", "ACCEPTED", "COMPLETED", "CANCELLED", "REJECTED"];

/**
 * Rental calendar — simple month grid showing booking counts per day,
 * click a day to see the bookings list. Stage 3 "Business accounts".
 * Mietkalender — einfache Monatsansicht mit Buchungsanzahl pro Tag.
 */
export default function RentalCalendarPage() {
  const t = useTranslations("business.calendar");
  const tStatus = useTranslations("booking.status");
  const locale = useLocale();
  const dfnsLocale = locale === "de" ? de : enUS;

  const [monthCursor, setMonthCursor] = useState(() => new Date());
  const [selectedDay, setSelectedDay] = useState<Date | null>(null);

  const monthStart = startOfMonth(monthCursor);
  const monthEnd = endOfMonth(monthCursor);
  const gridStart = startOfWeek(monthStart, { weekStartsOn: 1 });
  const gridEnd = endOfWeek(monthEnd, { weekStartsOn: 1 });

  const days = useMemo(
    () => eachDayOfInterval({ start: gridStart, end: gridEnd }),
    [gridStart, gridEnd]
  );

  const { data: bookings } = useQuery({
    queryKey: ["business-calendar", format(gridStart, "yyyy-MM-dd"), format(gridEnd, "yyyy-MM-dd")],
    queryFn: () =>
      businessApi.getBookingCalendar(format(gridStart, "yyyy-MM-dd"), format(gridEnd, "yyyy-MM-dd")),
    select: (r) => r.data.data,
  });

  function bookingsOnDay(day: Date): BookingResponse[] {
    if (!bookings) return [];
    return bookings.filter((b) =>
      isWithinInterval(day, { start: parseISO(b.startDate), end: parseISO(b.endDate) })
    );
  }

  const selectedBookings = selectedDay ? bookingsOnDay(selectedDay) : [];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="section-title">{t("title")}</h1>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setMonthCursor((m) => subMonths(m, 1))}
            aria-label={t("prevMonth")}
            className="p-2 rounded-lg text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 transition-colors"
          >
            <ChevronLeft size={18} />
          </button>
          <span className="text-sm font-medium text-slate-900 dark:text-slate-100 w-32 text-center">
            {format(monthCursor, "MMMM yyyy", { locale: dfnsLocale })}
          </span>
          <button
            type="button"
            onClick={() => setMonthCursor((m) => addMonths(m, 1))}
            aria-label={t("nextMonth")}
            className="p-2 rounded-lg text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 transition-colors"
          >
            <ChevronRight size={18} />
          </button>
        </div>
      </div>

      <div className="card p-4">
        <div className="grid grid-cols-7 gap-1 mb-2">
          {days.slice(0, 7).map((d) => (
            <div key={d.toISOString()} className="text-xs font-medium text-slate-400 dark:text-slate-500 text-center py-1">
              {format(d, "EEE", { locale: dfnsLocale })}
            </div>
          ))}
        </div>
        <div className="grid grid-cols-7 gap-1">
          {days.map((day) => {
            const dayBookings = bookingsOnDay(day);
            const statusesPresent = Array.from(new Set(dayBookings.map((b) => b.status)));
            const inMonth = isSameMonth(day, monthCursor);
            const selected = selectedDay && isSameDay(day, selectedDay);
            return (
              <button
                key={day.toISOString()}
                type="button"
                onClick={() => setSelectedDay(day)}
                className={cn(
                  "aspect-square rounded-lg p-1.5 flex flex-col items-start justify-between text-left transition-colors",
                  inMonth ? "bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100" : "bg-slate-50 dark:bg-slate-900/40 text-slate-400 dark:text-slate-600",
                  selected ? "ring-2 ring-brand-500" : "hover:bg-slate-50 dark:hover:bg-slate-700/50",
                  "border border-slate-100 dark:border-slate-700"
                )}
              >
                <span className="text-xs font-medium">{format(day, "d")}</span>
                {statusesPresent.length > 0 && (
                  <span className="flex flex-wrap items-center gap-0.5">
                    {statusesPresent.slice(0, 4).map((status) => (
                      <span
                        key={status}
                        className={cn("w-1.5 h-1.5 rounded-full", statusDotClass[status])}
                      />
                    ))}
                  </span>
                )}
              </button>
            );
          })}
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-x-4 gap-y-1.5 text-xs text-slate-500 dark:text-slate-400">
        {LEGEND_STATUSES.map((status) => (
          <span key={status} className="flex items-center gap-1.5">
            <span className={cn("w-1.5 h-1.5 rounded-full", statusDotClass[status])} />
            {tStatus(status)}
          </span>
        ))}
      </div>

      {selectedDay && (
        <div className="card p-5 space-y-3">
          <h2 className="font-semibold text-slate-900 dark:text-slate-100">
            {t("bookingsOnDay", { date: format(selectedDay, "PPP", { locale: dfnsLocale }) })}
          </h2>
          {selectedBookings.length === 0 ? (
            <p className="text-sm text-slate-500 dark:text-slate-400">{t("noBookings")}</p>
          ) : (
            <div className="space-y-2">
              {selectedBookings.map((b) => (
                <div key={b.id} className="flex items-center justify-between gap-3 rounded-xl border border-slate-100 dark:border-slate-700 p-3">
                  <div>
                    <div className="flex items-center gap-2 mb-0.5">
                      <p className="font-medium text-slate-900 dark:text-slate-100 text-sm">{b.bikeTitle}</p>
                      <BookingStatusBadge status={b.status} />
                    </div>
                    <p className="text-xs text-slate-500 dark:text-slate-400">
                      {b.renterName} · {format(parseISO(b.startDate), "MMM d")} – {format(parseISO(b.endDate), "MMM d")}
                    </p>
                  </div>
                  <span className="text-sm font-semibold text-slate-900 dark:text-slate-100">{formatPrice(b.totalPrice)}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
