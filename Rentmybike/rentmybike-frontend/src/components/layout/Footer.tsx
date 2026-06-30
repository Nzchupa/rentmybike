import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import { Bike, Mail } from "lucide-react";

export function Footer() {
  const locale = useLocale();
  const t = useTranslations("footer");
  const year = new Date().getFullYear();

  return (
    <footer className="border-t border-slate-200 bg-slate-50 mt-auto">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
          <div className="col-span-2 md:col-span-1">
            <Link
              href={`/${locale}/`}
              className="flex items-center gap-2 font-bold text-brand-600 mb-2"
            >
              <Bike size={20} />
              RentMyBike
            </Link>
            <p className="text-sm text-slate-500">{t("tagline")}</p>
            <a
              href="mailto:nazarchuprii@gmail.com"
              className="inline-flex items-center gap-1.5 text-sm text-slate-500 hover:text-slate-900 transition-colors mt-2"
            >
              <Mail size={14} />
              nazarchuprii@gmail.com
            </a>
          </div>

          <div>
            <h3 className="text-xs font-semibold uppercase tracking-wide text-slate-400 mb-3">
              {t("quickLinks")}
            </h3>
            <ul className="space-y-2 text-sm text-slate-600">
              <li>
                <Link href={`/${locale}/`} className="hover:text-slate-900 transition-colors">
                  {t("home")}
                </Link>
              </li>
              <li>
                <Link href={`/${locale}/bikes`} className="hover:text-slate-900 transition-colors">
                  {t("bikes")}
                </Link>
              </li>
              <li>
                <Link href={`/${locale}/how-it-works`} className="hover:text-slate-900 transition-colors">
                  {t("howItWorks")}
                </Link>
              </li>
              <li>
                <Link href={`/${locale}/faq`} className="hover:text-slate-900 transition-colors">
                  {t("faq")}
                </Link>
              </li>
            </ul>
          </div>

          <div>
            <h3 className="text-xs font-semibold uppercase tracking-wide text-slate-400 mb-3">
              {t("company")}
            </h3>
            <ul className="space-y-2 text-sm text-slate-600">
              <li>
                <Link href={`/${locale}/about`} className="hover:text-slate-900 transition-colors">
                  {t("about")}
                </Link>
              </li>
              <li>
                <Link href={`/${locale}/contact`} className="hover:text-slate-900 transition-colors">
                  {t("contact")}
                </Link>
              </li>
            </ul>
          </div>

          <div>
            <h3 className="text-xs font-semibold uppercase tracking-wide text-slate-400 mb-3">
              {t("legal")}
            </h3>
            <ul className="space-y-2 text-sm text-slate-600">
              <li>
                <Link href={`/${locale}/privacy`} className="hover:text-slate-900 transition-colors">
                  {t("privacy")}
                </Link>
              </li>
              <li>
                <Link href={`/${locale}/terms`} className="hover:text-slate-900 transition-colors">
                  {t("terms")}
                </Link>
              </li>
              <li>
                <Link href={`/${locale}/trust`} className="hover:text-slate-900 transition-colors">
                  {t("trust")}
                </Link>
              </li>
            </ul>
          </div>
        </div>

        <div className="mt-10 pt-6 border-t border-slate-200 text-sm text-slate-500 flex flex-col sm:flex-row items-center justify-between gap-2">
          <p>
            © {year} RentMyBike. {t("rights")}
          </p>
          <p>{t("studentProject")}</p>
        </div>
      </div>
    </footer>
  );
}
