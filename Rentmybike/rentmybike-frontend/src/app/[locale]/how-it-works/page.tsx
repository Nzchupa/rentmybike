import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import { getTranslations } from "next-intl/server";
import { Search, Send, Handshake, Camera, Star, MessageCircle, Bike, Wallet } from "lucide-react";
import { Button } from "@/components/ui/Button";
import type { Metadata } from "next";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("pages.howItWorks");
  return { title: t("title") };
}

/**
 * Dedicated "How RentMyBike Works" page (spec item #7) — the homepage's
 * how-it-works/safety sections only give a 3-step teaser; this page spells
 * out the full renter and owner flows plus the trust/safety features in
 * one place, so it can be linked from the footer and from onboarding.
 *
 * Eigene "So funktioniert RentMyBike"-Seite — die Sektionen auf der
 * Startseite sind nur ein 3-Schritte-Teaser; diese Seite erklärt den
 * vollständigen Mieter- und Eigentümer-Ablauf sowie die
 * Vertrauens-/Sicherheitsfunktionen an einem Ort.
 */
export default function HowItWorksPage() {
  // eslint-disable-next-line react-hooks/rules-of-hooks
  const t = useTranslations("pages.howItWorks");
  // eslint-disable-next-line react-hooks/rules-of-hooks
  const locale = useLocale();

  const renterSteps = [
    { icon: Search, title: t("renterStep1Title"), desc: t("renterStep1Desc") },
    { icon: Send, title: t("renterStep2Title"), desc: t("renterStep2Desc") },
    { icon: Handshake, title: t("renterStep3Title"), desc: t("renterStep3Desc") },
  ];

  const ownerSteps = [
    { icon: Bike, title: t("ownerStep1Title"), desc: t("ownerStep1Desc") },
    { icon: Handshake, title: t("ownerStep2Title"), desc: t("ownerStep2Desc") },
    { icon: Wallet, title: t("ownerStep3Title"), desc: t("ownerStep3Desc") },
  ];

  const safety = [
    { icon: Camera, title: t("safetyPhotosTitle"), desc: t("safetyPhotosDesc") },
    { icon: Star, title: t("safetyReviewsTitle"), desc: t("safetyReviewsDesc") },
    { icon: MessageCircle, title: t("safetyChatTitle"), desc: t("safetyChatDesc") },
  ];

  return (
    <div className="mx-auto max-w-5xl px-4 sm:px-6 lg:px-8 py-16">
      <div className="text-center mb-16">
        <h1 className="text-3xl md:text-4xl font-bold text-slate-900 mb-3">{t("title")}</h1>
        <p className="text-slate-600 max-w-2xl mx-auto">{t("subtitle")}</p>
      </div>

      {/* Renters */}
      <section className="mb-16">
        <h2 className="text-2xl font-semibold text-slate-900 mb-2">{t("forRentersTitle")}</h2>
        <p className="text-slate-500 mb-8">{t("forRentersSubtitle")}</p>
        <div className="grid md:grid-cols-3 gap-6">
          {renterSteps.map((s, i) => (
            <div key={s.title} className="card p-6 relative">
              <div className="absolute -top-3 -left-3 w-7 h-7 rounded-full bg-brand-600 text-white text-xs font-bold flex items-center justify-center">
                {i + 1}
              </div>
              <s.icon size={24} className="text-brand-600 mb-3" />
              <h3 className="font-semibold text-slate-900 mb-1">{s.title}</h3>
              <p className="text-sm text-slate-600">{s.desc}</p>
            </div>
          ))}
        </div>
        <div className="mt-6">
          <Button asChild>
            <Link href={`/${locale}/bikes`}>{t("forRentersCta")}</Link>
          </Button>
        </div>
      </section>

      {/* Owners */}
      <section className="mb-16">
        <h2 className="text-2xl font-semibold text-slate-900 mb-2">{t("forOwnersTitle")}</h2>
        <p className="text-slate-500 mb-8">{t("forOwnersSubtitle")}</p>
        <div className="grid md:grid-cols-3 gap-6">
          {ownerSteps.map((s, i) => (
            <div key={s.title} className="card p-6 relative">
              <div className="absolute -top-3 -left-3 w-7 h-7 rounded-full bg-brand-600 text-white text-xs font-bold flex items-center justify-center">
                {i + 1}
              </div>
              <s.icon size={24} className="text-brand-600 mb-3" />
              <h3 className="font-semibold text-slate-900 mb-1">{s.title}</h3>
              <p className="text-sm text-slate-600">{s.desc}</p>
            </div>
          ))}
        </div>
        <div className="mt-6">
          <Button variant="outline" asChild>
            <Link href={`/${locale}/dashboard/bikes/new`}>{t("forOwnersCta")}</Link>
          </Button>
        </div>
      </section>

      {/* Safety */}
      <section>
        <h2 className="text-2xl font-semibold text-slate-900 mb-2">{t("safetyTitle")}</h2>
        <p className="text-slate-500 mb-8">{t("safetySubtitle")}</p>
        <div className="grid md:grid-cols-3 gap-6">
          {safety.map((s) => (
            <div key={s.title} className="card p-6">
              <s.icon size={24} className="text-brand-600 mb-3" />
              <h3 className="font-semibold text-slate-900 mb-1">{s.title}</h3>
              <p className="text-sm text-slate-600">{s.desc}</p>
            </div>
          ))}
        </div>
      </section>

      <div className="mt-16 text-center">
        <p className="text-sm text-slate-500 mb-3">{t("faqPrompt")}</p>
        <Button variant="outline" asChild>
          <Link href={`/${locale}/faq`}>{t("faqCta")}</Link>
        </Button>
      </div>
    </div>
  );
}
