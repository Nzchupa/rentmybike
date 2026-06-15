"use client";

import { Suspense, useEffect, useState } from "react";
import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import { useSearchParams } from "next/navigation";
import { CheckCircle, XCircle, Mail } from "lucide-react";
import { authApi } from "@/lib/api";
import { Button } from "@/components/ui/Button";

type State = "pending" | "verifying" | "success" | "error";

/**
 * Email verification page.
 * Handles two cases:
 *  - ?email=  → pending state (just registered, show "check inbox")
 *  - ?token=  → actively verify the token via API
 *
 * E-Mail-Bestätigungsseite.
 */
function VerifyEmailContent() {
  const t = useTranslations("auth.verify");
  const locale = useLocale();
  const params = useSearchParams();
  const token = params.get("token");
  const email = params.get("email");

  const [state, setState] = useState<State>(token ? "verifying" : "pending");

  useEffect(() => {
    if (!token) return;
    authApi
      .verifyEmail(token)
      .then(() => setState("success"))
      .catch(() => setState("error"));
  }, [token]);

  if (state === "pending") {
    return (
      <div className="text-center">
        <div className="mx-auto mb-4 w-16 h-16 rounded-full bg-blue-100 flex items-center justify-center">
          <Mail size={32} className="text-blue-600" />
        </div>
        <h1 className="text-2xl font-bold text-slate-900 mb-2">{t("pendingTitle")}</h1>
        <p className="text-slate-600 max-w-sm mb-6">
          {t("pendingMessage", { email: email ?? "your email" })}
        </p>
        <Button asChild>
          <Link href={`/${locale}/auth/login`}>{t("goToLogin")}</Link>
        </Button>
      </div>
    );
  }

  if (state === "verifying") {
    return (
      <div className="text-center">
        <div className="w-8 h-8 border-4 border-brand-500 border-t-transparent rounded-full animate-spin mx-auto mb-4" />
        <p className="text-slate-600">Verifying... / Wird verifiziert...</p>
      </div>
    );
  }

  if (state === "success") {
    return (
      <div className="text-center">
        <div className="mx-auto mb-4 w-16 h-16 rounded-full bg-green-100 flex items-center justify-center">
          <CheckCircle size={32} className="text-green-600" />
        </div>
        <h1 className="text-2xl font-bold text-slate-900 mb-2">{t("successTitle")}</h1>
        <p className="text-slate-600 mb-6">{t("successMessage")}</p>
        <Button asChild>
          <Link href={`/${locale}/auth/login`}>{t("goToLogin")}</Link>
        </Button>
      </div>
    );
  }

  return (
    <div className="text-center">
      <div className="mx-auto mb-4 w-16 h-16 rounded-full bg-red-100 flex items-center justify-center">
        <XCircle size={32} className="text-red-500" />
      </div>
      <h1 className="text-2xl font-bold text-slate-900 mb-2">{t("errorTitle")}</h1>
      <p className="text-slate-600 mb-6">{t("errorMessage")}</p>
      <Button variant="secondary" asChild>
        <Link href={`/${locale}/auth/register`}>Register again / Erneut registrieren</Link>
      </Button>
    </div>
  );
}

export default function VerifyEmailPage() {
  const t = useTranslations("auth.verify");

  return (
    <div className="min-h-[80vh] flex items-center justify-center px-4 py-16">
      <div className="w-full max-w-md">
        <div className="card p-12">
          <Suspense
            fallback={
              <div className="text-center text-slate-500">{t("title")}...</div>
            }
          >
            <VerifyEmailContent />
          </Suspense>
        </div>
      </div>
    </div>
  );
}
