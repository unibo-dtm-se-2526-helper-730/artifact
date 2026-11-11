import { useEffect, useState } from 'react';
import { api } from '../api';
import { ErrorNotice, Notice } from '../components/Notice';
import { currentYear, date, money, quantity } from '../format';
import { useI18n } from '../i18n';
import type { Ivafe } from '../types';
import { useLoad } from '../useLoad';

/**
 * IVAFE table and the prices at 31/12 it needs: those of the year for the
 * securities still held, those of the previous year for the ones held on 1 January.
 */
export function IvafePage({ year }: { year: number }) {
  const { t, lang } = useI18n();
  const data = useLoad(
    () => Promise.all([api.ivafe(year), api.prices(year), api.prices(year - 1)]),
    [year],
  );

  const [ivafe, prices, previous] = data.data ?? [null, null, null];
  // the prices at 31/12 exist only once the year is over
  const yearClosed = year < currentYear();

  return (
    <section>
      <h1>{t('ivafe.title', { year })}</h1>
      {data.error != null && <ErrorNotice error={data.error} />}

      {ivafe && prices && previous && (
        <>
          <div className="hero">
            <div>
              <p className="muted">{t('ivafe.total')}</p>
              <p className="hero-value">{money(ivafe.total, lang)}</p>
              {ivafe.below_threshold && (
                <p className="muted">
                  {t('ivafe.below', { threshold: money(ivafe.payment_threshold, lang) })}
                </p>
              )}
            </div>
          </div>
          {!yearClosed && <Notice tone="info">{t('ivafe.yearOpen', { year })}</Notice>}
          {yearClosed && !ivafe.complete && (
            <Notice tone="warning">
              {t('ivafe.missing', { tickers: ivafe.missing_prices.join(', ') })}
            </Notice>
          )}

          {ivafe.rows.length === 0 ? (
            <p className="muted">{t('common.empty')}</p>
          ) : (
            <table className="table">
              <thead>
                <tr>
                  <th>{t('ivafe.ticker')}</th>
                  <th className="num">{t('portfolio.quantity')}</th>
                  <th>{t('ivafe.period')}</th>
                  <th className="num">{t('ivafe.days')}</th>
                  <th className="num">{t('ivafe.initial')}</th>
                  <th className="num">{t('ivafe.final')}</th>
                  <th className="num">{t('ivafe.amount')}</th>
                  <th>{t('ivafe.notes')}</th>
                </tr>
              </thead>
              <tbody>
                {ivafe.rows.map((row, i) => (
                  <tr key={i}>
                    <td>{row.ticker}</td>
                    <td className="num">{quantity(row.quantity, lang)}</td>
                    <td className="nowrap">
                      {date(row.from, lang)} – {date(row.to, lang)}
                    </td>
                    <td className="num">{row.holding_days}</td>
                    <td className="num">{money(row.initial_value, lang)}</td>
                    <td className="num">{money(row.final_value, lang)}</td>
                    <td className="num">{money(row.ivafe, lang)}</td>
                    <td className="small">{row.notes.map((n) => n.text).join(' ')}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          <PriceForm
            year={year}
            ivafe={ivafe}
            prices={prices.prices}
            previousPrices={previous.prices}
            yearClosed={yearClosed}
            onSaved={data.reload}
          />
        </>
      )}
    </section>
  );
}

interface PriceFormProps {
  year: number;
  ivafe: Ivafe;
  prices: Record<string, string>;
  previousPrices: Record<string, string>;
  yearClosed: boolean;
  onSaved: () => void;
}

function PriceForm({ year, ivafe, prices, previousPrices, yearClosed, onSaved }: PriceFormProps) {
  const { t } = useI18n();
  const firstDay = `${year}-01-01`;

  // which prices are needed: held at 31/12 of this year (once it is over),
  // held on 1/1 (= 31/12 of the year before)
  const heldAtEnd = yearClosed
    ? unique(ivafe.rows.filter((r) => !r.sold).map((r) => r.ticker))
    : [];
  const heldAtStart = unique(ivafe.rows.filter((r) => r.from === firstDay).map((r) => r.ticker));

  const [current, setCurrent] = useState(prices);
  const [before, setBefore] = useState(previousPrices);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<unknown>(null);

  // new data after a save or a year change: start again from what the server has
  useEffect(() => setCurrent(prices), [prices]);
  useEffect(() => setBefore(previousPrices), [previousPrices]);

  if (heldAtEnd.length === 0 && heldAtStart.length === 0) return null;

  async function save() {
    setSaving(true);
    setError(null);
    try {
      // the API replaces all the prices of a year: send the complete maps, empty fields removed
      if (heldAtEnd.length > 0) await api.savePrices(year, clean(current));
      if (heldAtStart.length > 0) await api.savePrices(year - 1, clean(before));
      onSaved();
    } catch (e) {
      setError(e);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="card">
      <h2>{t('ivafe.prices')}</h2>
      <p className="muted small">{t('ivafe.pricesIntro')}</p>
      <PriceInputs year={year} tickers={heldAtEnd} values={current} onChange={setCurrent} />
      <PriceInputs year={year - 1} tickers={heldAtStart} values={before} onChange={setBefore} />
      <div className="form-actions">
        <button type="button" className="button" disabled={saving} onClick={save}>
          {saving ? t('common.saving') : t('ivafe.savePrices')}
        </button>
      </div>
      {error != null && <ErrorNotice error={error} />}
    </div>
  );
}

function PriceInputs(props: {
  year: number;
  tickers: string[];
  values: Record<string, string>;
  onChange: (values: Record<string, string>) => void;
}) {
  const { year, tickers, values, onChange } = props;
  if (tickers.length === 0) return null;
  return (
    <div className="form-row wrap">
      {tickers.map((ticker) => (
        <label key={`${year}-${ticker}`}>
          {ticker} · 31/12/{year} (€)
          <input
            className="input num-input"
            inputMode="decimal"
            value={values[ticker] ?? ''}
            onChange={(e) => onChange({ ...values, [ticker]: e.target.value })}
          />
        </label>
      ))}
    </div>
  );
}

const unique = (items: string[]) => [...new Set(items)].sort();

/** Decimal comma accepted; empty fields mean "no price" and are not sent. */
function clean(values: Record<string, string>): Record<string, string> {
  return Object.fromEntries(
    Object.entries(values)
      .map(([ticker, price]) => [ticker, price.trim().replace(',', '.')])
      .filter(([, price]) => price !== ''),
  );
}
