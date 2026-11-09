import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
// Neuton is bundled with the app (no request to Google at runtime): regular and bold
import '@fontsource/neuton/400.css';
import '@fontsource/neuton/700.css';
import { App } from './App';
import { I18nProvider } from './i18n';
import './styles.css';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <I18nProvider>
      <App />
    </I18nProvider>
  </StrictMode>,
);
