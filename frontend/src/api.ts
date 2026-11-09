import type {
  CarriedLoss,
  Declaration,
  DeleteResponse,
  ImportBatch,
  ImportResponse,
  Instrument,
  InstrumentRequest,
  Ivafe,
  Lang,
  Portfolio,
  Prices,
  Problem,
  TaxYear,
  Transaction,
  YearSummary,
} from './types';

/** An error answered by the backend: detail is already translated, code is stable. */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly problem: Problem | null;

  constructor(status: number, code: string, detail: string, problem: Problem | null) {
    super(detail);
    this.status = status;
    this.code = code;
    this.problem = problem;
  }
}

// The language of the backend messages follows the language chosen in the UI.
let language: Lang = 'it';
export const setApiLanguage = (lang: Lang) => {
  language = lang;
};

const BASE = '/api/v1';

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set('Accept-Language', language);
  let response: Response;
  try {
    response = await fetch(BASE + path, { ...init, headers });
  } catch {
    // network error: the backend is not running or not reachable
    throw new ApiError(0, 'NETWORK', '', null);
  }
  if (!response.ok) {
    let problem: Problem | null = null;
    try {
      problem = (await response.json()) as Problem;
    } catch {
      // not a problem detail (e.g. a proxy error page)
    }
    throw new ApiError(
      response.status,
      problem?.code ?? `HTTP_${response.status}`,
      problem?.detail ?? response.statusText,
      problem,
    );
  }
  return (await response.json()) as T;
}

const json = (method: string, body: unknown): RequestInit => ({
  method,
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(body),
});

/** One function per endpoint of the backend. */
export const api = {
  upload(file: File) {
    const form = new FormData();
    form.append('file', file);
    return request<ImportResponse>('/imports', { method: 'POST', body: form });
  },
  imports: () => request<ImportBatch[]>('/imports'),

  instruments: () => request<Instrument[]>('/instruments'),
  saveInstrument: (ticker: string, body: InstrumentRequest) =>
    request<Instrument>(`/instruments/${encodeURIComponent(ticker)}`, json('PUT', body)),

  transactions: (year: number) => request<Transaction[]>(`/transactions?year=${year}`),
  deleteAll: (withInstruments: boolean) =>
    request<DeleteResponse>(`/transactions?instruments=${withInstruments}`, { method: 'DELETE' }),

  prices: (year: number) => request<Prices>(`/prices/${year}`),
  savePrices: (year: number, prices: Record<string, string>) =>
    request<Prices>(`/prices/${year}`, json('PUT', { prices })),

  carriedLosses: () => request<CarriedLoss[]>('/carried-losses'),
  saveCarriedLosses: (losses: CarriedLoss[]) => request<CarriedLoss[]>('/carried-losses', json('PUT', losses)),

  years: () => request<YearSummary[]>('/tax/years'),
  tax: (year: number) => request<TaxYear>(`/tax/${year}`),
  ivafe: (year: number) => request<Ivafe>(`/tax/${year}/ivafe`),
  declaration: (year: number) => request<Declaration>(`/tax/${year}/declaration`),
  portfolio: () => request<Portfolio>('/portfolio'),
};
