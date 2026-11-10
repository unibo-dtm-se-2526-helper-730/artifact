import { describe, expect, it, vi } from 'vitest';
import { api, ApiError, setApiLanguage } from '../src/api';

/** Replaces fetch with a mock that answers once with the given status and body. */
function answer(status: number, body?: unknown, statusText = '') {
  const response =
    body === undefined
      ? new Response('<html>Bad Gateway</html>', { status, statusText })
      : new Response(JSON.stringify(body), {
          status,
          statusText,
          headers: { 'Content-Type': 'application/json' },
        });
  const fetchMock = vi.fn().mockResolvedValue(response);
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

/** URL, method, headers and body of the only call made to fetch. */
function lastCall(fetchMock: ReturnType<typeof vi.fn>) {
  const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
  return { url, method: init.method ?? 'GET', headers: new Headers(init.headers), body: init.body };
}

describe('api requests', () => {
  it('calls the versioned endpoint and returns the JSON', async () => {
    const fetchMock = answer(200, [{ ticker: 'VGWD' }]);

    await expect(api.instruments()).resolves.toEqual([{ ticker: 'VGWD' }]);
    expect(lastCall(fetchMock).url).toBe('/api/v1/instruments');
  });

  it('sends the language of the interface to the backend', async () => {
    const fetchMock = answer(200, []);

    setApiLanguage('en');
    await api.imports();
    expect(lastCall(fetchMock).headers.get('Accept-Language')).toBe('en');

    setApiLanguage('it');
  });

  it('sends JSON bodies with PUT', async () => {
    const fetchMock = answer(200, { year: 2025, prices: { VGWD: '70' } });

    await api.savePrices(2025, { VGWD: '70' });
    const call = lastCall(fetchMock);
    expect(call.url).toBe('/api/v1/prices/2025');
    expect(call.method).toBe('PUT');
    expect(call.headers.get('Content-Type')).toBe('application/json');
    expect(JSON.parse(call.body as string)).toEqual({ prices: { VGWD: '70' } });
  });

  it('escapes tickers in the path', async () => {
    const fetchMock = answer(200, {});

    await api.saveInstrument('A/B', {
      isin: null,
      name: null,
      asset_class: 'STOCK',
      government_bond_share: null,
      foreign_withholding_rate: null,
    });
    expect(lastCall(fetchMock).url).toBe('/api/v1/instruments/A%2FB');
  });

  it('uploads the CSV as multipart form data', async () => {
    const fetchMock = answer(201, {
      batch_id: 1,
      imported: 1,
      duplicates: 0,
      errors: 0,
      issues: [],
    });
    const file = new File(['Date,Ticker'], 'revolut.csv', { type: 'text/csv' });

    await api.upload(file);
    const call = lastCall(fetchMock);
    expect(call.method).toBe('POST');
    expect((call.body as FormData).get('file')).toBeInstanceOf(File);
  });

  it('passes query parameters for delete all', async () => {
    const fetchMock = answer(200, { transactions: 0, imports: 0, instruments: 0 });

    await api.deleteAll(true);
    const call = lastCall(fetchMock);
    expect(call.url).toBe('/api/v1/transactions?instruments=true');
    expect(call.method).toBe('DELETE');
  });
});

describe('api errors', () => {
  it('turns a problem detail into an ApiError with the translated text', async () => {
    const problem = { status: 422, code: 'error.unclassified', detail: 'Classifica VGWD' };
    answer(422, problem);

    const error = await api.tax(2025).catch((e: unknown) => e);
    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({
      status: 422,
      code: 'error.unclassified',
      message: 'Classifica VGWD',
    });
    expect((error as ApiError).problem).toEqual(problem);
  });

  it('falls back to the HTTP status when the body is not a problem detail', async () => {
    answer(502, undefined, 'Bad Gateway');

    await expect(api.portfolio()).rejects.toMatchObject({
      status: 502,
      code: 'HTTP_502',
      message: 'Bad Gateway',
      problem: null,
    });
  });

  it('reports a network error when the backend cannot be reached', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));

    await expect(api.years()).rejects.toMatchObject({ status: 0, code: 'NETWORK' });
  });
});
