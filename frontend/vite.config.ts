import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

// In development the browser calls /api on the Vite server, which forwards it to
// Spring Boot: same origin for the browser, so no CORS configuration is needed.
// In production nginx does the same forwarding.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  // Tests live in test/, separated from the sources: run with `npm test`
  test: {
    environment: 'jsdom',
    include: ['test/**/*.test.{ts,tsx}'],
    setupFiles: ['test/setup.ts'],
    coverage: {
      provider: 'v8',
      include: ['src/**/*.{ts,tsx}'],
      exclude: ['src/main.tsx'],
      reporter: ['text', 'html'],
      reportsDirectory: 'coverage',
    },
  },
});
