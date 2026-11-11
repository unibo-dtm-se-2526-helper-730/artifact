import { Fragment, useState } from 'react';
import { ApiError, api } from '../api';
import { LOSS_CARRY_FORWARD_YEARS, carriedLossWarning } from '../carriedLosses';
import { ErrorNotice, Notice } from '../components/Notice';
import { date, money, percent, quantity } from '../format';
import { useI18n } from '../i18n';
import type { CarriedLoss, TaxEvent, TaxYear } from '../types';
import { useLoad } from '../useLoad';

interface Props {
  year: number;
  /** tax years with imported operations, to check the losses entered by hand */
  importedYears: number[];
  onDataChanged: () => void;
  onGoToInstruments: () => void;
}

/** Outcome of the year, the two income categories, the Zainetto and the audit trail. */
export function SummaryPage({ year, importedYears, onDataChanged, onGoToInstruments }: Props) {
  const { t } = useI18n();
  const tax = useLoad(() => api.tax(year), [year]);

  // 409: the calculation needs classified instruments; show which ones and where to go
  const unclassified =
    tax.error instanceof ApiError && tax.error.code === 'UNCLASSIFIED_INSTRUMENTS';

  return (
    <section>
      <h1>{t('summary.title', { year })}</h1>
      {unclassified && (
        <Notice tone="warning">
          <p>
            {t('summary.unclassified', {
              year,
              tickers: (tax.error as ApiError).problem?.tickers?.join(', ') ?? '',
            })}
          </p>
          <button type="button" className="button small" onClick={onGoToInstruments}>
            {t('summary.goInstruments')}
          </button>
        </Notice>
      )}
      {tax.error != null && !unclassified && <ErrorNotice error={tax.error} />}
      {tax.data && <Result tax={tax.data} />}

      <CarriedLosses
        importedYears={importedYears}
        onSaved={() => {
          tax.reload();
          onDataChanged();
        }}
      />
    </section>
  );
}

function Result({ tax }: { tax: TaxYear }) {
  const { t, lang } = useI18n();
  const rate = Number(tax.rate) * 100;
  const result = Number(tax.economic_result);
  const owed = !tax.below_threshold && Number(tax.total_tax) > 0;

  return (
    <>
      {/* hero: the two numbers the user came for */}
      <div className="hero">
        <div>
          <p className="muted">{t('summary.economic', { year: tax.year })}</p>
          <p className={`hero-value ${result < 0 ? 'negative' : ''}`}>
            {t(result < 0 ? 'summary.lost' : 'summary.gained', {
              amount: money(String(Math.abs(result)), lang),
            })}
          </p>
        </div>
        <div>
          <p className="hero-value">
            {owed
              ? t('summary.owe', { amount: money(tax.payable_tax, lang) })
              : t('summary.nothing')}
          </p>
          {tax.below_threshold && Number(tax.total_tax) > 0 && (
            <p className="muted">
              {t('summary.belowThreshold', {
                amount: money(tax.total_tax, lang),
                threshold: money(tax.payment_threshold, lang),
              })}
            </p>
          )}
        </div>
      </div>

      <div className="cards">
        <div className="card">
          <h2>{t('summary.misc')}</h2>
          <p className="muted small">{t('summary.miscHint')}</p>
          <dl className="figures">
            <dt>{t('summary.taxable')}</dt>
            <dd>{money(tax.miscellaneous_taxable, lang)}</dd>
            <dt>{t('summary.tax', { rate })}</dt>
            <dd>{money(tax.miscellaneous_tax, lang)}</dd>
          </dl>
        </div>
        <div className="card">
          <h2>{t('summary.capital')}</h2>
          <p className="muted small">{t('summary.capitalHint')}</p>
          <dl className="figures">
            <dt>{t('summary.taxable')}</dt>
            <dd>{money(tax.capital_taxable, lang)}</dd>
            <dt>{t('summary.tax', { rate })}</dt>
            <dd>{money(tax.capital_tax, lang)}</dd>
          </dl>
        </div>
        <div className="card">
          <h2>{t('summary.zainetto')}</h2>
          <dl className="figures">
            <dt>{t('summary.zainettoTotal')}</dt>
            <dd>{money(tax.zainetto.closing_total, lang)}</dd>
            <dt>{t('summary.used')}</dt>
            <dd>{money(tax.zainetto.used_total, lang)}</dd>
          </dl>
          {tax.zainetto.closing.length === 0 ? (
            <p className="muted small">{t('summary.zainettoEmpty')}</p>
          ) : (
            <ul className="plain small">
              {tax.zainetto.closing.map((loss) => (
                <li key={loss.year}>
                  {t('summary.expires', {
                    year: loss.year,
                    amount: money(loss.remaining, lang),
                    last: loss.last_usable_year,
                  })}
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>

      <Events events={tax.events} rate={tax.rate} />
    </>
  );
}

/** The audit trail: every taxable event; a sale expands into the lots it consumed. */
function Events({ events, rate }: { events: TaxEvent[]; rate: string }) {
  const { t, lang } = useI18n();
  const [open, setOpen] = useState<string | null>(null);

  if (events.length === 0) return null;
  return (
    <>
      <h2>{t('summary.events')}</h2>
      <p className="muted small">{t('summary.eventsHint')}</p>
      <table className="table">
        <thead>
          <tr>
            <th>{t('transactions.date')}</th>
            <th>{t('instruments.ticker')}</th>
            <th>{t('transactions.type')}</th>
            <th>{t('summary.category')}</th>
            <th className="num">{t('summary.amount')}</th>
            <th className="num">{t('summary.rate')}</th>
            <th className="num">{t('summary.taxableAmount')}</th>
          </tr>
        </thead>
        <tbody>
          {events.map((e) => {
            const expandable = e.consumptions.length > 0;
            const isOpen = open === e.transaction_id;
            return (
              <Fragment key={e.transaction_id}>
                <tr
                  className={expandable ? 'clickable' : ''}
                  onClick={() => expandable && setOpen(isOpen ? null : e.transaction_id)}
                  aria-expanded={expandable ? isOpen : undefined}
                >
                  <td>
                    {expandable && <span className="chevron">{isOpen ? '▾' : '▸'}</span>}
                    {date(e.executed_at, lang)}
                  </td>
                  <td>{e.ticker}</td>
                  <td>{t(`event.${e.type}`)}</td>
                  <td>{t(`cat.${e.category}`)}</td>
                  <td className={`num ${Number(e.amount) < 0 ? 'negative' : ''}`}>
                    {money(e.amount, lang)}
                  </td>
                  {/* a loss is not taxed: it goes into the Zainetto, so no rate */}
                  <td className="num">
                    {e.type === 'CAPITAL_LOSS'
                      ? '-'
                      : percent(Number(rate) * Number(e.weight), lang)}
                  </td>
                  <td className="num">{money(e.taxable_amount, lang)}</td>
                </tr>
                {isOpen && (
                  <tr className="lots">
                    <td colSpan={7}>
                      <table className="table inner">
                        <thead>
                          <tr>
                            <th>{t('lots.acquired')}</th>
                            <th className="num">{t('lots.quantity')}</th>
                            <th className="num">{t('lots.cost')}</th>
                            <th className="num">{t('lots.proceeds')}</th>
                            <th className="num">{t('lots.gain')}</th>
                          </tr>
                        </thead>
                        <tbody>
                          {e.consumptions.map((c, i) => (
                            <tr key={i}>
                              <td>{date(c.acquired_at, lang)}</td>
                              <td className="num">{quantity(c.quantity, lang)}</td>
                              <td className="num">{money(c.cost, lang)}</td>
                              <td className="num">{money(c.proceeds, lang)}</td>
                              <td className={`num ${Number(c.gain) < 0 ? 'negative' : ''}`}>
                                {money(c.gain, lang)}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </td>
                  </tr>
                )}
              </Fragment>
            );
          })}
        </tbody>
      </table>
    </>
  );
}

/**
 * Losses from years before the first import: the whole list is saved at once. A loss of a
 * year already imported, or one expired before it, gets a warning but can still be saved.
 */
function CarriedLosses({
  importedYears,
  onSaved,
}: {
  importedYears: number[];
  onSaved: () => void;
}) {
  const { t } = useI18n();
  const loaded = useLoad(() => api.carriedLosses(), []);
  const [rows, setRows] = useState<CarriedLoss[] | null>(null);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<'saved' | null>(null);
  const [error, setError] = useState<unknown>(null);

  const current = rows ?? loaded.data ?? [];
  const update = (i: number, change: Partial<CarriedLoss>) =>
    setRows(current.map((row, j) => (j === i ? { ...row, ...change } : row)));

  async function save() {
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      setRows(
        await api.saveCarriedLosses(
          current.map((r) => ({ ...r, amount: r.amount.replace(',', '.') })),
        ),
      );
      setMessage('saved');
      onSaved();
    } catch (e) {
      setError(e);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="card carried">
      <h2>{t('carried.title')}</h2>
      <p className="muted small">{t('carried.intro')}</p>
      {current.map((row, i) => (
        <Fragment key={i}>
          <div className="form-row">
            <label>
              {t('carried.year')}
              <input
                className="input num-input"
                type="number"
                value={row.loss_year}
                onChange={(e) => update(i, { loss_year: Number(e.target.value) })}
              />
            </label>
            <label>
              {t('carried.amount')}
              <input
                className="input num-input"
                inputMode="decimal"
                value={row.amount}
                onChange={(e) => update(i, { amount: e.target.value })}
              />
            </label>
            <button
              type="button"
              className="button secondary small"
              onClick={() => setRows(current.filter((_, j) => j !== i))}
            >
              {t('carried.remove')}
            </button>
          </div>
          <LossWarning lossYear={row.loss_year} importedYears={importedYears} />
        </Fragment>
      ))}
      <div className="form-actions">
        <button
          type="button"
          className="button secondary"
          onClick={() =>
            setRows([...current, { loss_year: new Date().getFullYear() - 1, amount: '' }])
          }
        >
          {t('carried.add')}
        </button>
        <button type="button" className="button" disabled={saving} onClick={save}>
          {saving ? t('common.saving') : t('common.save')}
        </button>
      </div>
      {message && <Notice tone="success">{t('common.saved')}</Notice>}
      {error != null && <ErrorNotice error={error} />}
    </div>
  );
}

/** The warning of one loss entered by hand, if any. */
function LossWarning({ lossYear, importedYears }: { lossYear: number; importedYears: number[] }) {
  const { t } = useI18n();
  const warning = carriedLossWarning(lossYear, importedYears);
  if (warning === 'imported') {
    return <Notice tone="warning">{t('carried.warnImported', { year: lossYear })}</Notice>;
  }
  if (warning === 'expired') {
    return (
      <Notice tone="warning">
        {t('carried.warnExpired', {
          year: lossYear,
          last: lossYear + LOSS_CARRY_FORWARD_YEARS,
          first: Math.min(...importedYears),
        })}
      </Notice>
    );
  }
  return null;
}
