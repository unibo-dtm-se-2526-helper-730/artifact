import { api } from '../api';
import { ErrorNotice } from '../components/Notice';
import { dateTime, money, quantity } from '../format';
import { useI18n } from '../i18n';
import type { OperationType } from '../types';
import { useLoad } from '../useLoad';

/** Only the operations that matter for taxes; cash movements are stored but not shown. */
const SHOWN: OperationType[] = ['BUY', 'SELL', 'DIVIDEND', 'COUPON'];

export function TransactionsPage({ year }: { year: number }) {
  const { t, lang } = useI18n();
  const data = useLoad(() => Promise.all([api.transactions(year), api.instruments()]), [year]);

  const [transactions, instruments] = data.data ?? [[], []];
  const names = new Map(instruments.map((i) => [i.ticker, i.name]));
  const rows = transactions.filter((tx) => SHOWN.includes(tx.type));

  return (
    <section>
      <h1>{t('transactions.title', { year })}</h1>
      {data.error != null && <ErrorNotice error={data.error} />}
      {data.data && rows.length === 0 && <p className="muted">{t('common.empty')}</p>}
      {rows.length > 0 && (
        <table className="table">
          <thead>
            <tr>
              <th>{t('transactions.date')}</th>
              <th>{t('transactions.type')}</th>
              <th>{t('instruments.ticker')}</th>
              <th className="num">{t('transactions.quantity')}</th>
              <th className="num">{t('transactions.price')}</th>
              <th className="num">{t('transactions.total')}</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((tx) => (
              <tr key={tx.id}>
                <td>{dateTime(tx.executed_at, lang)}</td>
                <td>
                  <span className={`badge badge-op-${tx.type.toLowerCase()}`}>{t(`op.${tx.type as 'BUY'}`)}</span>
                </td>
                {/* the full instrument name appears on hover */}
                <td title={names.get(tx.ticker ?? '') ?? undefined}>{tx.ticker}</td>
                <td className="num">{quantity(tx.quantity, lang)}</td>
                <td className="num">{money(tx.price_per_share, lang)}</td>
                <td className="num">{money(tx.total_amount, lang)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
