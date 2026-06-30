import { useTranslations } from "next-intl";
import { getTranslations } from "next-intl/server";
import { Mail } from "lucide-react";
import type { Metadata } from "next";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("pages.contact");
  return { title: t("title") };
}

export default function ContactPage() {
  // eslint-disable-next-line react-hooks/rules-of-hooks
  const t = useTranslations("pages.contact");

  return (
    <div className="mx-auto max-w-3xl px-4 sm:px-6 lg:px-8 py-16">
      <h1 className="text-3xl font-bold text-slate-900 mb-6">{t("title")}</h1>
      <div className="space-y-4 text-slate-600 leading-relaxed">
        <p>{t("body1")}</p>
        <div className="flex items-center gap-2 text-slate-900 font-medium">
          <Mail size={18} className="text-brand-600" />
          <span>{t("emailLabel")}: nazarchuprii@gmail.com</span>
        </div>
        <p>{t("body2")}</p>
      </div>
    </div>
  );
}
