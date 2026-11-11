import { describe, expect, it } from 'vitest';
import { carriedLossWarning } from '../src/carriedLosses';

describe('carriedLossWarning', () => {
  const imported = [2026, 2024, 2025];

  it('warns about a year that already has imported transactions', () => {
    expect(carriedLossWarning(2025, imported)).toBe('imported');
  });

  it('warns about a loss that expired before the first imported year', () => {
    // a 2019 loss can be used until 2023, the first imported year is 2024
    expect(carriedLossWarning(2019, imported)).toBe('expired');
  });

  it('accepts a loss that can still offset an imported year', () => {
    // a 2020 loss can be used until 2024
    expect(carriedLossWarning(2020, imported)).toBeNull();
    expect(carriedLossWarning(2023, imported)).toBeNull();
  });

  it('says nothing before the first import', () => {
    expect(carriedLossWarning(2010, [])).toBeNull();
  });
});
