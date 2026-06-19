"use client";

import { useId, useState } from "react";

export interface ChartPoint {
  label: string;
  value: number;
}

interface TimeSeriesChartProps {
  data: ChartPoint[];
  color?: string;
  height?: number;
  formatValue?: (v: number) => string;
}

/**
 * Lightweight dependency-free line chart for the admin stats dashboard.
 * No charting library (recharts, chart.js, ...) is installed in this project,
 * and adding one is unnecessary for a handful of simple time-series — a small
 * hand-rolled SVG component covers it without growing the bundle or the
 * dependency surface.
 * Leichtgewichtiges, abhängigkeitsfreies Liniendiagramm für das
 * Admin-Statistik-Dashboard. In diesem Projekt ist keine
 * Diagramm-Bibliothek installiert, und für ein paar einfache Zeitreihen
 * lohnt sich keine — eine kleine handgebaute SVG-Komponente deckt das ab,
 * ohne Bundle oder Abhängigkeitsfläche zu vergrößern.
 */
export function TimeSeriesChart({
  data,
  color = "#0ea5e9",
  height = 160,
  formatValue = (v) => String(v),
}: TimeSeriesChartProps) {
  const gradientId = useId();
  const [hoverIndex, setHoverIndex] = useState<number | null>(null);

  const width = 600; // viewBox units — scales responsively via SVG width=100%
  const padX = 8;
  const padTop = 12;
  const padBottom = 24;

  const values = data.map((d) => d.value);
  const max = Math.max(1, ...values); // avoid divide-by-zero on all-zero data
  const innerW = width - padX * 2;
  const innerH = height - padTop - padBottom;

  const points = data.map((d, i) => {
    const x = data.length === 1 ? padX : padX + (i / (data.length - 1)) * innerW;
    const y = padTop + innerH - (d.value / max) * innerH;
    return { x, y, ...d };
  });

  const linePath = points
    .map((p, i) => `${i === 0 ? "M" : "L"}${p.x.toFixed(1)},${p.y.toFixed(1)}`)
    .join(" ");
  const areaPath = `${linePath} L${points[points.length - 1]?.x.toFixed(1)},${(padTop + innerH).toFixed(1)} L${points[0]?.x.toFixed(1)},${(padTop + innerH).toFixed(1)} Z`;

  // Only label a handful of x-axis ticks (first, middle, last) — printing one
  // label per day would overlap badly on a 90-day range.
  // Nur eine Handvoll x-Achsen-Beschriftungen (erste, mittlere, letzte) —
  // eine Beschriftung pro Tag würde sich bei einem 90-Tage-Zeitraum stark
  // überlappen.
  const tickIndices = data.length <= 1
    ? [0]
    : Array.from(new Set([0, Math.floor((data.length - 1) / 2), data.length - 1]));

  const hovered = hoverIndex !== null ? points[hoverIndex] : null;

  return (
    <div className="relative w-full">
      <svg
        viewBox={`0 0 ${width} ${height}`}
        className="w-full"
        height={height}
        onMouseLeave={() => setHoverIndex(null)}
      >
        <defs>
          <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={color} stopOpacity={0.25} />
            <stop offset="100%" stopColor={color} stopOpacity={0} />
          </linearGradient>
        </defs>

        <path d={areaPath} fill={`url(#${gradientId})`} stroke="none" />
        <path d={linePath} fill="none" stroke={color} strokeWidth={2} />

        {points.map((p, i) => (
          <circle
            key={i}
            cx={p.x}
            cy={p.y}
            r={hoverIndex === i ? 4 : 0}
            fill={color}
            className="transition-all"
          />
        ))}

        {/* Invisible wide hit-targets, one per data point, for hover tooltips */}
        {points.map((p, i) => (
          <rect
            key={`hit-${i}`}
            x={p.x - innerW / Math.max(data.length, 1) / 2}
            y={0}
            width={innerW / Math.max(data.length, 1)}
            height={height}
            fill="transparent"
            onMouseEnter={() => setHoverIndex(i)}
          />
        ))}

        {tickIndices.map((i) => (
          <text
            key={i}
            x={points[i].x}
            y={height - 6}
            textAnchor="middle"
            fontSize={10}
            fill="#94a3b8"
          >
            {data[i].label}
          </text>
        ))}
      </svg>

      {hovered && (
        <div
          className="absolute -translate-x-1/2 -translate-y-full bg-slate-900 text-white text-xs rounded-lg px-2 py-1 pointer-events-none whitespace-nowrap"
          style={{
            left: `${(hovered.x / width) * 100}%`,
            top: `${(hovered.y / height) * 100}%`,
          }}
        >
          {hovered.label}: {formatValue(hovered.value)}
        </div>
      )}
    </div>
  );
}
