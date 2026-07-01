import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import { getTranslations } from "next-intl/server";
import { Mail, LifeBuoy } from "lucide-react";
import { Button } from "@/components/ui/Button";
import type { Metadata } from "next";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("pages.contact");
  return { title: t("title") };
}

export default function ContactPage() {
  // eslint-disable-next-line react-hooks/rules-of-hooks
  const t = useTranslations("pages.contact");
  // eslint-disable-next-line react-hooks/rules-of-hooks
  const locale = useLocale();

  return (
    <div className="mx-auto max-w-3xl px-4 sm:px-6 lg:px-8 py-16">
      <h1 className="text-3xl font-bold text-slate-900 mb-6">{t("title")}</h1>
      <div className="space-y-4 text-slate-600 leading-relaxed">
        <p>{t("body1")}</p>

        {/* Tracked support tickets are now the preferred channel for
            logged-in users (subject/category/thread/status instead of a
            one-off email) — the direct email stays below as a fallback for
            anyone not logged in, or for anything outside the app. /
            Nachverfolgbare Support-Tickets sind jetzt der bevorzugte Kanal
            für angemeldete Benutzer — die direkte E-Mail bleibt als
            Fallback für nicht angemeldete Besucher oder Anliegen außerhalb
            der App. */}
        <div className="card p-5 flex items-center justify-between gap-4 flex-wrap not-prose">
          <div className="flex items-start gap-3">
            <LifeBuoy size={20} className="text-brand-600 shrink-0 mt-0.5" />
            <div>
              <p className="font-medium text-slate-900">{t("ticketTitle")}</p>
              <p className="text-sm text-slate-500 mt-0.5">{t("ticketBody")}</p>
            </div>
          </div>
          <Button asChild size="sm">
            <Link href={`/${locale}/dashboard/support`}>{t("ticketCta")}</Link>
          </Button>
        </div>

        <div className="flex items-center gap-2 text-slate-900 font-medium">
          <Mail size={18} className="text-brand-600" />
          <span>{t("emailLabel")}: nazarchuprii@gmail.com</span>
        </div>
        <p>{t("body2")}</p>
      </div>
    </div>
  );
}
