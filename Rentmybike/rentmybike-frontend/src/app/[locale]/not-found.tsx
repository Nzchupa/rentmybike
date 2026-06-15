"use client";

import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import { Button } from "@/components/ui/Button";

export default function NotFound() {
  const t = useTranslations("errors");
  const locale = useLocale();

  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] text-center px-4">
      <p className="text-8xl font-bold text-brand-500 mb-4">404</p>
      <h1 className="text-2xl font-bold text-slate-900 mb-2">{t("404")}</h1>
      <p className="text-slate-500 mb-8">
        The page you are looking for does not exist.
      </p>
      <Button asChild>
        <Link href={`/${locale}/`}>{t("goHome")}</Link>
      </Button>
    </div>
  );
}
