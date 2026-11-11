/** Years a loss can be carried forward, as in the backend rules (CurrentRegime). */
export const LOSS_CARRY_FORWARD_YEARS = 4;

/**
 * Why a loss entered by hand may be wrong, or null if it looks fine. The app cannot tell a
 * duplicate from a loss realized at another broker, so these are warnings, never blocks.
 * - imported: the year already has imported operations, whose losses the app computes itself;
 * - expired: the loss could be used only before the first imported year, so it is never used.
 *
 * @param importedYears tax years with imported operations, in any order
 */
export function carriedLossWarning(
  lossYear: number,
  importedYears: number[],
): 'imported' | 'expired' | null {
  if (importedYears.length === 0 || !Number.isInteger(lossYear)) return null;
  if (importedYears.includes(lossYear)) return 'imported';
  if (lossYear + LOSS_CARRY_FORWARD_YEARS < Math.min(...importedYears)) return 'expired';
  return null;
}
