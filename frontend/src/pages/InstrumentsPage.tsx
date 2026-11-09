import { useState } from 'react';
import { api } from '../api';
import { ErrorNotice, Notice } from '../components/Notice';
import { fractionToPercent, percentToFraction } from '../format';
import { useI18n } from '../i18n';
import {
  ASSET_CLASSES,
  allowsForeignWithholding,
  allowsGovernmentBondShare,
  type AssetClass,
  type Instrument,
} from '../types';
import { useLoad } from '../useLoad';

/** One editable row per ticker; each row is saved on its own. */
export function InstrumentsPage({ onDataChanged }: { onDataChanged: () => void }) {
  const { t } = useI18n();
  const instruments = useLoad(() => api.instruments(), []);

  return (
    <section>
      <h1>{t('instruments.title')}</h1>
      <p className="muted">{t('instruments.intro')}</p>
      {instruments.error != null && <ErrorNotice error={instruments.error} />}
      {instruments.data?.length === 0 && <Notice tone="info">{t('instruments.empty')}</Notice>}
      {instruments.data && instruments.data.length > 0 && (
        <div className="table-scroll">
          <table className="table">
            <thead>
              <tr>
                <th>{t('instruments.ticker')}</th>
                <th>{t('instruments.name')}</th>
                <th>{t('instruments.isin')}</th>
                <th>{t('instruments.class')}</th>
                <th>{t('instruments.govShare')}</th>
                <th>{t('instruments.withholding')}</th>
                <th>{t('instruments.status')}</th>
                <th aria-label={t('common.save')} />
              </tr>
            </thead>
            <tbody>
              {instruments.data.map((instrument) => (
                <InstrumentRow key={instrument.ticker} instrument={instrument} onSaved={onDataChanged} />
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function InstrumentRow({ instrument, onSaved }: { instrument: Instrument; onSaved: () => void }) {
  const { t } = useI18n();
  const [saved, setSaved] = useState(instrument);
  const [name, setName] = useState(instrument.name ?? '');
  const [isin, setIsin] = useState(instrument.isin ?? '');
  const [assetClass, setAssetClass] = useState<AssetClass | null>(instrument.asset_class);
  const [govShare, setGovShare] = useState(fractionToPercent(instrument.government_bond_share));
  const [withholding, setWithholding] = useState(fractionToPercent(instrument.foreign_withholding_rate));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<unknown>(null);

  // the optional fields exist only for some asset classes, as in the backend
  const govAllowed = allowsGovernmentBondShare(assetClass);
  const withholdingAllowed = allowsForeignWithholding(assetClass);

  async function save() {
    setSaving(true);
    setError(null);
    try {
      const result = await api.saveInstrument(instrument.ticker, {
        name: name.trim() || null,
        isin: isin.trim().toUpperCase() || null,
        asset_class: assetClass,
        government_bond_share: govAllowed ? percentToFraction(govShare) : null,
        foreign_withholding_rate: withholdingAllowed ? percentToFraction(withholding) : null,
      });
      setSaved(result);
      onSaved(); // the tax years may now be computable
    } catch (e) {
      setError(e);
    } finally {
      setSaving(false);
    }
  }

  return (
    <>
      <tr>
        <th scope="row">{instrument.ticker}</th>
        <td>
          <input className="input" value={name} onChange={(e) => setName(e.target.value)} />
        </td>
        <td>
          <input className="input" value={isin} maxLength={12} onChange={(e) => setIsin(e.target.value)} />
        </td>
        <td>
          <select
            className="input"
            value={assetClass ?? ''}
            onChange={(e) => setAssetClass((e.target.value || null) as AssetClass | null)}
          >
            <option value="">{t('instruments.unclassified')}</option>
            {ASSET_CLASSES.map((c) => (
              <option key={c} value={c}>
                {t(`class.${c}`)}
              </option>
            ))}
          </select>
        </td>
        <td>
          <input
            className="input num-input"
            inputMode="decimal"
            disabled={!govAllowed}
            value={govAllowed ? govShare : ''}
            onChange={(e) => setGovShare(e.target.value)}
          />
        </td>
        <td>
          <input
            className="input num-input"
            inputMode="decimal"
            disabled={!withholdingAllowed}
            value={withholdingAllowed ? withholding : ''}
            onChange={(e) => setWithholding(e.target.value)}
          />
        </td>
        <td>
          <span className={`badge ${saved.classified ? 'badge-ok' : 'badge-warning'}`}>
            {t(saved.classified ? 'instruments.ok' : 'instruments.todo')}
          </span>
        </td>
        <td>
          <button type="button" className="button small" disabled={saving} onClick={save}>
            {saving ? t('common.saving') : t('common.save')}
          </button>
        </td>
      </tr>
      {error != null && (
        <tr>
          <td colSpan={8}>
            <ErrorNotice error={error} />
          </td>
        </tr>
      )}
    </>
  );
}
