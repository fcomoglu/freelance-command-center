import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render } from '@testing-library/react-native';
import type { ReactElement } from 'react';

import i18n from '@/i18n';

/** A successful API result as openapi-fetch returns it; `204 No Content` without data. */
export const ok = (data?: unknown) =>
  ({ data, response: new Response(null, { status: data ? 200 : 204 }) }) as never;

/**
 * Renders a screen with the app's query client setup, in English and without retries. Garbage collection timers
 * are off, since they would keep Jest running after the last test.
 */
export async function renderScreen(screen: ReactElement) {
  await i18n.changeLanguage('en');
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: Infinity }, mutations: { gcTime: Infinity } },
  });
  return render(<QueryClientProvider client={client}>{screen}</QueryClientProvider>);
}
