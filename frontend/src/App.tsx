import { useEffect, useState } from 'react';
import { api } from './api';
import { ErrorNotice, Notice } from './components/Notice';
import { useI18n } from './i18n';
import { DeclarationPage } from './pages/DeclarationPage';
import { ImportPage } from './pages/ImportPage';
import { InstrumentsPage } from './pages/InstrumentsPage';
import { IvafePage } from './pages/IvafePage';
import { PortfolioPage } from './pages/PortfolioPage';
import { SummaryPage } from './pages/SummaryPage';
import { TransactionsPage } from './pages/TransactionsPage';
import { useLoad } from './useLoad';

const PAGES = [
  'import',
  'instruments',
  'transactions',
  'summary',
  'declaration',
  'ivafe',
  'portfolio',
] as const;
type Page = (typeof PAGES)[number];

/** Pages computed for one tax year: they need the year menu. */
const NEEDS_YEAR: Page[] = ['transactions', 'summary', 'declaration', 'ivafe'];

/** The page lives in the URL hash (#summary), so reload and back button keep it. */
function pageFromHash(): Page {
  const hash = window.location.hash.slice(1);
  return (PAGES as readonly string[]).includes(hash) ? (hash as Page) : 'import';
}

export function App() {
  const { t, lang, setLang } = useI18n();
  const [page, setPage] = useState<Page>(pageFromHash);
  const [year, setYear] = useState<number | null>(null);

  // bumped after imports, deletions and classifications: the year menu reloads
  const [dataVersion, setDataVersion] = useState(0);
  const onDataChanged = () => setDataVersion((v) => v + 1);
  const years = useLoad(() => api.years(), [dataVersion]);

  useEffect(() => {
    const onHash = () => setPage(pageFromHash());
    window.addEventListener('hashchange', onHash);
    return () => window.removeEventListener('hashchange', onHash);
  }, []);

  // keep the chosen year if it still exists, otherwise the most recent one
  useEffect(() => {
    const available = years.data?.map((y) => y.year) ?? [];
    if (year == null || !available.includes(year)) setYear(available[0] ?? null);
  }, [years.data, year]);

  const go = (target: Page) => {
    window.location.hash = target;
  };

  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="brand">
          <strong>{t('app.title')}</strong>
          <span>{t('app.subtitle')}</span>
        </div>
        <nav>
          {PAGES.map((p) => (
            <a
              key={p}
              href={`#${p}`}
              className={p === page ? 'active' : ''}
              aria-current={p === page ? 'page' : undefined}
            >
              {t(`nav.${p}`)}
            </a>
          ))}
        </nav>
      </aside>

      <div className="main">
        <header className="topbar">
          <label className="year-select">
            {t('year.label')}
            <select
              value={year ?? ''}
              disabled={!years.data?.length}
              onChange={(e) => setYear(Number(e.target.value))}
            >
              {!years.data?.length && <option value="">{t('year.none')}</option>}
              {years.data?.map((y) => (
                <option key={y.year} value={y.year}>
                  {y.year}
                </option>
              ))}
            </select>
          </label>

          <div className="topbar-actions">
            <div className="segmented" role="group" aria-label="Language">
              {(['it', 'en'] as const).map((l) => (
                <button key={l} type="button" aria-pressed={lang === l} onClick={() => setLang(l)}>
                  {l.toUpperCase()}
                </button>
              ))}
            </div>
          </div>
        </header>

        <main className="content">
          {years.error != null && page !== 'import' && <ErrorNotice error={years.error} />}
          {NEEDS_YEAR.includes(page) && year == null ? (
            years.data && <Notice tone="info">{t('year.needed')}</Notice>
          ) : (
            <>
              {page === 'import' && <ImportPage onDataChanged={onDataChanged} />}
              {page === 'instruments' && <InstrumentsPage onDataChanged={onDataChanged} />}
              {page === 'transactions' && year != null && <TransactionsPage year={year} />}
              {page === 'summary' && year != null && (
                <SummaryPage
                  key={`${year}-${lang}`}
                  year={year}
                  importedYears={years.data?.map((y) => y.year) ?? []}
                  onDataChanged={onDataChanged}
                  onGoToInstruments={() => go('instruments')}
                />
              )}
              {page === 'declaration' && year != null && (
                <DeclarationPage
                  key={`${year}-${lang}`}
                  year={year}
                  onGoToInstruments={() => go('instruments')}
                />
              )}
              {page === 'ivafe' && year != null && (
                <IvafePage key={`${year}-${lang}`} year={year} />
              )}
              {page === 'portfolio' && <PortfolioPage key={lang} />}
            </>
          )}
        </main>
      </div>
    </div>
  );
}
