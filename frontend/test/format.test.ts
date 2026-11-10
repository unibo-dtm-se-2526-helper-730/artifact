import { describe, expect, it } from 'vitest';
import {
  date,
  dateTime,
  fractionToPercent,
  money,
  percent,
  percentToFraction,
  quantity,
} from '../src/format';

// Intl puts narrow no-break spaces in some locales: compare on plain spaces
const plain = (s: string) => s.replace(/[\u00a0\u202f]/g, ' ');

describe('money', () => {
  it('formats euro amounts in the language of the interface', () => {
    expect(plain(money('1234567.5', 'it'))).toBe('1.234.567,50 €');
    expect(money('1234567.5', 'en')).toBe('€1,234,567.50');
  });

  it('shows a dash when there is no amount', () => {
    expect(money(null, 'it')).toBe('-');
    expect(money(undefined, 'en')).toBe('-');
  });
});

describe('quantity', () => {
  it('keeps every decimal of fractional shares', () => {
    expect(quantity('0.12345678', 'en')).toBe('0.12345678');
    expect(quantity('3', 'it')).toBe('3');
  });

  it('shows a dash when there is no quantity', () => {
    expect(quantity(null, 'it')).toBe('-');
  });
});

describe('percent', () => {
  it('shows a fraction as a percentage', () => {
    // the space before % depends on the ICU version of Node
    expect(plain(percent('0.26', 'it'))).toMatch(/^26 ?%$/);
    expect(percent(0.125, 'en', 1)).toBe('12.5%');
  });
});

describe('dates', () => {
  it('shows instants in Italian time', () => {
    // 23:30 UTC on 31 December is already 1 January in Rome
    expect(plain(dateTime('2025-12-31T23:30:00Z', 'it'))).toBe('01/01/26, 00:30');
  });

  it('shows a local date as it is, whatever the time zone', () => {
    expect(date('2025-12-31', 'en')).toBe('31 Dec 2025');
  });
});

describe('percentage fields', () => {
  it('turns what the user types into a fraction for the API', () => {
    expect(percentToFraction('40')).toBe('0.4');
    expect(percentToFraction(' 40,5 ')).toBe('0.405');
    expect(percentToFraction('')).toBeNull();
  });

  it('passes invalid input through, so the backend rejects it with a message', () => {
    expect(percentToFraction('abc')).toBe('abc');
  });

  it('turns a fraction from the API back into a percentage', () => {
    expect(fractionToPercent('0.405')).toBe('40.5');
    expect(fractionToPercent(null)).toBe('');
  });
});
