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
import type { BookingResponse } from "@/types";

/**
 * Rental calendar — simple month grid showing booking counts per day,
 * click a day to see the bookings list. Stage 3 "Business accounts".
 * Mietkalender — einfache Monatsansicht mit Buchungsanzahl pro Tag.
 */
export default function RentalCalendarPage() {
  const t = useTranslations("business.calendar");
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
            className="p-2 rounded-lg hover:bg-slate-100 transition-colors"
          >
            <ChevronLeft size={18} />
          </button>
          <span className="text-sm font-medium text-slate-900 w-32 text-center">
            {format(monthCursor, "MMMM yyyy", { locale: dfnsLocale })}
          </span>
          <button
            type="button"
            onClick={() => setMonthCursor((m) => addMonths(m, 1))}
            aria-label={t("nextMonth")}
            className="p-2 rounded-lg hover:bg-slate-100 transition-colors"
          >
            <ChevronRight size={18} />
          </button>
        </div>
      </div>

      <div className="card p-4">
        <div className="grid grid-cols-7 gap-1 mb-2">
          {days.slice(0, 7).map((d) => (
            <div key={d.toISOString()} className="text-xs font-medium text-slate-400 text-center py-1">
              {format(d, "EEE", { locale: dfnsLocale })}
            </div>
          ))}
        </div>
        <div className="grid grid-cols-7 gap-1">
          {days.map((day) => {
            const count = bookingsOnDay(day).length;
            const inMonth = isSameMonth(day, monthCursor);
            const selected = selectedDay && isSameDay(day, selectedDay);
            return (
              <button
                key={day.toISOString()}
                type="button"
                onClick={() => setSelectedDay(day)}
                className={cn(
                  "aspect-square rounded-lg p-1.5 flex flex-col items-start justify-between text-left transition-colors",
                  inMonth ? "bg-white" : "bg-slate-50 text-slate-400",
                  selected ? "ring-2 ring-brand-500" : "hover:bg-slate-50",
                  "border border-slate-100"
                )}
              >
                <span className="text-xs font-medium">{format(day, "d")}</span>
                {count > 0 && (
                  <span className="text-[11px] font-semibold px-1.5 py-0.5 rounded-full bg-brand-100 text-brand-700">
                    {count}
                  </span>
                )}
              </button>
            );
          })}
        </div>
      </div>

      {selectedDay && (
        <div className="card p-5 space-y-3">
          <h2 className="font-semibold text-slate-900">
            {t("bookingsOnDay", { date: format(selectedDay, "PPP", { locale: dfnsLocale }) })}
          </h2>
          {selectedBookings.length === 0 ? (
            <p className="text-sm text-slate-500">{t("noBookings")}</p>
          ) : (
            <div className="space-y-2">
              {selectedBookings.map((b) => (
                <div key={b.id} className="flex items-center justify-between gap-3 rounded-xl border border-slate-100 p-3">
                  <div>
                    <p className="font-medium text-slate-900 text-sm">{b.bikeTitle}</p>
                    <p className="text-xs text-slate-500">
                      {b.renterName} · {format(parseISO(b.startDate), "MMM d")} – {format(parseISO(b.endDate), "MMM d")}
                    </p>
                  </div>
                  <span className="text-sm font-semibold text-slate-900">{formatPrice(b.totalPrice)}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
