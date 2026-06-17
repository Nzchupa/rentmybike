"use client";

import { useState } from "react";
import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { DayPicker } from "react-day-picker";
import { format, isBefore, parseISO, startOfToday } from "date-fns";
import { de, enUS } from "date-fns/locale";
import toast from "react-hot-toast";
import "react-day-picker/dist/style.css";
import { bookingsApi } from "@/lib/api";
import { useAuthStore } from "@/store/auth.store";
import { Button } from "@/components/ui/Button";
import { formatPrice, calcRentalDays, calcTotalPrice, toIsoDate } from "@/lib/utils";
import type { BikeResponse } from "@/types";

interface BookingFormProps {
  bike: BikeResponse;
}

/**
 * Booking form with date range picker and price calculation.
 * Buchungsformular mit Datumsbereichsauswahl und Preisberechnung.
 *
 * Embedded in the bike detail page right column.
 * Im rechten Bereich der Fahrrad-Detailseite eingebettet.
 */
export function BookingForm({ bike }: BookingFormProps) {
  const t = useTranslations("booking.form");
  const te = useTranslations("booking.errors");
  const tNav = useTranslations("nav");
  const locale = useLocale();
  const { user } = useAuthStore();
  const queryClient = useQueryClient();

  const [range, setRange] = useState<{ from?: Date; to?: Date }>({});
  const [message, setMessage] = useState("");
  const [showCalendar, setShowCalendar] = useState(false);

  // Occupied date ranges for this bike — used to disable already-booked dates
  // in the calendar and to validate overlap before submitting, instead of
  // only finding out via a generic error toast after the POST fails.
  // Belegte Datumsbereiche für dieses Fahrrad — werden verwendet, um bereits
  // gebuchte Termine im Kalender zu deaktivieren und Überschneidungen vor dem
  // Senden zu validieren, statt dies erst nach einem fehlgeschlagenen POST
  // über eine generische Fehlermeldung zu erfahren.
  const { data: bookedRanges = [] } = useQuery({
    queryKey: ["booked-dates", bike.id],
    queryFn: () => bookingsApi.getBookedDates(bike.id),
    select: (r) => r.data.data ?? [],
    staleTime: 60 * 1000,
  });

  const { mutate: createBooking, isPending } = useMutation({
    mutationFn: bookingsApi.create,
    onSuccess: () => {
      toast.success(t("requestSent"));
      setRange({});
      setMessage("");
      setShowCalendar(false);
      // Query key mismatch bug: this used to invalidate ["my-renter-bookings"],
      // but RenterBookingsPage (dashboard/bookings/renter/page.tsx) actually
      // queries under ["renter-bookings"]. Since the keys never matched, the
      // renter's booking list cache was never invalidated after a successful
      // request, so the newly booked bike didn't appear in "My Rentals" until
      // the 60s staleTime lapsed or a hard refresh happened — looking exactly
      // like "bike missing from the list after booking".
      // Schlüssel-Diskrepanz-Bug: dies hat vorher ["my-renter-bookings"]
      // invalidiert, aber RenterBookingsPage (dashboard/bookings/renter/
      // page.tsx) fragt tatsächlich unter ["renter-bookings"] ab. Da die
      // Schlüssel nie übereinstimmten, wurde der Buchungslisten-Cache des
      // Mieters nach einer erfolgreichen Anfrage nie invalidiert, sodass das
      // neu gebuchte Fahrrad erst nach Ablauf der 60s staleTime oder einem
      // harten Refresh in "Meine Mieten" erschien.
      queryClient.invalidateQueries({ queryKey: ["renter-bookings"] });
      // Also invalidate the booked-date ranges for this bike so the calendar
      // immediately reflects the new reservation if the user stays on the page.
      // Auch die belegten Datumsbereiche für dieses Fahrrad invalidieren,
      // damit der Kalender die neue Reservierung sofort widerspiegelt, falls
      // der Benutzer auf der Seite bleibt.
      queryClient.invalidateQueries({ queryKey: ["booked-dates", bike.id] });
    },
    onError: (err: Error) => {
      toast.error(err.message);
    },
  });

  if (!user) {
    return (
      <div className="card p-6 text-center space-y-4">
        <p className="text-slate-600">{t("loginRequired")}</p>
        <Button asChild className="w-full">
          <Link href={`/${locale}/auth/login`}>{tNav("login")}</Link>
        </Button>
      </div>
    );
  }

  const today = startOfToday();
  const { from: startDate, to: endDate } = range;
  const rentalDays =
    startDate && endDate ? calcRentalDays(toIsoDate(startDate), toIsoDate(endDate)) : 0;
  const totalPrice =
    startDate && endDate
      ? calcTotalPrice(bike.pricePerDay, toIsoDate(startDate), toIsoDate(endDate))
      : 0;

  // Date ranges already booked (PENDING/ACCEPTED), parsed for the calendar's
  // `disabled` prop and for the overlap check below.
  const bookedIntervals = bookedRanges.map((r) => ({
    from: parseISO(r.startDate),
    to: parseISO(r.endDate),
  }));

  function rangeOverlapsBooked(s: Date, e: Date): boolean {
    const sIso = toIsoDate(s);
    const eIso = toIsoDate(e);
    return bookedRanges.some((r) => sIso <= r.endDate && eIso >= r.startDate);
  }

  const pastDateError = !!startDate && isBefore(startDate, today);
  const endBeforeStartError = !!startDate && !!endDate && isBefore(endDate, startDate);
  const dateConflictError =
    !!startDate && !!endDate && !endBeforeStartError && rangeOverlapsBooked(startDate, endDate);

  const dateError = pastDateError
    ? te("pastDate")
    : endBeforeStartError
      ? te("endBeforeStart")
      : dateConflictError
        ? te("dateConflict")
        : null;

  const canSubmit =
    !!startDate && !!endDate && !pastDateError && !endBeforeStartError && !dateConflictError;

  function handleSubmit() {
    if (!startDate || !endDate || !canSubmit) return;
    createBooking({
      bikeId: bike.id,
      startDate: toIsoDate(startDate),
      endDate: toIsoDate(endDate),
      message: message.trim() || undefined,
    });
  }

  return (
    <div className="card p-6 space-y-4">
      <h2 className="text-lg font-semibold text-slate-900">{t("title")}</h2>

      {/* Price */}
      <div className="text-3xl font-bold text-brand-600">
        {formatPrice(bike.pricePerDay, locale)}
        <span className="text-base font-normal text-slate-500"> {t("pricePerDay")}</span>
      </div>

      {/* Date selection */}
      <div>
        <button
          type="button"
          onClick={() => setShowCalendar(!showCalendar)}
          className="w-full flex items-center justify-between rounded-xl border border-slate-300 px-4 py-3 text-sm text-left hover:border-brand-500 transition"
        >
          <div>
            <div className="text-xs text-slate-500 mb-0.5">{t("startDate")}</div>
            <div className={startDate ? "text-slate-900 font-medium" : "text-slate-400"}>
              {startDate ? format(startDate, "dd MMM yyyy") : t("selectDate")}
            </div>
          </div>
          <div className="w-px h-8 bg-slate-200 mx-3" />
          <div>
            <div className="text-xs text-slate-500 mb-0.5">{t("endDate")}</div>
            <div className={endDate ? "text-slate-900 font-medium" : "text-slate-400"}>
              {endDate ? format(endDate, "dd MMM yyyy") : t("selectDate")}
            </div>
          </div>
        </button>

        {/* Calendar */}
        {showCalendar && (
          <div className="mt-2 rounded-2xl border border-slate-200 p-3 bg-white shadow-lg">
            <DayPicker
              mode="range"
              selected={{ from: range.from, to: range.to }}
              onSelect={(r) => setRange({ from: r?.from, to: r?.to })}
              disabled={[{ before: today }, ...bookedIntervals]}
              locale={locale === "de" ? de : enUS}
              className="!font-sans"
            />
            <Button
              size="sm"
              variant="ghost"
              className="w-full mt-2"
              onClick={() => setShowCalendar(false)}
            >
              {t("done")}
            </Button>
          </div>
        )}

        {dateError && (
          <p className="mt-2 text-sm text-red-600">{dateError}</p>
        )}
      </div>

      {/* Message */}
      <div>
        <label className="label">{t("message")}</label>
        <textarea
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          rows={3}
          maxLength={500}
          placeholder={t("messagePlaceholder")}
          className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm resize-none outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500"
        />
      </div>

      {/* Price summary */}
      {canSubmit && (
        <div className="rounded-xl bg-slate-50 p-4 space-y-2 text-sm">
          <div className="flex justify-between">
            <span className="text-slate-600">{t("pricePerDay")}</span>
            <span>{formatPrice(bike.pricePerDay, locale)}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-slate-600">× {t("days", { count: rentalDays })}</span>
            <span />
          </div>
          <div className="border-t border-slate-200 pt-2 flex justify-between font-semibold">
            <span>{t("totalPrice")}</span>
            <span className="text-brand-600">{formatPrice(totalPrice, locale)}</span>
          </div>
        </div>
      )}

      <Button
        onClick={handleSubmit}
        disabled={!canSubmit}
        loading={isPending}
        className="w-full"
        size="lg"
      >
        {t("submit")}
      </Button>
    </div>
  );
}
