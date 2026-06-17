"use client";

import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import toast from "react-hot-toast";
import { Bike } from "lucide-react";
import { useAuth } from "@/hooks/useAuth";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import type { LoginRequest } from "@/types";

// Validation messages are resolved at submit-time via the translation
// function passed into makeLoginSchema(), so the error text follows the
// active locale instead of always showing both languages at once.
//
// Validierungsmeldungen werden zur Absendezeit über die in
// makeLoginSchema() übergebene Übersetzungsfunktion aufgelöst, sodass der
// Fehlertext der aktiven Sprache folgt, anstatt immer beide Sprachen
// gleichzeitig zu zeigen.
function makeLoginSchema(t: (key: string) => string) {
  return z.object({
    email: z.string().email(t("auth.errors.invalidEmail")),
    password: z.string().min(1, t("auth.errors.passwordRequired")),
  });
}

type LoginForm = z.infer<ReturnType<typeof makeLoginSchema>>;

/**
 * Login page.
 * Anmeldeseite.
 */
export default function LoginPage() {
  const t = useTranslations("auth.login");
  const tRoot = useTranslations();
  const locale = useLocale();
  const { login } = useAuth();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginForm>({ resolver: zodResolver(makeLoginSchema(tRoot)) });

  async function onSubmit(data: LoginForm) {
    try {
      await login(data as LoginRequest);
    } catch (err) {
      const msg = err instanceof Error ? err.message : tRoot("auth.errors.loginFailed");
      toast.error(msg);
    }
  }

  return (
    <div className="min-h-[80vh] flex items-center justify-center px-4 py-16">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center gap-2 text-brand-600 font-bold text-2xl mb-1">
            <Bike size={28} />
            RentMyBike
          </div>
          <h1 className="text-2xl font-bold text-slate-900">{t("title")}</h1>
          <p className="text-slate-500 text-sm mt-1">{t("subtitle")}</p>
        </div>

        <div className="card p-8">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
            <Input
              label={t("email")}
              type="email"
              autoComplete="email"
              placeholder="you@example.com"
              error={errors.email?.message}
              {...register("email")}
            />
            <Input
              label={t("password")}
              type="password"
              autoComplete="current-password"
              placeholder="••••••••"
              error={errors.password?.message}
              {...register("password")}
            />

            <Button type="submit" className="w-full" loading={isSubmitting}>
              {t("submit")}
            </Button>
          </form>

          <p className="mt-6 text-center text-sm text-slate-500">
            {t("noAccount")}{" "}
            <Link
              href={`/${locale}/auth/register`}
              className="font-medium text-brand-600 hover:underline"
            >
              {t("signUp")}
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
