import { useState } from 'react';
import { api } from '../api';
import { DonutChart, type Slice } from '../components/DonutChart';
import { ErrorNotice } from '../components/Notice';
import { money, percent, quantity } from '../format';
import { useI18n } from '../i18n';
import { useLoad } from '../useLoad';

/** Six categorical slots (validated for color-vision deficiencies); further positions go into "Other". */
const MAX_SLICES = 6;
const SERIES = [
  'var(--series-1)',
  'var(--series-2)',
  'var(--series-3)',
  'var(--series-4)',
  'var(--series-5)',
  'var(--series-6)',
];

export function PortfolioPage() {
  const { t, lang } = useI18n();
  const portfolio = useLoad(() => api.portfolio(), []);
  const [view, setView] = useState<'ticker' | 'class'>('ticker');
  const p = portfolio.data;

  // positions arrive sorted by cost: colors follow that stable order
  const items = p
    ? view === 'ticker'
      ? p.positions.map((x) => ({
          key: x.ticker,
          label: x.ticker,
          weight: Number(x.weight),
          cost: x.cost,
        }))
      : p.by_asset_class.map((c) => ({
          key: c.asset_class ?? 'null',
          label: t(`class.${c.asset_class ?? 'null'}`),
          weight: Number(c.weight),
          cost: c.cost,
        }))
    : [];

  const shown = items.length > MAX_SLICES ? items.slice(0, MAX_SLICES - 1) : items;
  const rest = items.length > MAX_SLICES ? items.slice(MAX_SLICES - 1) : [];
  const slices: Slice[] = shown.map((item, i) => ({
    key: item.key,
    label: item.label,
    weight: item.weight,
    value: money(item.cost, lang),
    color: SERIES[i],
  }));
  if (rest.length > 0) {
    const restCost = rest.reduce((sum, item) => sum + Number(item.cost), 0);
    slices.push({
      key: 'other',
      label: t('portfolio.other'),
      weight: rest.reduce((sum, item) => sum + item.weight, 0),
      value: money(String(restCost), lang),
      color: 'var(--series-other)',
    });
  }

  return (
    <section>
      <h1>{t('portfolio.title')}</h1>
      <p className="muted">{t('portfolio.intro')}</p>
      {portfolio.error != null && <ErrorNotice error={portfolio.error} />}
      {p && p.positions.length === 0 && <p className="muted">{t('portfolio.empty')}</p>}

      {p && p.positions.length > 0 && (
        <>
          <div className="segmented" role="group">
            <button
              type="button"
              aria-pressed={view === 'ticker'}
              onClick={() => setView('ticker')}
            >
              {t('portfolio.byTicker')}
            </button>
            <button type="button" aria-pressed={view === 'class'} onClick={() => setView('class')}>
              {t('portfolio.byClass')}
            </button>
          </div>

          <div className="card chart-card">
            <DonutChart
              slices={slices}
              centerLabel={t('portfolio.total')}
              centerValue={money(p.total_cost, lang)}
              formatWeight={(w) => percent(w, lang, 1)}
            />
          </div>

          {/* the table carries the exact weights; the chart enlarges very small slices */}
          <table className="table">
            <thead>
              <tr>
                <th>{t('instruments.ticker')}</th>
                <th>{t('instruments.name')}</th>
                <th>{t('instruments.class')}</th>
                <th className="num">{t('portfolio.quantity')}</th>
                <th className="num">{t('portfolio.average')}</th>
                <th className="num">{t('portfolio.cost')}</th>
                <th className="num">{t('portfolio.weight')}</th>
              </tr>
            </thead>
            <tbody>
              {p.positions.map((x) => (
                <tr key={x.ticker}>
                  <td>{x.ticker}</td>
                  <td>{x.name ?? ''}</td>
                  <td>{t(`class.${x.asset_class ?? 'null'}`)}</td>
                  <td className="num">{quantity(x.quantity, lang)}</td>
                  <td className="num">{money(x.average_cost, lang)}</td>
                  <td className="num">{money(x.cost, lang)}</td>
                  <td className="num">{percent(x.weight, lang, 2)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}
    </section>
  );
}
