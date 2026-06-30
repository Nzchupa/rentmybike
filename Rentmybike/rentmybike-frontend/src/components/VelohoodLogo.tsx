interface VelohoodLogoProps {
  size?: number;
}

/**
 * Velohood map-pin logo — renders the SVG icon inline.
 * Uses currentColor so the parent's text color drives the pin fill.
 * The bike frame strokes are always white (designed for colored backgrounds).
 */
export function VelohoodLogo({ size = 24 }: VelohoodLogoProps) {
  const width = Math.round((size * 46) / 56);
  return (
    <svg
      width={width}
      height={size}
      viewBox="0 0 46 56"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      {/* Pin shape */}
      <path
        d="M23 2 C11.4 2 2 11.4 2 23 C2 36 23 54 23 54 C23 54 44 36 44 23 C44 11.4 34.6 2 23 2 Z"
        fill="currentColor"
      />
      {/* Rear wheel */}
      <circle cx="14" cy="29" r="5" stroke="white" strokeWidth="2" fill="none" />
      {/* Front wheel */}
      <circle cx="32" cy="29" r="5" stroke="white" strokeWidth="2" fill="none" />
      {/* Chain stays */}
      <line x1="23" y1="29" x2="14" y2="29" stroke="white" strokeWidth="1.8" strokeLinecap="round" />
      {/* Seat stays */}
      <line x1="14" y1="29" x2="21" y2="19" stroke="white" strokeWidth="1.5" strokeLinecap="round" />
      {/* Seat tube */}
      <line x1="23" y1="29" x2="21" y2="19" stroke="white" strokeWidth="1.8" strokeLinecap="round" />
      {/* Top tube */}
      <line x1="21" y1="19" x2="30" y2="19" stroke="white" strokeWidth="1.5" strokeLinecap="round" />
      {/* Down tube */}
      <line x1="30" y1="19" x2="23" y2="29" stroke="white" strokeWidth="1.8" strokeLinecap="round" />
      {/* Front fork */}
      <line x1="30" y1="19" x2="32" y2="29" stroke="white" strokeWidth="1.8" strokeLinecap="round" />
      {/* Seat post */}
      <line x1="21" y1="19" x2="21" y2="15" stroke="white" strokeWidth="1.5" strokeLinecap="round" />
      {/* Saddle */}
      <line x1="18.5" y1="15" x2="23.5" y2="15" stroke="white" strokeWidth="2.2" strokeLinecap="round" />
      {/* Handlebar stem */}
      <line x1="30" y1="19" x2="30" y2="15" stroke="white" strokeWidth="1.5" strokeLinecap="round" />
      {/* Handlebar — same level as saddle */}
      <line x1="27.5" y1="15" x2="32.5" y2="15" stroke="white" strokeWidth="2.2" strokeLinecap="round" />
    </svg>
  );
}
