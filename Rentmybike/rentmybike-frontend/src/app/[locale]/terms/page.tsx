import { useTranslations } from "next-intl";
import { getTranslations } from "next-intl/server";
import type { Metadata } from "next";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("pages.terms");
  return { title: t("title") };
}

export default function TermsPage() {
  // eslint-disable-next-line react-hooks/rules-of-hooks
  const t = useTranslations("pages.terms");

  const sections = [1, 2, 3, 4, 5, 6] as const;

  return (
    <div className="mx-auto max-w-3xl px-4 sm:px-6 lg:px-8 py-16">
      <h1 className="text-3xl font-bold text-slate-900 mb-4">{t("title")}</h1>
      <p className="text-slate-500 mb-8">{t("intro")}</p>
      <div className="space-y-8">
        {sections.map((n) => (
          <div key={n}>
            <h2 className="text-lg font-semibold text-slate-900 mb-2">
              {t(`section${n}Title`)}
            </h2>
            <p className="text-slate-600 leading-relaxed">{t(`section${n}Body`)}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
