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

const registerSchema = z
  .object({
    firstName: z.string().min(2, "Min 2 characters / Min. 2 Zeichen"),
    lastName:  z.string().min(2, "Min 2 characters / Min. 2 Zeichen"),
    email:     z.string().email("Invalid email / Ungültige E-Mail"),
    password:  z.string().min(8, "Min 8 characters / Min. 8 Zeichen"),
    confirmPassword: z.string(),
  })
  .refine((d) => d.password === d.confirmPassword, {
    message: "Passwords do not match / Passwörter stimmen nicht überein",
    path: ["confirmPassword"],
  });

type RegisterForm = z.infer<typeof registerSchema>;

/**
 * Registration page.
 * Registrierungsseite.
 */
export default function RegisterPage() {
  const t = useTranslations("auth.register");
  const locale = useLocale();
  const { register: registerUser } = useAuth();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterForm>({ resolver: zodResolver(registerSchema) });

  async function onSubmit(data: RegisterForm) {
    try {
      await registerUser({
        firstName: data.firstName,
        lastName:  data.lastName,
        email:     data.email,
        password:  data.password,
      });
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Registration failed";
      toast.error(msg);
    }
  }

  return (
    <div className="min-h-[80vh] flex items-center justify-center px-4 py-16">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <div className="inline-flex items-center gap-2 text-brand-600 font-bold text-2xl mb-1">
            <Bike size={28} />
            RentMyBike
          </div>
          <h1 className="text-2xl font-bold text-slate-900">{t("title")}</h1>
          <p className="text-slate-500 text-sm mt-1">{t("subtitle")}</p>
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
              placeholder="Min. 8 characters"
              error={errors.password?.message}
              {...register("password")}
            />
            <Input
              label={t("confirmPassword")}
              type="password"
              autoComplete="new-password"
              placeholder="Repeat password"
              error={errors.confirmPassword?.message}
              {...register("confirmPassword")}
            />

            <p className="text-xs text-slate-500">{t("termsNote")}</p>

            <Button type="submit" className="w-full" loading={isSubmitting}>
              {t("submit")}
            </Button>
          </form>

          <p className="mt-6 text-center text-sm text-slate-500">
            {t("haveAccount")}{" "}
            <Link
              href={`/${locale}/auth/login`}
              className="font-medium text-brand-600 hover:underline"
            >
              {t("signIn")}
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
