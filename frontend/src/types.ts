// Shapes of the JSON returned by the backend (snake_case, amounts as strings).
// Amounts stay strings: they are formatted for display, never used in arithmetic.

export type Lang = 'it' | 'en';

export type AssetClass =
  | 'STOCK'
  | 'BOND'
  | 'GOVERNMENT_BOND_WL'
  | 'GOVERNMENT_BOND_NON_WL'
  | 'UCITS_ETF'
  | 'NON_UCITS_ETF'
  | 'ETC_ETN';

export const ASSET_CLASSES: AssetClass[] = [
  'STOCK',
  'BOND',
  'GOVERNMENT_BOND_WL',
  'GOVERNMENT_BOND_NON_WL',
  'UCITS_ETF',
  'NON_UCITS_ETF',
  'ETC_ETN',
];

/** Same rules as AssetClass in the backend: which optional fields an asset class accepts. */
export const allowsGovernmentBondShare = (c: AssetClass | null) => c === 'UCITS_ETF';
export const allowsForeignWithholding = (c: AssetClass | null) =>
  c === 'STOCK' || c === 'BOND' || c === 'GOVERNMENT_BOND_WL' || c === 'GOVERNMENT_BOND_NON_WL';

/** A translatable message: text is already in the language of the request. */
export interface Msg {
  code: string;
  params: unknown[];
  text: string;
}

/** RFC 9457 problem detail returned by the backend for every error. */
export interface Problem {
  status: number;
  detail: string;
  code: string;
  tickers?: string[];
}

// --- imports -------------------------------------------------------------------

export interface RowIssue {
  file_row: number;
  severity: 'ERROR' | 'WARNING';
  code: string;
  params: unknown[];
  message: string;
}

export interface ImportResponse {
  batch_id: number;
  imported: number;
  duplicates: number;
  errors: number;
  issues: RowIssue[];
}

export interface ImportBatch {
  id: number;
  filename: string;
  broker: string;
  imported_at: string;
  imported: number;
  duplicates: number;
  errors: number;
}

// --- instruments and transactions ------------------------------------------------

export interface Instrument {
  ticker: string;
  isin: string | null;
  name: string | null;
  asset_class: AssetClass | null;
  government_bond_share: string | null;
  foreign_withholding_rate: string | null;
  classified: boolean;
}

export interface InstrumentRequest {
  isin: string | null;
  name: string | null;
  asset_class: AssetClass | null;
  government_bond_share: string | null;
  foreign_withholding_rate: string | null;
}

export type OperationType =
  | 'BUY'
  | 'SELL'
  | 'DIVIDEND'
  | 'COUPON'
  | 'CASH_TOP_UP'
  | 'CASH_WITHDRAWAL'
  | 'CUSTODY_FEE';

export interface Transaction {
  id: string;
  executed_at: string;
  tax_year: number;
  type: OperationType;
  ticker: string | null;
  quantity: string | null;
  price_per_share: string | null;
  total_amount: string;
  currency: string;
  file_row: number;
}

export interface DeleteResponse {
  transactions: number;
  imports: number;
  instruments: number;
}

// --- prices and carried losses -----------------------------------------------------

export interface Prices {
  year: number;
  prices: Record<string, string>;
}

export interface CarriedLoss {
  loss_year: number;
  amount: string;
}

// --- tax ------------------------------------------------------------------------

export interface YearSummary {
  year: number;
  total_tax: string | null;
  zainetto_total: string | null;
  event_count: number | null;
  error_code: string | null;
}

export interface Consumption {
  buy_transaction_id: string;
  acquired_at: string;
  quantity: string;
  cost: string;
  cash_cost: string;
  proceeds: string;
  gain: string;
}

export type EventType = 'CAPITAL_GAIN' | 'CAPITAL_LOSS' | 'DIVIDEND' | 'COUPON';
export type IncomeCategory = 'MISCELLANEOUS' | 'CAPITAL' | 'EXCLUDED';

export interface TaxEvent {
  transaction_id: string;
  executed_at: string;
  ticker: string;
  type: EventType;
  category: IncomeCategory;
  amount: string;
  weight: string;
  foreign_tax: string;
  taxable_amount: string;
  consumptions: Consumption[];
}

export interface Loss {
  year: number;
  amount: string;
  remaining: string;
  last_usable_year: number;
}

export interface Zainetto {
  opening: Loss[];
  expired: Loss[];
  gains: string;
  losses: string;
  compensations: { loss_year: number; used: string }[];
  realized_loss: Loss | null;
  closing: Loss[];
  opening_total: string;
  used_total: string;
  closing_total: string;
}

export interface TaxYear {
  year: number;
  economic_result: string;
  miscellaneous_taxable: string;
  capital_taxable: string;
  rate: string;
  miscellaneous_tax: string;
  capital_tax: string;
  total_tax: string;
  payment_threshold: string;
  below_threshold: boolean;
  payable_tax: string;
  zainetto: Zainetto;
  events: TaxEvent[];
}

export interface IvafeRow {
  ticker: string;
  quantity: string;
  from: string;
  to: string;
  sold: boolean;
  year_end_price: string | null;
  initial_value: string | null;
  final_value: string | null;
  holding_days: number;
  ivafe: string | null;
  notes: Msg[];
}

export interface Ivafe {
  year: number;
  rate: string;
  total: string;
  payment_threshold: string;
  below_threshold: boolean;
  complete: boolean;
  missing_prices: string[];
  rows: IvafeRow[];
}

export interface DeclarationItem {
  key: string;
  description: Msg;
  amount: string;
  rigo: string;
  detail: Msg | null;
  secondary: string | null;
}

export interface Declaration {
  year: number;
  model: string;
  sections: { quadro: string; title: Msg; items: DeclarationItem[] }[];
  warnings: Msg[];
}

export interface Position {
  ticker: string;
  name: string | null;
  asset_class: AssetClass | null;
  quantity: string;
  cost: string;
  tax_cost: string;
  average_cost: string;
  weight: string;
}

export interface Portfolio {
  positions: Position[];
  by_asset_class: { asset_class: AssetClass | null; cost: string; weight: string }[];
  total_cost: string;
}
