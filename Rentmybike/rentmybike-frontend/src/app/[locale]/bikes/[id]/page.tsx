"use client";

import Image from "next/image";
import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { MapPin, ChevronLeft, Star } from "lucide-react";
import { bikesApi, reviewsApi } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { Avatar } from "@/components/ui/Avatar";
import { StarRating } from "@/components/ui/StarRating";
import { ApprovalStatusBadge } from "@/components/ui/Badge";
import { formatPrice, formatDate } from "@/lib/utils";
import { useAuthStore } from "@/store/auth.store";
import { useState } from "react";
import type { BikePhotoResponse } from "@/types";
import { BookingForm } from "@/components/booking/BookingForm";
import { FavoriteButton } from "@/components/bikes/FavoriteButton";

interface BikeDetailPageProps {
  // Next.js 14 passes route params as a plain (already-resolved) object —
  // the Promise<...> + use() pattern is a Next.js 15 App Router feature.
  // This project is on next@14.2.29 (see package.json), so wrapping params
  // in a Promise and unwrapping it with React's use() throws minified React
  // error #438 ("An unsupported type was passed to use()"), since a plain
  // object isn't a thenable.
  // Next.js 14 übergibt Routen-Parameter als einfaches (bereits aufgelöstes)
  // Objekt — das Promise<...> + use()-Muster ist ein Next.js 15 App
  // Router-Feature. Dieses Projekt läuft auf next@14.2.29 (siehe
  // package.json), daher wirft das Einpacken von params in ein Promise und
  // das Entpacken mit Reacts use() den minimierten React-Fehler #438 ("An
  // unsupported type was passed to use()"), da ein einfaches Objekt kein
  // Thenable ist.
  params: { id: string };
}

/**
 * Public bike detail page — photo gallery, info, booking panel, reviews.
 * Öffentliche Fahrrad-Detailseite — Fotogalerie, Info, Buchungspanel, Bewertungen.
 */
export default function BikeDetailPage({ params }: BikeDetailPageProps) {
  const { id } = params;
  const t = useTranslations("bikes.detail");
  const locale = useLocale();
  const { user } = useAuthStore();
  const [selectedPhoto, setSelectedPhoto] = useState(0);

  const { data: bikeData, isLoading } = useQuery({
    queryKey: ["bike", id],
    queryFn: () => bikesApi.getById(id),
    select: (r) => r.data.data,
  });

  const { data: ratingData } = useQuery({
    queryKey: ["bike-rating", id],
    queryFn: () => reviewsApi.getBikeRating(id),
    select: (r) => r.data.data,
  });

  const { data: reviewsData } = useQuery({
    queryKey: ["bike-reviews", id],
    queryFn: () => reviewsApi.getBikeReviews(id, 0, 5),
    select: (r) => r.data.data,
  });

  if (isLoading) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-10">
        <div className="animate-pulse space-y-4">
          <div className="h-72 bg-slate-100 rounded-2xl" />
          <div className="h-8 bg-slate-100 rounded w-1/2" />
          <div className="h-4 bg-slate-100 rounded w-1/4" />
        </div>
      </div>
    );
  }

  if (!bikeData) return null;

  const bike = bikeData;
  const photos: BikePhotoResponse[] = bike.photos ?? [];
  const displayPhoto = photos[selectedPhoto]?.url ?? bike.primaryPhotoUrl;
  const isOwner = user?.id === bike.ownerId;

  return (
    <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-10">
      {/* Back */}
      <Link
        href={`/${locale}/bikes`}
        className="inline-flex items-center gap-1 text-sm text-slate-500 hover:text-slate-900 mb-6"
      >
        <ChevronLeft size={16} />
        {t("backToSearch")}
      </Link>

      <div className="grid lg:grid-cols-3 gap-8">
        {/* Left: photos + info */}
        <div className="lg:col-span-2 space-y-6">
          {/* Main photo */}
          <div className="relative h-72 md:h-96 rounded-2xl overflow-hidden bg-slate-100">
            {displayPhoto ? (
              <Image
                src={displayPhoto}
                alt={bike.title}
                fill
                className="object-cover"
                sizes="(max-width: 1024px) 100vw, 66vw"
                priority
              />
            ) : (
              <div className="flex h-full items-center justify-center text-slate-300">
                <Star size={64} />
              </div>
            )}
            <div className="absolute top-3 right-3">
              <ApprovalStatusBadge status={bike.approvalStatus} />
            </div>
          </div>

          {/* Thumbnail strip */}
          {photos.length > 1 && (
            <div className="flex gap-2 overflow-x-auto pb-2">
              {photos.map((photo, i) => (
                <button
                  key={photo.id}
                  onClick={() => setSelectedPhoto(i)}
                  className={`relative shrink-0 w-20 h-16 rounded-xl overflow-hidden border-2 transition ${
                    i === selectedPhoto ? "border-brand-500" : "border-transparent"
                  }`}
                >
                  <Image src={photo.url} alt="" fill className="object-cover" sizes="80px" />
                </button>
              ))}
            </div>
          )}

          {/* Title + meta */}
          <div>
            <div className="flex items-start justify-between gap-4">
              <h1 className="text-3xl font-bold text-slate-900">{bike.title}</h1>
              <div className="text-right shrink-0">
                <span className="text-2xl font-bold text-brand-600">
                  {formatPrice(bike.pricePerDay, locale)}
                </span>
                <div className="text-sm text-slate-500">{t("pricePerDay")}</div>
              </div>
            </div>

            <div className="flex items-center justify-between mt-2">
              <div className="flex items-center gap-2 text-slate-500">
                <MapPin size={16} />
                <span>{bike.city}{bike.address ? `, ${bike.address}` : ""}</span>
              </div>
              {!isOwner && <FavoriteButton bikeId={bike.id} variant="full" />}
            </div>

            {ratingData && ratingData.reviewCount > 0 && (
              <div className="flex items-center gap-2 mt-2">
                <StarRating rating={ratingData.averageRating} size="sm" />
                <span className="text-sm text-slate-500">
                  {ratingData.averageRating.toFixed(1)} ({t("reviewsCount", { count: ratingData.reviewCount })})
                </span>
              </div>
            )}
          </div>

          {/* Description */}
          <div>
            <h2 className="text-lg font-semibold text-slate-900 mb-2">{t("description")}</h2>
            <p className="text-slate-600 whitespace-pre-line">{bike.description}</p>
          </div>

          {/* Owner */}
          <div className="card p-4">
            <p className="text-sm font-medium text-slate-500 mb-3">{t("owner")}</p>
            <Link
              href={`/${locale}/users/${bike.ownerId}`}
              className="flex items-center gap-3 hover:opacity-80"
            >
              <Avatar name={bike.ownerName} avatarUrl={bike.ownerAvatarUrl} size="md" />
              <span className="font-medium text-slate-900">{bike.ownerName}</span>
            </Link>
          </div>

          {/* Reviews */}
          {reviewsData && reviewsData.totalElements > 0 && (
            <div>
              <h2 className="text-lg font-semibold text-slate-900 mb-4">{t("reviews")}</h2>
              <div className="space-y-4">
                {reviewsData.content.map((review) => (
                  <div key={review.id} className="card p-4">
                    <div className="flex items-start gap-3">
                      <Avatar
                        name={review.reviewerName}
                        avatarUrl={review.reviewerAvatarUrl}
                        size="sm"
                      />
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

        {/* Right: Booking panel */}
        <div className="lg:col-span-1">
          <div className="sticky top-24">
            {bike.approvalStatus === "APPROVED" && bike.available && !isOwner ? (
              <BookingForm bike={bike} />
            ) : isOwner ? (
              <div className="card p-6 text-center text-slate-500">
                <p className="mb-4">{t("ownerNotice")}</p>
                <Button variant="outline" asChild>
                  <Link href={`/${locale}/dashboard/bikes`}>
                    {t("editInDashboard")}
                  </Link>
                </Button>
              </div>
            ) : (
              <div className="card p-6 text-center text-slate-500">
                <p>
                  {!bike.available ? t("unavailable") : t("pendingApproval")}
                </p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
