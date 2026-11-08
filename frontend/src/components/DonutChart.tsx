import { useState } from 'react';

export interface Slice {
  key: string;
  label: string;
  /** Real share of the total, between 0 and 1: shown in tooltip and legend. */
  weight: number;
  /** Already formatted value for the tooltip (e.g. "€ 382,05"). */
  value: string;
  color: string;
}

interface Props {
  slices: Slice[];
  centerLabel: string;
  centerValue: string;
  formatWeight: (weight: number) => string;
}

/** Below this share a slice would be a hairline: it is drawn this big, the table keeps the real weight. */
const MIN_VISIBLE_SHARE = 0.012;

const SIZE = 220;
const CENTER = SIZE / 2;
const OUTER = 100;
const INNER = 68;

/** SVG path of one ring segment between two angles (radians, 0 = top, clockwise). */
function arc(start: number, end: number): string {
  const point = (radius: number, angle: number) =>
    `${CENTER + radius * Math.sin(angle)} ${CENTER - radius * Math.cos(angle)}`;
  const large = end - start > Math.PI ? 1 : 0;
  return [
    `M ${point(OUTER, start)}`,
    `A ${OUTER} ${OUTER} 0 ${large} 1 ${point(OUTER, end)}`,
    `L ${point(INNER, end)}`,
    `A ${INNER} ${INNER} 0 ${large} 0 ${point(INNER, start)}`,
    'Z',
  ].join(' ');
}

/**
 * Donut chart with hover tooltip and legend. Identity is never color alone:
 * every slice is named in the legend, and the page shows the same data as a table.
 */
export function DonutChart({ slices, centerLabel, centerValue, formatWeight }: Props) {
  const [active, setActive] = useState<string | null>(null);

  // display shares: small slices raised to the minimum, then everything rescaled to 100%
  const raised = slices.map((s) => Math.max(s.weight, MIN_VISIBLE_SHARE));
  const sum = raised.reduce((a, b) => a + b, 0) || 1;
  let angle = 0;
  const segments = slices.map((slice, i) => {
    const start = angle;
    angle += (raised[i] / sum) * 2 * Math.PI;
    return { slice, start, end: angle };
  });

  const activeSlice = slices.find((s) => s.key === active);

  return (
    <div className="donut">
      <svg
        viewBox={`0 0 ${SIZE} ${SIZE}`}
        className="donut-svg"
        role="img"
        aria-label={slices.map((s) => `${s.label} ${formatWeight(s.weight)}`).join(', ')}
      >
        {segments.length === 1 ? (
          // a single position is a full ring: one arc cannot span 360 degrees
          <circle
            cx={CENTER}
            cy={CENTER}
            r={(OUTER + INNER) / 2}
            fill="none"
            stroke={segments[0].slice.color}
            strokeWidth={OUTER - INNER}
            onMouseEnter={() => setActive(segments[0].slice.key)}
            onMouseLeave={() => setActive(null)}
          />
        ) : (
          segments.map(({ slice, start, end }) => (
            <path
              key={slice.key}
              d={arc(start, end)}
              fill={slice.color}
              className={`donut-slice ${active && active !== slice.key ? 'dimmed' : ''}`}
              onMouseEnter={() => setActive(slice.key)}
              onMouseLeave={() => setActive(null)}
            />
          ))
        )}
        <text x={CENTER} y={CENTER - 6} textAnchor="middle" className="donut-center-label">
          {activeSlice ? activeSlice.label : centerLabel}
        </text>
        <text x={CENTER} y={CENTER + 16} textAnchor="middle" className="donut-center-value">
          {activeSlice ? `${activeSlice.value} · ${formatWeight(activeSlice.weight)}` : centerValue}
        </text>
      </svg>

      <ul className="legend">
        {slices.map((s) => (
          <li
            key={s.key}
            className={active === s.key ? 'active' : ''}
            onMouseEnter={() => setActive(s.key)}
            onMouseLeave={() => setActive(null)}
          >
            <span className="swatch" style={{ background: s.color }} aria-hidden="true" />
            <span className="legend-label">{s.label}</span>
            <span className="legend-value">{formatWeight(s.weight)}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
