import { ApiError, api } from '../api';
import { ErrorNotice, Notice } from '../components/Notice';
import { currentYear, money } from '../format';
import { useI18n } from '../i18n';
import { useLoad } from '../useLoad';

/** Modello 730 summary: one card per quadro (T, M, W), texts translated by the backend. */
export function DeclarationPage({
  year,
  onGoToInstruments,
}: {
  year: number;
  onGoToInstruments: () => void;
}) {
  const { t, lang } = useI18n();
  const declaration = useLoad(() => api.declaration(year), [year]);
  const unclassified =
    declaration.error instanceof ApiError && declaration.error.code === 'UNCLASSIFIED_INSTRUMENTS';
  const d = declaration.data;
  // until the year is over there are no prices at 31/12: quadro W is filled from January
  const yearClosed = year < currentYear();
  const warnings =
    d?.warnings.filter((w) => yearClosed || w.code !== 'declaration.missing_year_end_price') ?? [];

  return (
    <section>
      {unclassified && (
        <Notice tone="warning">
          <p>{(declaration.error as ApiError).message}</p>
          <button type="button" className="button small" onClick={onGoToInstruments}>
            {t('summary.goInstruments')}
          </button>
        </Notice>
      )}
      {declaration.error != null && !unclassified && <ErrorNotice error={declaration.error} />}

      {d && (
        <>
          <h1>{t('declaration.title', { model: d.model, year: d.year })}</h1>
          <p className="muted small">{t('declaration.disclaimer')}</p>

          {!yearClosed && <Notice tone="info">{t('declaration.yearOpen', { year })}</Notice>}
          {warnings.length > 0 && (
            <Notice tone="warning">
              <ul className="plain">
                {warnings.map((w, i) => (
                  <li key={i}>{w.text}</li>
                ))}
              </ul>
            </Notice>
          )}
          {d.sections.length === 0 && <p className="muted">{t('declaration.empty')}</p>}

          {d.sections.map((section) => (
            <div className="card" key={section.quadro}>
              <h2>{section.title.text}</h2>
              <table className="table">
                <thead>
                  <tr>
                    <th>{t('declaration.rigo')}</th>
                    <th>{t('declaration.what')}</th>
                    <th className="num">{t('declaration.amount')}</th>
                    <th>{t('declaration.detail')}</th>
                    <th className="num">{t('declaration.secondary')}</th>
                  </tr>
                </thead>
                <tbody>
                  {section.items.map((item) => (
                    <tr key={item.key}>
                      <td className="nowrap">{item.rigo}</td>
                      <td>{item.description.text}</td>
                      <td className="num">{money(item.amount, lang)}</td>
                      <td className="small">{item.detail?.text ?? ''}</td>
                      <td className="num">
                        {item.secondary == null ? '' : money(item.secondary, lang)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ))}
        </>
      )}
    </section>
  );
}
