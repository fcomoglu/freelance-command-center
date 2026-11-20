import { createApiClient } from '@fcc/api-client';
import { QueryClient } from '@tanstack/react-query';

/**
 * The API is reached through the same origin (Vite proxy locally, reverse proxy when deployed), so the refresh
 * token travels as an HttpOnly cookie and no refresh token store is needed.
 */
export const api = createApiClient({
  baseUrl: import.meta.env.VITE_API_URL ?? '',
  // Tabs share the cookie; refreshing one at a time keeps a rotated token from being replayed by another tab.
  runExclusive: (task) =>
    'locks' in navigator ? navigator.locks.request('fcc-token-refresh', () => task()) : task(),
});

export const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false } },
});
