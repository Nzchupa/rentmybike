import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import { getTranslations } from "next-intl/server";
import {
  Bike,
  Users,
  Star,
  Search,
  Send,
  Handshake,
  Camera,
  MessageCircle,
  LayoutDashboard,
  CalendarDays,
  PackageSearch,
  ShieldCheck,
} from "lucide-react";
import { Button } from "@/components/ui/Button";
import { PopularBikes } from "@/components/home/PopularBikes";
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
      <HowItWorksSection />
      <PopularBikes />
      <FeaturesSection />
      <SafetySection />
      <AudienceSection />
      <BusinessCtaSection />
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

function HowItWorksSection() {
  // eslint-disable-next-line react-hooks/rules-of-hooks
  const t = useTranslations("home.howItWorks");

  const steps = [
    { icon: <Search size={24} className="text-brand-600" />, title: t("step1Title"), desc: t("step1Desc") },
    { icon: <Send size={24} className="text-brand-600" />, title: t("step2Title"), desc: t("step2Desc") },
    { icon: <Handshake size={24} className="text-brand-600" />, title: t("step3Title"), desc: t("step3Desc") },
  ];

  return (
    <section className="py-20 bg-slate-50">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <h2 className="text-3xl font-bold text-center text-slate-900 mb-12">{t("title")}</h2>
        <div className="grid md:grid-cols-3 gap-8">
          {steps.map((s, i) => (
            <div key={s.title} className="relative text-center">
              <div className="mx-auto mb-4 w-14 h-14 rounded-2xl bg-white shadow-sm flex items-center justify-center">
                {s.icon}
              </div>
              <div className="mx-auto mb-3 w-6 h-6 rounded-full bg-brand-600 text-white text-xs font-bold flex items-center justify-center">
                {i + 1}
              </div>
              <h3 className="text-lg font-semibold text-slate-900 mb-2">{s.title}</h3>
              <p className="text-slate-600 text-sm max-w-xs mx-auto">{s.desc}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

function SafetySection() {
  // eslint-disable-next-line react-hooks/rules-of-hooks
  const t = useTranslations("home.safety");

  const items = [
    { icon: <Camera size={24} className="text-brand-600" />, title: t("photosTitle"), desc: t("photosDesc") },
    { icon: <Star size={24} className="text-amber-500" />, title: t("reviewsTitle"), desc: t("reviewsDesc") },
    { icon: <MessageCircle size={24} className="text-brand-600" />, title: t("directTitle"), desc: t("directDesc") },
  ];

  return (
    <section className="py-20 bg-white">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="text-center mb-12">
          <div className="inline-flex items-center gap-2 rounded-full bg-green-50 px-4 py-1.5 text-sm font-medium text-green-700 mb-4">
            <ShieldCheck size={16} />
            {t("title")}
          </div>
          <p className="text-slate-600 max-w-xl mx-auto">{t("subtitle")}</p>
        </div>
        <div className="grid md:grid-cols-3 gap-8">
          {items.map((item) => (
            <div key={item.title} className="card p-8 text-center">
              <div className="mx-auto mb-4 w-14 h-14 rounded-2xl bg-brand-50 flex items-center justify-center">
                {item.icon}
              </div>
              <h3 className="text-lg font-semibold text-slate-900 mb-2">{item.title}</h3>
              <p className="text-slate-600 text-sm">{item.desc}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

function AudienceSection() {
  // eslint-disable-next-line react-hooks/rules-of-hooks
  const t = useTranslations("home.audience");
  // eslint-disable-next-line react-hooks/rules-of-hooks
  const locale = useLocale();

  return (
    <section className="py-20 bg-slate-50">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="grid md:grid-cols-2 gap-8">
          <div className="card p-8">
            <h3 className="text-2xl font-bold text-slate-900 mb-1">{t("ownersTitle")}</h3>
            <p className="text-slate-600 mb-6">{t("ownersDesc")}</p>
            <ul className="space-y-3 mb-8 text-slate-700">
              <li className="flex items-start gap-2">
                <span className="mt-1.5 w-1.5 h-1.5 rounded-full bg-brand-600 shrink-0" />
                {t("owner1")}
              </li>
              <li className="flex items-start gap-2">
                <span className="mt-1.5 w-1.5 h-1.5 rounded-full bg-brand-600 shrink-0" />
                {t("owner2")}
              </li>
              <li className="flex items-start gap-2">
                <span className="mt-1.5 w-1.5 h-1.5 rounded-full bg-brand-600 shrink-0" />
                {t("owner3")}
              </li>
            </ul>
            <Button asChild>
              <Link href={`/${locale}/dashboard/bikes/new`}>{t("ownersCta")}</Link>
            </Button>
          </div>

          <div className="card p-8">
            <h3 className="text-2xl font-bold text-slate-900 mb-1">{t("rentersTitle")}</h3>
            <p className="text-slate-600 mb-6">{t("rentersDesc")}</p>
            <ul className="space-y-3 mb-8 text-slate-700">
              <li className="flex items-start gap-2">
                <span className="mt-1.5 w-1.5 h-1.5 rounded-full bg-brand-600 shrink-0" />
                {t("renter1")}
              </li>
              <li className="flex items-start gap-2">
                <span className="mt-1.5 w-1.5 h-1.5 rounded-full bg-brand-600 shrink-0" />
                {t("renter2")}
              </li>
              <li className="flex items-start gap-2">
                <span className="mt-1.5 w-1.5 h-1.5 rounded-full bg-brand-600 shrink-0" />
                {t("renter3")}
              </li>
            </ul>
            <Button variant="outline" asChild>
              <Link href={`/${locale}/bikes`}>{t("rentersCta")}</Link>
            </Button>
          </div>
        </div>
      </div>
    </section>
  );
}

function BusinessCtaSection() {
  // eslint-disable-next-line react-hooks/rules-of-hooks
  const t = useTranslations("home.business");
  // eslint-disable-next-line react-hooks/rules-of-hooks
  const locale = useLocale();

  const items = [
    { icon: <LayoutDashboard size={18} className="text-brand-600" />, label: t("item1") },
    { icon: <CalendarDays size={18} className="text-brand-600" />, label: t("item2") },
    { icon: <PackageSearch size={18} className="text-brand-600" />, label: t("item3") },
    { icon: <ShieldCheck size={18} className="text-brand-600" />, label: t("item4") },
  ];

  return (
    <section className="py-20 bg-gradient-to-br from-slate-900 to-slate-800">
      <div className="mx-auto max-w-5xl px-4 sm:px-6 lg:px-8 text-center">
        <h2 className="text-3xl font-bold text-white mb-3">{t("title")}</h2>
        <p className="text-slate-300 mb-10 max-w-xl mx-auto">{t("subtitle")}</p>
        <div className="grid sm:grid-cols-2 gap-4 max-w-2xl mx-auto mb-10 text-left">
          {items.map((item) => (
            <div key={item.label} className="flex items-center gap-3 rounded-xl bg-white/5 px-4 py-3">
              {item.icon}
              <span className="text-sm text-slate-100">{item.label}</span>
            </div>
          ))}
        </div>
        <Button size="lg" asChild>
          <Link href={`/${locale}/dashboard/profile`}>{t("cta")}</Link>
        </Button>
      </div>
    </section>
  );
}
