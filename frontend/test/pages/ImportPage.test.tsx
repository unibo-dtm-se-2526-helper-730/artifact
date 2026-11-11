import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api, ApiError } from '../../src/api';
import { I18nProvider } from '../../src/i18n';
import { ImportPage } from '../../src/pages/ImportPage';
import type { ImportBatch, ImportResponse } from '../../src/types';

// The page talks to the backend only through api: replace it, keep ApiError real
vi.mock('../../src/api', async (importOriginal) => {
  const original = await importOriginal<typeof import('../../src/api')>();
  return { ...original, api: { imports: vi.fn(), upload: vi.fn(), deleteAll: vi.fn() } };
});

const batch: ImportBatch = {
  id: 1,
  filename: 'revolut_2025.csv',
  broker: 'REVOLUT',
  imported_at: '2026-01-15T10:00:00Z',
  imported: 11,
  duplicates: 0,
  errors: 1,
};

function renderPage(onDataChanged = vi.fn()) {
  render(
    <I18nProvider>
      <ImportPage onDataChanged={onDataChanged} />
    </I18nProvider>,
  );
  return onDataChanged;
}

describe('ImportPage', () => {
  beforeEach(() => {
    localStorage.setItem('lang', 'it'); // texts below are the Italian ones
    vi.mocked(api.imports).mockResolvedValue([batch]);
  });

  it('shows the import history with the broker of each file', async () => {
    vi.mocked(api.imports).mockResolvedValue([
      batch,
      { ...batch, id: 2, filename: 'trade_republic_2025.csv', broker: 'TRADE_REPUBLIC' },
    ]);
    renderPage();

    const row = (await screen.findByText('revolut_2025.csv')).closest('tr')!;
    expect(within(row).getByText('11')).toBeInTheDocument();
    expect(within(row).getByText('Revolut')).toBeInTheDocument();
    const other = screen.getByText('trade_republic_2025.csv').closest('tr')!;
    expect(within(other).getByText('Trade Republic')).toBeInTheDocument();
  });

  it('lists the supported brokers', async () => {
    renderPage();

    expect(
      await screen.findByText('Broker supportati: Revolut, Trade Republic.'),
    ).toBeInTheDocument();
  });

  it('says when there are no imports yet', async () => {
    vi.mocked(api.imports).mockResolvedValue([]);
    renderPage();

    expect(await screen.findByText('Nessun dato.')).toBeInTheDocument();
  });

  it('uploads a CSV and shows the report with the row issues', async () => {
    const report: ImportResponse = {
      batch_id: 2,
      imported: 10,
      duplicates: 1,
      errors: 0,
      issues: [
        {
          file_row: 8,
          severity: 'WARNING',
          code: 'warning.implied_fee',
          params: [],
          message: 'Commissione implicita',
        },
      ],
    };
    vi.mocked(api.upload).mockResolvedValue(report);
    const onDataChanged = renderPage();

    const file = new File(['Date,Ticker'], 'revolut.csv', { type: 'text/csv' });
    await userEvent.upload(document.querySelector<HTMLInputElement>('input[type=file]')!, file);

    expect(
      await screen.findByText('Importate 10 · operazioni duplicate 1 · righe con errori 0'),
    ).toBeInTheDocument();
    expect(screen.getByText('Commissione implicita')).toBeInTheDocument();
    expect(api.upload).toHaveBeenCalledWith(file);
    expect(onDataChanged).toHaveBeenCalled();
  });

  it('shows the message translated by the backend when the import fails', async () => {
    vi.mocked(api.upload).mockRejectedValue(
      new ApiError(400, 'error.unsupported_broker', 'Formato del file non riconosciuto', null),
    );
    renderPage();

    const file = new File(['x'], 'other.csv', { type: 'text/csv' });
    await userEvent.upload(document.querySelector<HTMLInputElement>('input[type=file]')!, file);

    expect(await screen.findByRole('alert')).toHaveTextContent('Formato del file non riconosciuto');
  });

  it('deletes everything only after confirmation', async () => {
    vi.mocked(api.deleteAll).mockResolvedValue({ transactions: 11, imports: 1, instruments: 5 });
    const onDataChanged = renderPage();
    const user = userEvent.setup();

    await user.click(screen.getByRole('button', { name: 'Elimina tutte le operazioni' }));
    expect(api.deleteAll).not.toHaveBeenCalled();

    const dialog = screen.getByRole('dialog');
    await user.click(within(dialog).getByRole('checkbox'));
    await user.click(within(dialog).getByRole('button', { name: 'Elimina' }));

    expect(api.deleteAll).toHaveBeenCalledWith(true);
    expect(
      await screen.findByText('Eliminate 11 operazioni, 1 import e 5 strumenti.'),
    ).toBeInTheDocument();
    expect(onDataChanged).toHaveBeenCalled();
  });

  it('does nothing when the confirmation is cancelled', async () => {
    renderPage();
    const user = userEvent.setup();

    await user.click(screen.getByRole('button', { name: 'Elimina tutte le operazioni' }));
    await user.click(screen.getByRole('button', { name: 'Annulla' }));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(api.deleteAll).not.toHaveBeenCalled();
  });
});
