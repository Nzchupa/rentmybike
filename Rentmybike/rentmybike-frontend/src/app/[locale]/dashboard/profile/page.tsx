"use client";

import { useEffect, useRef } from "react";
import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import toast from "react-hot-toast";
import { Camera } from "lucide-react";
import Link from "next/link";
import { useLocale } from "next-intl";
import { usersApi, businessApi } from "@/lib/api";
import { useAuthStore } from "@/store/auth.store";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Avatar } from "@/components/ui/Avatar";
import { VerificationBadge } from "@/components/ui/VerificationBadge";
import { BusinessVerificationCard } from "@/components/dashboard/BusinessVerificationCard";
import { formatDate } from "@/lib/utils";
import type { UpdateProfileRequest, ChangePasswordRequest, UpgradeToBusinessRequest } from "@/types";

function makeBusinessSchema(t: (key: string) => string) {
  return z.object({
    businessName: z.string().min(2, t("dashboard.profile.validation.minTwoChars")).max(150),
  });
}

// Validation messages are resolved at submit-time via the translation
// function passed into makeProfileSchema()/makePasswordSchema(), so the
// error text follows the active locale instead of always showing English.
//
// Validierungsmeldungen werden zur Absendezeit über die in
// makeProfileSchema()/makePasswordSchema() übergebene Übersetzungsfunktion
// aufgelöst, sodass der Fehlertext der aktiven Sprache folgt, anstatt
// immer Englisch zu zeigen.
function makeProfileSchema(t: (key: string) => string) {
  return z.object({
    firstName: z.string().min(2, t("dashboard.profile.validation.minTwoChars")),
    lastName:  z.string().min(2, t("dashboard.profile.validation.minTwoChars")),
    phone:     z.string().max(30).optional(),
  });
}

function makePasswordSchema(t: (key: string) => string) {
  return z
    .object({
      currentPassword:    z.string().min(1, t("dashboard.profile.validation.required")),
      newPassword:        z.string().min(8, t("auth.errors.weakPassword")),
      confirmNewPassword: z.string(),
    })
    .refine((d) => d.newPassword === d.confirmNewPassword, {
      message: t("auth.errors.passwordMismatch"),
      path: ["confirmNewPassword"],
    });
}

/**
 * Profile edit page.
 * Profil-Bearbeitungsseite.
 */
export default function ProfilePage() {
  const t = useTranslations("dashboard.profile");
  const tRoot = useTranslations();
  const locale = useLocale();
  const { user, setUser } = useAuthStore();
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Business upgrade form
  const {
    register: regBiz,
    handleSubmit: handleBiz,
    formState: { errors: bizErrors, isSubmitting: upgradingBiz },
  } = useForm({
    resolver: zodResolver(makeBusinessSchema(tRoot)),
    defaultValues: { businessName: "" },
  });

  const { mutateAsync: upgradeToBusiness } = useMutation({
    mutationFn: (data: UpgradeToBusinessRequest) => businessApi.upgrade(data),
    onSuccess: (res) => {
      setUser(res.data.data);
      toast.success(t("business.upgraded"));
    },
    onError: (e: Error) => toast.error(e.message),
  });

  // Profile form
  const {
    register: regProfile,
    handleSubmit: handleProfile,
    reset: resetProfile,
    formState: { errors: profileErrors, isSubmitting: savingProfile },
  } = useForm({
    resolver: zodResolver(makeProfileSchema(tRoot)),
    defaultValues: {
      firstName: user?.firstName ?? "",
      lastName:  user?.lastName ?? "",
      phone:     user?.phone ?? "",
    },
  });

  // `user` is null on first render (it's still loading from /users/me when
  // this page mounts) and useForm's defaultValues are only read once, at
  // mount time — so without this, the form would stay stuck on empty
  // strings even after the real profile data arrives a moment later.
  //
  // `user` ist beim ersten Rendern null (wird noch von /users/me geladen,
  // wenn diese Seite mountet), und die defaultValues von useForm werden nur
  // einmal, beim Mounten, gelesen — ohne dies würde das Formular bei leeren
  // Strings bleiben, selbst nachdem die echten Profildaten kurz danach
  // eintreffen.
  useEffect(() => {
    if (user) {
      resetProfile({
        firstName: user.firstName ?? "",
        lastName: user.lastName ?? "",
        phone: user.phone ?? "",
      });
    }
  }, [user, resetProfile]);

  // Password form
  const {
    register: regPwd,
    handleSubmit: handlePwd,
    reset: resetPwd,
    formState: { errors: pwdErrors, isSubmitting: savingPwd },
  } = useForm({ resolver: zodResolver(makePasswordSchema(tRoot)) });

  const { mutateAsync: updateProfile } = useMutation({
    mutationFn: (data: UpdateProfileRequest) => usersApi.updateProfile(data),
    onSuccess: (res) => {
      setUser(res.data.data);
      toast.success(t("profileSaved"));
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutateAsync: changePassword } = useMutation({
    mutationFn: (data: ChangePasswordRequest) => usersApi.changePassword(data),
    onSuccess: () => {
      toast.success(t("passwordChanged"));
      resetPwd();
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutate: uploadAvatar, isPending: uploadingAvatar } = useMutation({
    mutationFn: (file: File) => usersApi.uploadAvatar(file),
    onSuccess: (res) => {
      setUser(res.data.data);
      toast.success(t("avatarUpdated"));
    },
    onError: (e: Error) => toast.error(e.message),
  });

  if (!user) return null;

  return (
    <div className="max-w-lg space-y-8">
      <h1 className="section-title">{t("title")}</h1>

      {/* Avatar */}
      <div className="flex items-center gap-4">
        <div className="relative">
          <Avatar name={user.fullName} avatarUrl={user.avatarUrl} size="xl" />
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            className="absolute -bottom-1 -right-1 w-8 h-8 bg-brand-500 text-white rounded-full flex items-center justify-center shadow hover:bg-brand-600 transition"
          >
            {uploadingAvatar ? (
              <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
            ) : (
              <Camera size={14} />
            )}
          </button>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
            className="hidden"
            onChange={(e) => {
              const file = e.target.files?.[0];
              if (file) uploadAvatar(file);
            }}
          />
        </div>
        <div>
          <p className="font-semibold text-slate-900 dark:text-slate-100">{user.fullName}</p>
          <p className="text-sm text-slate-500 dark:text-slate-400">{user.email}</p>
          <div className="flex flex-wrap items-center gap-2 mt-2">
            <VerificationBadge
              verified={user.emailVerified}
              verifiedLabel={t("emailVerifiedBadge")}
              unverifiedLabel={t("emailUnverifiedBadge")}
            />
            <span className="text-xs text-slate-400 dark:text-slate-500">
              {t("memberSince", { date: formatDate(user.createdAt, locale) })}
            </span>
          </div>
        </div>
      </div>

      {/* Profile form */}
      <div className="card p-6 space-y-5">
        <h2 className="font-semibold text-slate-900 dark:text-slate-100">{t("fullName")}</h2>
        <form
          onSubmit={handleProfile(async (data) => {
            await updateProfile({
              firstName: data.firstName,
              lastName:  data.lastName,
              phone:     data.phone || undefined,
            });
          })}
          className="space-y-4"
        >
          <div className="grid grid-cols-2 gap-3">
            <Input
              label={t("firstName")}
              error={profileErrors.firstName?.message}
              {...regProfile("firstName")}
            />
            <Input
              label={t("lastName")}
              error={profileErrors.lastName?.message}
              {...regProfile("lastName")}
            />
          </div>
          <Input
            label={t("phone")}
            type="tel"
            placeholder="+49 123 456789"
            error={profileErrors.phone?.message}
            {...regProfile("phone")}
          />
          <Button type="submit" loading={savingProfile}>
            {t("saveChanges")}
          </Button>
        </form>
      </div>

      {/* Password form */}
      <div className="card p-6 space-y-5">
        <h2 className="font-semibold text-slate-900 dark:text-slate-100">{t("changePassword")}</h2>
        <form
          onSubmit={handlePwd(async (data) => {
            await changePassword(data as ChangePasswordRequest);
          })}
          className="space-y-4"
        >
          <Input
            label={t("currentPassword")}
            type="password"
            error={pwdErrors.currentPassword?.message as string}
            {...regPwd("currentPassword")}
          />
          <Input
            label={t("newPassword")}
            type="password"
            placeholder={t("newPasswordHint")}
            error={pwdErrors.newPassword?.message as string}
            {...regPwd("newPassword")}
          />
          <Input
            label={t("confirmNewPassword")}
            type="password"
            error={pwdErrors.confirmNewPassword?.message as string}
            {...regPwd("confirmNewPassword")}
          />
          <Button type="submit" loading={savingPwd} variant="secondary">
            {t("changePassword")}
          </Button>
        </form>
      </div>

      {/* Business account */}
      <div className="card p-6 space-y-5">
        <h2 className="font-semibold text-slate-900 dark:text-slate-100">{t("business.title")}</h2>

        {user.role === "BUSINESS" ? (
          <BusinessVerificationCard
            verified={user.businessVerified}
            locale={locale}
            verifiedLabel={t("business.currentBadgeVerified")}
            pendingLabel={t("business.currentBadgePending")}
            pendingNotice={t("business.pendingNotice")}
            goToDashboardLabel={t("business.goToDashboard")}
          />
        ) : (
          <div className="space-y-4">
            <p className="text-sm text-slate-500 dark:text-slate-400">{t("business.pitch")}</p>
            <form
              onSubmit={handleBiz(async (data) => {
                await upgradeToBusiness(data as UpgradeToBusinessRequest);
              })}
              className="space-y-4"
            >
              <Input
                label={t("business.businessName")}
                placeholder={t("business.businessNamePlaceholder")}
                error={bizErrors.businessName?.message}
                {...regBiz("businessName")}
              />
              <Button type="submit" loading={upgradingBiz}>
                {t("business.upgradeButton")}
              </Button>
            </form>
          </div>
        )}
      </div>
    </div>
  );
}
