import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  // Mobile pins React; the web app's range includes that version, so the workspace holds one copy. Tests need it: the
  // libraries they load from the root bypass this dedupe, which keeps bundles on one copy if the versions part.
  resolve: { dedupe: ['react', 'react-dom'] },
  server: {
    // Same origin as the API, as when deployed behind a reverse proxy: no CORS, and the refresh cookie just works.
    proxy: { '/api': 'http://localhost:8080' },
  },
  test: { environment: 'jsdom' },
});
