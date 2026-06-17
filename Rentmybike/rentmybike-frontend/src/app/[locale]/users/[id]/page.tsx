"use client";

import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { ChevronLeft } from "lucide-react";
import { usersApi, reviewsApi } from "@/lib/api";
import { Avatar } from "@/components/ui/Avatar";
import { StarRating } from "@/components/ui/StarRating";
import { formatDate } from "@/lib/utils";

interface PublicProfilePageProps {
  // Next.js 14 (this project's version) passes params synchronously, not as
  // a Promise — see the matching comment in bikes/[id]/page.tsx.
  params: { id: string };
}

/**
 * Public user profile page — shown when clicking a bike owner's name/avatar
 * from the bike detail page. Mirrors the data already exposed by the public
 * GET /api/v1/users/{id}/public endpoint (rating, review count, join date).
 * Öffentliche Benutzerprofil-Seite — wird beim Klick auf Name/Avatar eines
 * Fahrrad-Eigentümers auf der Fahrrad-Detailseite angezeigt.
 */
export default function PublicProfilePage({ params }: PublicProfilePageProps) {
  const { id } = params;
  const t = useTranslations("users.profile");
  const locale = useLocale();

  const { data: profile, isLoading } = useQuery({
    queryKey: ["public-profile", id],
    queryFn: () => usersApi.getPublicProfile(id),
    select: (r) => r.data.data,
  });

  const { data: reviewsData } = useQuery({
    queryKey: ["user-reviews", id],
    queryFn: () => reviewsApi.getUserReviews(id, 0, 10),
    select: (r) => r.data.data,
    enabled: !!profile,
  });

  if (isLoading) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-10">
        <div className="animate-pulse space-y-4">
          <div className="h-20 w-20 rounded-full bg-slate-100" />
          <div className="h-6 bg-slate-100 rounded w-1/3" />
        </div>
      </div>
    );
  }

  if (!profile) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-10 text-center text-slate-500">
        <p>{t("notFound")}</p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl px-4 sm:px-6 py-10">
      <Link
        href={`/${locale}/bikes`}
        className="inline-flex items-center gap-1 text-sm text-slate-500 hover:text-slate-900 mb-6"
      >
        <ChevronLeft size={16} />
        {t("back")}
      </Link>

      <div className="card p-6 flex items-center gap-4">
        <Avatar name={profile.fullName} avatarUrl={profile.avatarUrl} size="xl" />
        <div>
          <h1 className="text-2xl font-bold text-slate-900">{profile.fullName}</h1>
          <p className="text-sm text-slate-500 mt-1">
            {t("memberSince", { date: formatDate(profile.createdAt, locale) })}
          </p>
          {profile.reviewCount > 0 && (
            <div className="flex items-center gap-2 mt-2">
              <StarRating rating={profile.averageRating} size="sm" />
              <span className="text-sm text-slate-500">
                {profile.averageRating.toFixed(1)} ({t("reviewsCount", { count: profile.reviewCount })})
              </span>
            </div>
          )}
        </div>
      </div>

      {reviewsData && reviewsData.totalElements > 0 && (
        <div className="mt-8">
          <h2 className="text-lg font-semibold text-slate-900 mb-4">{t("reviews")}</h2>
          <div className="space-y-4">
            {reviewsData.content.map((review) => (
              <div key={review.id} className="card p-4">
                <div className="flex items-start gap-3">
                  <Avatar name={review.reviewerName} avatarUrl={review.reviewerAvatarUrl} size="sm" />
                  <div className="flex-1">
                    <div className="flex items-center justify-between">
                      <span className="font-medium text-slate-900 text-sm">
                        {review.reviewerName}
                      </span>
                      <StarRating rating={review.rating} size="sm" />
                    </div>
                    {review.comment && (
                      <p className="text-sm text-slate-600 mt-1">{review.comment}</p>
                    )}
                    <p className="text-xs text-slate-400 mt-1">
                      {formatDate(review.createdAt, locale)}
                    </p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
