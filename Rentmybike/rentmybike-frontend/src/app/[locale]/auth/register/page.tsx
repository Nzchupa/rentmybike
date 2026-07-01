"use client";

import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import toast from "react-hot-toast";
import { VelohoodLogo } from "@/components/VelohoodLogo";
import { useAuth } from "@/hooks/useAuth";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";

// Validation messages are resolved at submit-time via the translation
// function passed into makeRegisterSchema(), so the error text follows the
// active locale instead of always showing both languages at once.
//
// Validierungsmeldungen werden zur Absendezeit über die in
// makeRegisterSchema() übergebene Übersetzungsfunktion aufgelöst, sodass der
// Fehlertext der aktiven Sprache folgt, anstatt immer beide Sprachen
// gleichzeitig zu zeigen.
function makeRegisterSchema(t: (key: string) => string) {
  return z
    .object({
      firstName: z.string().min(2, t("auth.errors.nameMin")),
      lastName:  z.string().min(2, t("auth.errors.nameMin")),
      email:     z.string().email(t("auth.errors.invalidEmail")),
      password:  z.string().min(8, t("auth.errors.weakPassword")),
      confirmPassword: z.string(),
      agreedToTerms: z.boolean().refine((v) => v === true, {
        message: t("auth.register.agreeToTerms.required"),
      }),
    })
    .refine((d) => d.password === d.confirmPassword, {
      message: t("auth.errors.passwordMismatch"),
      path: ["confirmPassword"],
    });
}

type RegisterForm = z.infer<ReturnType<typeof makeRegisterSchema>>;

/**
 * Registration page.
 * Registrierungsseite.
 */
export default function RegisterPage() {
  const t = useTranslations("auth.register");
  const tRoot = useTranslations();
  const locale = useLocale();
  const { register: registerUser } = useAuth();

  const {
    register,
    handleSubmit,
    control,
    formState: { errors, isSubmitting },
  } = useForm<RegisterForm>({
    resolver: zodResolver(makeRegisterSchema(tRoot)),
    defaultValues: { agreedToTerms: false },
  });

  async function onSubmit(data: RegisterForm) {
    try {
      await registerUser({
        firstName: data.firstName,
        lastName:  data.lastName,
        email:     data.email,
        password:  data.password,
      });
    } catch (err) {
      const msg = err instanceof Error ? err.message : tRoot("auth.errors.registrationFailed");
      toast.error(msg);
    }
  }

  return (
    <div className="min-h-[80vh] flex items-center justify-center px-4 py-16">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <div className="inline-flex items-center gap-2 text-brand-600 font-bold text-2xl mb-1">
            <VelohoodLogo size={32} />
            Velohood
          </div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">{t("title")}</h1>
          <p className="text-slate-500 dark:text-slate-400 text-sm mt-1">{t("subtitle")}</p>
        </div>

        <div className="card p-8">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <Input
                label={t("firstName")}
                autoComplete="given-name"
                error={errors.firstName?.message}
                {...register("firstName")}
              />
              <Input
                label={t("lastName")}
                autoComplete="family-name"
                error={errors.lastName?.message}
                {...register("lastName")}
              />
            </div>

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
              autoComplete="new-password"
              placeholder={t("passwordHint")}
              error={errors.password?.message}
              {...register("password")}
            />
            <Input
              label={t("confirmPassword")}
              type="password"
              autoComplete="new-password"
              placeholder={t("confirmPasswordPlaceholder")}
              error={errors.confirmPassword?.message}
              {...register("confirmPassword")}
            />

            <div className="space-y-1">
              <Controller
                name="agreedToTerms"
                control={control}
                render={({ field }) => (
                  <label className="flex items-start gap-2 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={field.value}
                      onChange={field.onChange}
                      className="mt-0.5 h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500 cursor-pointer"
                    />
                    <span className="text-xs text-slate-600 dark:text-slate-400">
                      {t("agreeToTerms.before")}
                      <Link
                        href={`/${locale}/terms`}
                        className="font-medium text-brand-600 dark:text-brand-400 hover:underline"
                        target="_blank"
                      >
                        {t("agreeToTerms.terms")}
                      </Link>
                      {t("agreeToTerms.middle")}
                      <Link
                        href={`/${locale}/privacy`}
                        className="font-medium text-brand-600 dark:text-brand-400 hover:underline"
                        target="_blank"
                      >
                        {t("agreeToTerms.privacy")}
                      </Link>
                      {t("agreeToTerms.after")}
                    </span>
                  </label>
                )}
              />
              {errors.agreedToTerms && (
                <p className="text-xs text-red-500">{errors.agreedToTerms.message}</p>
              )}
            </div>

            <Button type="submit" className="w-full" loading={isSubmitting}>
              {t("submit")}
            </Button>
          </form>

          <p className="mt-6 text-center text-sm text-slate-500 dark:text-slate-400">
            {t("haveAccount")}{" "}
            <Link
              href={`/${locale}/auth/login`}
              className="font-medium text-brand-600 dark:text-brand-400 hover:underline"
            >
              {t("signIn")}
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
