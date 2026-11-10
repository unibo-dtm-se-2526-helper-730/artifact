// Static checks of the frontend: run with `npm run lint`
import js from '@eslint/js';
import prettier from 'eslint-config-prettier';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import { defineConfig, globalIgnores } from 'eslint/config';
import globals from 'globals';
import tseslint from 'typescript-eslint';

export default defineConfig([
  globalIgnores(['dist', 'coverage']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      tseslint.configs.recommended,
      reactRefresh.configs.vite,
      // Formatting is Prettier's job: turn off the rules that would clash with it
      prettier,
    ],
    plugins: { 'react-hooks': reactHooks },
    rules: {
      // The two classic hook rules. The React Compiler rules of the plugin's
      // "recommended" preset (set-state-in-effect, immutability, ...) are left out for now
      'react-hooks/rules-of-hooks': 'error',
      'react-hooks/exhaustive-deps': 'warn',
      // i18n.tsx exports the provider together with the useI18n hook
      'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
    },
    languageOptions: {
      ecmaVersion: 2022,
      globals: globals.browser,
    },
  },
]);
