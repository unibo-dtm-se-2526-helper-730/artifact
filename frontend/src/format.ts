import type { Lang } from './types';

// Display formatting only: the values stay exact strings everywhere else.
// Dates are shown in Italian time, like the tax year is computed.

const locale = (lang: Lang) => (lang === 'it' ? 'it-IT' : 'en-GB');
const ZONE = 'Europe/Rome';

export function money(value: string | null | undefined, lang: Lang): string {
  if (value == null) return '-';
  return new Intl.NumberFormat(locale(lang), { style: 'currency', currency: 'EUR' }).format(
    Number(value),
  );
}

/** Quantities keep every decimal they have (fractional shares have up to 8). */
export function quantity(value: string | null | undefined, lang: Lang): string {
  if (value == null) return '-';
  return new Intl.NumberFormat(locale(lang), { maximumFractionDigits: 10 }).format(Number(value));
}

/** A fraction (0.26) shown as a percentage (26%). */
export function percent(fraction: string | number, lang: Lang, digits = 2): string {
  return new Intl.NumberFormat(locale(lang), {
    style: 'percent',
    maximumFractionDigits: digits,
  }).format(Number(fraction));
}

export function dateTime(iso: string, lang: Lang): string {
  return new Intl.DateTimeFormat(locale(lang), {
    dateStyle: 'short',
    timeStyle: 'short',
    timeZone: ZONE,
  }).format(new Date(iso));
}

export function date(iso: string, lang: Lang): string {
  // LocalDate values ("2025-12-31") have no time zone: format them as they are
  const value = iso.length === 10 ? new Date(`${iso}T12:00:00Z`) : new Date(iso);
  return new Intl.DateTimeFormat(locale(lang), { dateStyle: 'medium', timeZone: ZONE }).format(
    value,
  );
}

/** "40" or "40,5" typed in a percentage field -> "0.4" / "0.405" for the API. */
export function percentToFraction(input: string): string | null {
  const text = input.trim().replace(',', '.');
  if (text === '') return null;
  const value = Number(text);
  if (Number.isNaN(value)) return text; // let the backend reject it with a clear message
  return String(Number((value / 100).toFixed(8)));
}

/** "0.4" from the API -> "40" for a percentage field. */
export function fractionToPercent(fraction: string | null): string {
  if (fraction == null) return '';
  return String(Number((Number(fraction) * 100).toFixed(6)));
}
