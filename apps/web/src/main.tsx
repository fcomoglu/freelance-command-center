import { QueryClientProvider } from '@tanstack/react-query';
import { RouterProvider } from '@tanstack/react-router';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import './i18n';
import './index.css';
import { api, queryClient } from './api';
import { router } from './router';

// Signing in or out re-runs the route guards; data of a previous user never outlives their session.
api.session.subscribe(() => {
  if (api.session.getStatus() === 'signedOut') queryClient.clear();
  void router.invalidate();
});

const container = document.getElementById('root');
if (!container) {
  throw new Error('Root element #root not found');
}

createRoot(container).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  </StrictMode>,
);
