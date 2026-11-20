import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render } from '@testing-library/react';
import type { ReactElement } from 'react';
import { I18nextProvider } from 'react-i18next';

import i18n from './i18n';

/** A successful API result as openapi-fetch returns it; `204 No Content` without data. */
export const ok = (data?: unknown) => ({ data, response: new Response(null, { status: data ? 200 : 204 }) });

/** Renders a screen with the providers of the app, in English and without query retries. */
export async function renderScreen(screen: ReactElement) {
  await i18n.changeLanguage('en');
  return render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <I18nextProvider i18n={i18n}>{screen}</I18nextProvider>
    </QueryClientProvider>,
  );
}
