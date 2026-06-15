import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import { getTranslations } from "next-intl/server";
import { Bike, Users, Star } from "lucide-react";
import { Button } from "@/components/ui/Button";
import type { Metadata } from "next";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("home.hero");
  return { title: t("title") };
}

/**
 * Landing page — server component.
 * Startseite — Server-Komponente.
 */
export default function HomePage() {
  return (
    <div>
      <HeroSection />
      <FeaturesSection />
    </div>
  );
}

function HeroSection() {
  // eslint-disable-next-line react-hooks/rules-of-hooks
  const t = useTranslations("home.hero");
  // eslint-disable-next-line react-hooks/rules-of-hooks
  const locale = useLocale();

  return (
    <section className="relative overflow-hidden bg-gradient-to-br from-brand-50 via-white to-green-50 py-24 md:py-36">
      <div className="mx-auto max-w-4xl px-4 sm:px-6 lg:px-8 text-center">
        <div className="inline-flex items-center gap-2 rounded-full bg-brand-100 px-4 py-1.5 text-sm font-medium text-brand-700 mb-8">
          <Bike size={16} />
          Peer-to-peer bike rental
        </div>

        <h1 className="text-5xl md:text-6xl font-bold text-slate-900 mb-6 leading-tight">
          {t("title")}
        </h1>

        <p className="text-xl text-slate-600 mb-10 max-w-2xl mx-auto">
          {t("subtitle")}
        </p>

        <div className="flex flex-wrap gap-4 justify-center">
          <Button size="lg" asChild>
            <Link href={`/${locale}/bikes`}>{t("cta")}</Link>
          </Button>
          <Button variant="outline" size="lg" asChild>
            <Link href={`/${locale}/dashboard/bikes/new`}>{t("ctaOwner")}</Link>
          </Button>
        </div>

        <div className="mt-16 flex flex-wrap items-center justify-center gap-8 text-sm text-slate-500">
          <div className="flex items-center gap-2">
            <Users size={16} className="text-brand-500" />
            <span>100% peer-to-peer</span>
          </div>
          <div className="flex items-center gap-2">
            <Star size={16} className="text-amber-500" />
            <span>Verified reviews</span>
          </div>
          <div className="flex items-center gap-2">
            <Bike size={16} className="text-brand-500" />
            <span>All bike types</span>
          </div>
        </div>
      </div>
    </section>
  );
}

function FeaturesSection() {
  // eslint-disable-next-line react-hooks/rules-of-hooks
  const t = useTranslations("home.features");

  const features = [
    {
      icon: <Users size={28} className="text-brand-600" />,
      title: t("peer"),
      desc: t("peerDesc"),
    },
    {
      icon: <Bike size={28} className="text-brand-600" />,
      title: t("flexible"),
      desc: t("flexibleDesc"),
    },
    {
      icon: <Star size={28} className="text-amber-500" />,
      title: t("trusted"),
      desc: t("trustedDesc"),
    },
  ];

  return (
    <section className="py-20 bg-white">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <h2 className="text-3xl font-bold text-center text-slate-900 mb-12">
          {t("title")}
        </h2>

        <div className="grid md:grid-cols-3 gap-8">
          {features.map((f) => (
            <div key={f.title} className="card p-8 text-center">
              <div className="mx-auto mb-4 w-14 h-14 rounded-2xl bg-brand-50 flex items-center justify-center">
                {f.icon}
              </div>
              <h3 className="text-lg font-semibold text-slate-900 mb-2">{f.title}</h3>
              <p className="text-slate-600 text-sm">{f.desc}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
