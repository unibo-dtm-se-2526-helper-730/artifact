import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api, ApiError } from '../../src/api';
import { I18nProvider } from '../../src/i18n';
import { SummaryPage } from '../../src/pages/SummaryPage';
import type { CarriedLoss } from '../../src/types';

vi.mock('../../src/api', async (importOriginal) => {
  const original = await importOriginal<typeof import('../../src/api')>();
  return { ...original, api: { tax: vi.fn(), carriedLosses: vi.fn(), saveCarriedLosses: vi.fn() } };
});

function renderPage(losses: CarriedLoss[], importedYears: number[]) {
  vi.mocked(api.carriedLosses).mockResolvedValue(losses);
  render(
    <I18nProvider>
      <SummaryPage
        year={2026}
        importedYears={importedYears}
        onDataChanged={vi.fn()}
        onGoToInstruments={vi.fn()}
      />
    </I18nProvider>,
  );
}

describe('SummaryPage, losses of previous years', () => {
  beforeEach(() => {
    localStorage.setItem('lang', 'it');
    // the result of the year is not under test here
    vi.mocked(api.tax).mockRejectedValue(new ApiError(500, 'INTERNAL', 'errore', null));
  });

  it('warns about a loss of a year already imported, without blocking the save', async () => {
    renderPage([{ loss_year: 2025, amount: '100' }], [2026, 2025]);

    expect(
      await screen.findByText(/Per il 2025 ci sono già operazioni importate/),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Salva' })).toBeEnabled();
  });

  it('warns about a loss that expired before the first imported year', async () => {
    renderPage([{ loss_year: 2019, amount: '100' }], [2025, 2026]);

    expect(
      await screen.findByText(/Una perdita del 2019 si può usare fino al 2023/),
    ).toBeInTheDocument();
  });

  it('shows no warning for a loss that can still be used', async () => {
    renderPage([{ loss_year: 2023, amount: '100' }], [2025, 2026]);

    expect(await screen.findByDisplayValue('2023')).toBeInTheDocument();
    expect(
      screen.queryByText(/già operazioni importate|non verrà mai utilizzata/),
    ).not.toBeInTheDocument();
  });
});
