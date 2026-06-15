import { Star } from "lucide-react";
import { cn, STARS } from "@/lib/utils";

interface StarRatingProps {
  rating: number;        // 0–5 (can be decimal for display)
  size?: "sm" | "md";
  interactive?: false;
  className?: string;
}

interface InteractiveStarRatingProps {
  rating: number;
  onChange: (rating: number) => void;
  size?: "sm" | "md";
  interactive: true;
  className?: string;
}

type Props = StarRatingProps | InteractiveStarRatingProps;

const sizeMap = { sm: 14, md: 18 };

/**
 * Read-only or interactive 1–5 star rating component.
 * Nur-Lese oder interaktive 1–5 Sternebewertung.
 */
export function StarRating(props: Props) {
  const { rating, size = "md", className } = props;
  const px = sizeMap[size];

  return (
    <div className={cn("flex items-center gap-0.5", className)}>
      {STARS.map((star) => {
        const filled = star <= Math.round(rating);
        return (
          <button
            key={star}
            type="button"
            onClick={
              props.interactive
                ? () => (props as InteractiveStarRatingProps).onChange(star)
                : undefined
            }
            disabled={!props.interactive}
            className={cn(
              "focus:outline-none transition-transform",
              props.interactive && "hover:scale-110 cursor-pointer",
              !props.interactive && "cursor-default"
            )}
          >
            <Star
              size={px}
              className={cn(
                filled ? "fill-amber-400 text-amber-400" : "text-slate-300"
              )}
            />
          </button>
        );
      })}
    </div>
  );
}
