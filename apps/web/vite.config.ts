import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  // Mobile pins its own React version; packages hoisted to the root must still use the web app's single copy.
  resolve: { dedupe: ['react', 'react-dom'] },
  server: {
    // Same origin as the API, as when deployed behind a reverse proxy: no CORS, and the refresh cookie just works.
    proxy: { '/api': 'http://localhost:8080' },
  },
});
