import { useRef, useState, type DragEvent } from 'react';
import { api } from '../api';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { ErrorNotice, Notice } from '../components/Notice';
import { dateTime } from '../format';
import { useI18n } from '../i18n';
import type { DeleteResponse, ImportResponse } from '../types';
import { useLoad } from '../useLoad';

/** Upload of the CSV, report of the last import, history and "delete everything". */
export function ImportPage({ onDataChanged }: { onDataChanged: () => void }) {
  const { t, lang } = useI18n();
  const history = useLoad(() => api.imports(), []);
  const input = useRef<HTMLInputElement>(null);

  const [dragging, setDragging] = useState(false);
  const [uploading, setUploading] = useState<string | null>(null);
  const [result, setResult] = useState<ImportResponse | null>(null);
  const [error, setError] = useState<unknown>(null);

  const [confirming, setConfirming] = useState(false);
  const [alsoInstruments, setAlsoInstruments] = useState(false);
  const [deleted, setDeleted] = useState<DeleteResponse | null>(null);

  async function upload(file: File) {
    setUploading(file.name);
    setResult(null);
    setError(null);
    setDeleted(null);
    try {
      setResult(await api.upload(file));
      history.reload();
      onDataChanged(); // new years may have appeared
    } catch (e) {
      setError(e);
    } finally {
      setUploading(null);
    }
  }

  function onDrop(event: DragEvent) {
    event.preventDefault();
    setDragging(false);
    const file = event.dataTransfer.files[0];
    if (file) void upload(file);
  }

  async function deleteAll() {
    setConfirming(false);
    setError(null);
    setResult(null);
    try {
      setDeleted(await api.deleteAll(alsoInstruments));
      history.reload();
      onDataChanged();
    } catch (e) {
      setError(e);
    }
  }

  return (
    <section>
      <h1>{t('import.title')}</h1>

      {/* drop zone: also a button, so it works with the keyboard */}
      <div
        className={`dropzone ${dragging ? 'dragging' : ''}`}
        onDragOver={(e) => {
          e.preventDefault();
          setDragging(true);
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={onDrop}
      >
        <p>
          {t('import.drop')}{' '}
          <button type="button" className="link-button" onClick={() => input.current?.click()}>
            {t('import.choose')}
          </button>
        </p>
        <input
          ref={input}
          type="file"
          accept=".csv,text/csv"
          hidden
          onChange={(e) => {
            const file = e.target.files?.[0];
            if (file) void upload(file);
            e.target.value = ''; // the same file can be chosen again
          }}
        />
      </div>

      {uploading && <Notice tone="info">{t('import.uploading', { name: uploading })}</Notice>}
      {error != null && <ErrorNotice error={error} />}
      {deleted && <Notice tone="success">{t('import.deleted', { ...deleted })}</Notice>}

      {result && (
        <>
          <Notice tone={result.errors > 0 ? 'warning' : 'success'}>
            {t('import.result', { imported: result.imported, duplicates: result.duplicates, errors: result.errors })}
          </Notice>
          {result.issues.length > 0 && (
            <table className="table">
              <thead>
                <tr>
                  <th className="num">{t('import.row')}</th>
                  <th>{t('import.severity')}</th>
                  <th>{t('import.message')}</th>
                </tr>
              </thead>
              <tbody>
                {result.issues.map((issue, i) => (
                  <tr key={i}>
                    <td className="num">{issue.file_row}</td>
                    <td>
                      <span className={`badge badge-${issue.severity.toLowerCase()}`}>
                        {t(issue.severity === 'ERROR' ? 'import.ERROR' : 'import.WARNING')}
                      </span>
                    </td>
                    <td>{issue.message}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}

      <h2>{t('import.history')}</h2>
      {history.error != null && <ErrorNotice error={history.error} />}
      {history.data && history.data.length === 0 && <p className="muted">{t('common.empty')}</p>}
      {history.data && history.data.length > 0 && (
        <table className="table">
          <thead>
            <tr>
              <th>{t('import.date')}</th>
              <th>{t('import.file')}</th>
              <th className="num">{t('import.imported')}</th>
              <th className="num">{t('import.duplicates')}</th>
              <th className="num">{t('import.errors')}</th>
            </tr>
          </thead>
          <tbody>
            {history.data.map((b) => (
              <tr key={b.id}>
                <td>{dateTime(b.imported_at, lang)}</td>
                <td>{b.filename}</td>
                <td className="num">{b.imported}</td>
                <td className="num">{b.duplicates}</td>
                <td className="num">{b.errors}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <div className="danger-zone">
        <div>
          <strong>{t('import.danger')}</strong>
          <p className="muted">{t('import.dangerText')}</p>
        </div>
        <button type="button" className="button danger" onClick={() => setConfirming(true)}>
          {t('import.danger')}
        </button>
      </div>

      {confirming && (
        <ConfirmDialog
          title={t('import.confirmTitle')}
          confirmLabel={t('import.delete')}
          danger
          onConfirm={deleteAll}
          onCancel={() => setConfirming(false)}
        >
          <p>{t('import.confirmText')}</p>
          <label className="checkbox">
            <input type="checkbox" checked={alsoInstruments} onChange={(e) => setAlsoInstruments(e.target.checked)} />
            {t('import.alsoInstruments')}
          </label>
        </ConfirmDialog>
      )}
    </section>
  );
}
