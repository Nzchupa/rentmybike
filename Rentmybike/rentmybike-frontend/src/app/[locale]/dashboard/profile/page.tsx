"use client";

import { useRef } from "react";
import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import toast from "react-hot-toast";
import { Camera } from "lucide-react";
import { usersApi } from "@/lib/api";
import { useAuthStore } from "@/store/auth.store";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Avatar } from "@/components/ui/Avatar";
import type { UpdateProfileRequest, ChangePasswordRequest } from "@/types";

const profileSchema = z.object({
  firstName: z.string().min(2, "Min 2 chars"),
  lastName:  z.string().min(2, "Min 2 chars"),
  phone:     z.string().max(30).optional(),
});

const passwordSchema = z
  .object({
    currentPassword:    z.string().min(1, "Required"),
    newPassword:        z.string().min(8, "Min 8 chars"),
    confirmNewPassword: z.string(),
  })
  .refine((d) => d.newPassword === d.confirmNewPassword, {
    message: "Passwords do not match",
    path: ["confirmNewPassword"],
  });

/**
 * Profile edit page.
 * Profil-Bearbeitungsseite.
 */
export default function ProfilePage() {
  const t = useTranslations("dashboard.profile");
  const { user, setUser } = useAuthStore();
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Profile form
  const {
    register: regProfile,
    handleSubmit: handleProfile,
    formState: { errors: profileErrors, isSubmitting: savingProfile },
  } = useForm({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      firstName: user?.firstName ?? "",
      lastName:  user?.lastName ?? "",
      phone:     user?.phone ?? "",
    },
  });

  // Password form
  const {
    register: regPwd,
    handleSubmit: handlePwd,
    reset: resetPwd,
    formState: { errors: pwdErrors, isSubmitting: savingPwd },
  } = useForm({ resolver: zodResolver(passwordSchema) });

  const { mutateAsync: updateProfile } = useMutation({
    mutationFn: (data: UpdateProfileRequest) => usersApi.updateProfile(data),
    onSuccess: (res) => {
      setUser(res.data.data);
      toast.success("Profile saved / Profil gespeichert");
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutateAsync: changePassword } = useMutation({
    mutationFn: (data: ChangePasswordRequest) => usersApi.changePassword(data),
    onSuccess: () => {
      toast.success("Password changed / Passwort geändert");
      resetPwd();
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const { mutate: uploadAvatar, isPending: uploadingAvatar } = useMutation({
    mutationFn: (file: File) => usersApi.uploadAvatar(file),
    onSuccess: (res) => {
      setUser(res.data.data);
      toast.success("Avatar updated / Avatar aktualisiert");
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
          <p className="font-semibold text-slate-900">{user.fullName}</p>
          <p className="text-sm text-slate-500">{user.email}</p>
        </div>
      </div>

      {/* Profile form */}
      <div className="card p-6 space-y-5">
        <h2 className="font-semibold text-slate-900">{t("fullName")}</h2>
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
              label="First name / Vorname"
              error={profileErrors.firstName?.message}
              {...regProfile("firstName")}
            />
            <Input
              label="Last name / Nachname"
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
        <h2 className="font-semibold text-slate-900">{t("changePassword")}</h2>
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
            placeholder="Min. 8 characters"
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
    </div>
  );
}
