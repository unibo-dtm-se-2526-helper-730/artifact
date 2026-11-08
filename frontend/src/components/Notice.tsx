import type { ReactNode } from 'react';
import { ApiError } from '../api';
import { useI18n } from '../i18n';

type Tone = 'info' | 'success' | 'warning' | 'error';

/** A message box. The tone is also written as text for screen readers, never color alone. */
export function Notice({ tone, children }: { tone: Tone; children: ReactNode }) {
  return (
    <div className={`notice notice-${tone}`} role={tone === 'error' ? 'alert' : 'status'}>
      {children}
    </div>
  );
}

/** Shows an error from the API with the text the backend translated, or a network hint. */
export function ErrorNotice({ error }: { error: unknown }) {
  const { t } = useI18n();
  const text =
    error instanceof ApiError
      ? error.code === 'NETWORK'
        ? t('common.network')
        : error.message
      : String(error);
  return <Notice tone="error">{text}</Notice>;
}
