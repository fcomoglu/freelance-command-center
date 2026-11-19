import createFetchClient from 'openapi-fetch';
import createQueryClient from 'openapi-react-query';

import type { paths } from './schema';

export type { components, paths } from './schema';

export interface ApiClientOptions {
  baseUrl: string;
  /** Returns the current access token, or `undefined` when signed out. */
  getAccessToken?: () => string | undefined;
}

/** Creates the typed fetch client and its TanStack Query bindings. */
export function createApiClient({ baseUrl, getAccessToken }: ApiClientOptions) {
  const fetchClient = createFetchClient<paths>({ baseUrl });
  if (getAccessToken) {
    fetchClient.use({
      onRequest({ request }) {
        const token = getAccessToken();
        if (token) request.headers.set('Authorization', `Bearer ${token}`);
        return request;
      },
    });
  }
  return { fetchClient, query: createQueryClient(fetchClient) };
}
