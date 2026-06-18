"use client";

import { useState } from "react";
import { Heart } from "lucide-react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { favoritesApi } from "@/lib/api";
import { useAuthStore } from "@/store/auth.store";
import { cn } from "@/lib/utils";

interface FavoriteButtonProps {
  bikeId: string;
  /** "icon" = small heart-only overlay for cards, "full" = heart + count for the detail page */
  variant?: "icon" | "full";
  className?: string;
}

/**
 * Heart toggle for favoriting a bike (Stage 2 "Beta launch" trust feature).
 * Herz-Umschalter zum Favorisieren eines Fahrrads (Stage-2-"Beta-Start"-Vertrauensfeature).
 *
 * <p>Works for anonymous visitors too — the status query always succeeds
 * (favorited=false for them), but clicking the heart while logged out
 * redirects to login rather than firing a doomed 401 mutation.
 * <p>Funktioniert auch für anonyme Besucher — die Statusabfrage gelingt immer
 * (favorited=false für sie), aber ein Klick auf das Herz im abgemeldeten
 * Zustand leitet zum Login weiter, statt eine zum Scheitern verurteilte
 * 401-Mutation auszulösen.
 */
export function FavoriteButton({ bikeId, variant = "icon", className }: FavoriteButtonProps) {
  const queryClient = useQueryClient();
  const { user } = useAuthStore();
  const [pendingClick, setPendingClick] = useState(false);

  const { data: status } = useQuery({
    queryKey: ["favorite-status", bikeId],
    queryFn: () => favoritesApi.getStatus(bikeId),
    select: (r) => r.data.data,
    staleTime: 30_000,
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["favorite-status", bikeId] });
    queryClient.invalidateQueries({ queryKey: ["my-favorites"] });
  };

  const { mutate: addFavorite } = useMutation({
    mutationFn: () => favoritesApi.add(bikeId),
    onSuccess: invalidate,
    onSettled: () => setPendingClick(false),
  });

  const { mutate: removeFavorite } = useMutation({
    mutationFn: () => favoritesApi.remove(bikeId),
    onSuccess: invalidate,
    onSettled: () => setPendingClick(false),
  });

  const favorited = status?.favorited ?? false;
  const count = status?.favoriteCount ?? 0;

  function handleClick(e: React.MouseEvent) {
    e.preventDefault();
    e.stopPropagation();

    if (!user) {
      window.location.href = "/login";
      return;
    }

    setPendingClick(true);
    if (favorited) {
      removeFavorite();
    } else {
      addFavorite();
    }
  }

  if (variant === "full") {
    return (
      <button
        type="button"
        onClick={handleClick}
        disabled={pendingClick}
        className={cn(
          "inline-flex items-center gap-2 rounded-xl border border-slate-200 px-4 py-2.5 text-sm font-medium transition hover:bg-slate-50 disabled:opacity-60",
          favorited && "border-red-200 bg-red-50 text-red-600 hover:bg-red-50",
          className
        )}
      >
        <Heart size={18} className={favorited ? "fill-red-500 text-red-500" : ""} />
        {favorited ? "Saved" : "Save"}
        {count > 0 && <span className="text-slate-400">({count})</span>}
      </button>
    );
  }

  return (
    <button
      type="button"
      onClick={handleClick}
      disabled={pendingClick}
      aria-label={favorited ? "Remove from favorites" : "Add to favorites"}
      className={cn(
        "flex items-center justify-center w-8 h-8 rounded-full bg-white/90 backdrop-blur shadow hover:bg-white transition disabled:opacity-60",
        className
      )}
    >
      <Heart
        size={16}
        className={favorited ? "fill-red-500 text-red-500" : "text-slate-500"}
      />
    </button>
  );
}
