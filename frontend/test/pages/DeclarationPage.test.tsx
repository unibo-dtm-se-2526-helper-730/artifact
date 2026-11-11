import { render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from '../../src/api';
import { I18nProvider } from '../../src/i18n';
import { DeclarationPage } from '../../src/pages/DeclarationPage';
import type { Declaration } from '../../src/types';

vi.mock('../../src/api', async (importOriginal) => {
  const original = await importOriginal<typeof import('../../src/api')>();
  return { ...original, api: { declaration: vi.fn() } };
});

function declarationOf(year: number): Declaration {
  return {
    year,
    model: `730/${year + 1}`,
    sections: [],
    warnings: [
      {
        code: 'declaration.unknown_state_of_origin',
        params: ['VGWD'],
        text: 'VGWD: Stato da inserire',
      },
      {
        code: 'declaration.missing_year_end_price',
        params: ['VWCE'],
        text: 'VWCE: manca il prezzo',
      },
    ],
  };
}

function renderPage(year: number) {
  vi.mocked(api.declaration).mockResolvedValue(declarationOf(year));
  render(
    <I18nProvider>
      <DeclarationPage year={year} onGoToInstruments={vi.fn()} />
    </I18nProvider>,
  );
}

describe('DeclarationPage', () => {
  beforeEach(() => {
    localStorage.setItem('lang', 'it');
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date('2026-11-11T12:00:00Z'));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('does not ask the prices at 31/12 while the year is not over', async () => {
    renderPage(2026);

    expect(await screen.findByText('VGWD: Stato da inserire')).toBeInTheDocument();
    expect(screen.queryByText('VWCE: manca il prezzo')).not.toBeInTheDocument();
    expect(screen.getByText(/Il 2026 non è ancora finito/)).toBeInTheDocument();
  });

  it('shows every warning for a year that is over', async () => {
    renderPage(2025);

    expect(await screen.findByText('VWCE: manca il prezzo')).toBeInTheDocument();
    expect(screen.getByText('VGWD: Stato da inserire')).toBeInTheDocument();
    expect(screen.queryByText(/non è ancora finito/)).not.toBeInTheDocument();
  });
});
