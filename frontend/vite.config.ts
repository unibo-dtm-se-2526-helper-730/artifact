import { defineConfig } from 'vite';
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
});
