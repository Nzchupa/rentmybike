import Image from "next/image";
import { getInitials, cn } from "@/lib/utils";

interface AvatarProps {
  name: string;
  avatarUrl?: string | null;
  size?: "sm" | "md" | "lg" | "xl";
  className?: string;
}

const sizeMap = {
  sm: { px: 28, text: "text-xs" },
  md: { px: 36, text: "text-sm" },
  lg: { px: 48, text: "text-base" },
  xl: { px: 80, text: "text-xl" },
};

/**
 * Avatar with image or initials fallback.
 * Avatar mit Bild oder Initialen-Fallback.
 */
export function Avatar({ name, avatarUrl, size = "md", className }: AvatarProps) {
  const { px, text } = sizeMap[size];

  if (avatarUrl) {
    return (
      <div
        className={cn("relative shrink-0 overflow-hidden rounded-full", className)}
        style={{ width: px, height: px }}
      >
        <Image
          src={avatarUrl}
          alt={name}
          fill
          sizes={`${px}px`}
          className="object-cover"
        />
      </div>
    );
  }

  return (
    <div
      className={cn(
        "shrink-0 rounded-full bg-brand-500 text-white flex items-center justify-center font-semibold",
        text,
        className
      )}
      style={{ width: px, height: px }}
    >
      {getInitials(name)}
    </div>
  );
}
