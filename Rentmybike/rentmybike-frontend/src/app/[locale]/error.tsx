"use client";

import { useEffect } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/Button";

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  const t = useTranslations("errors");

  useEffect(() => {
    console.error("[Page Error]:", error);
  }, [error]);

  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] text-center px-4">
      <p className="text-6xl font-bold text-red-400 mb-4">500</p>
      <h1 className="text-2xl font-bold text-slate-900 mb-2">{t("500")}</h1>
      <p className="text-slate-500 mb-8">{error.message}</p>
      <Button onClick={reset} variant="secondary">
        Try again / Erneut versuchen
      </Button>
    </div>
  );
}
