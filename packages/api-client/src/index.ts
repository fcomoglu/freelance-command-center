import createFetchClient from 'openapi-fetch';
import createQueryClient from 'openapi-react-query';

import type { paths } from './schema';
import { type ExclusiveRunner, type RefreshTokenStore, Session } from './session';

export type { components, paths } from './schema';
export { ApiError, toProblem, unwrap, type ApiProblem } from './problem';
export { Session, type ExclusiveRunner, type RefreshTokenStore, type SessionStatus } from './session';

export interface ApiClientOptions {
  /** API origin, e.g. `http://localhost:8080`; empty when the API is served from the same origin. */
  baseUrl: string;
  /** Where the refresh token is kept; omit in browsers, which use the HttpOnly cookie. */
  refreshTokenStore?: RefreshTokenStore;
  /** Serializes token refreshes across tabs or processes; defaults to no coordination. */
  runExclusive?: ExclusiveRunner;
}

const AUTH_PATH = '/api/v1/auth/';

const isAuthRequest = (request: Request) =>
  new URL(request.url, 'http://base').pathname.startsWith(AUTH_PATH);

/**
 * Creates the typed fetch client, its TanStack Query bindings and the session that authenticates every request.
 * The session uses its own client so token handling never passes through these middlewares.
 */
export function createApiClient({
  baseUrl,
  refreshTokenStore,
  runExclusive = (task) => task(),
}: ApiClientOptions) {
  const session = new Session(createFetchClient<paths>({ baseUrl }), refreshTokenStore, runExclusive);
  const fetchClient = createFetchClient<paths>({ baseUrl });
  // The middlewares return nothing: returning the same object makes openapi-fetch check it with `instanceof
  // Request/Response`, which fails in React Native, where fetch returns objects of a different class.
  fetchClient.use({
    async onRequest({ request }) {
      if (isAuthRequest(request)) return;
      const token = await session.getAccessToken();
      if (token) request.headers.set('Authorization', `Bearer ${token}`);
    },
    async onResponse({ request, response }) {
      // Auth endpoints answer 401 for wrong passwords and spent links; only a rejected access token ends the session.
      if (response.status === 401 && !isAuthRequest(request)) await session.handleUnauthorized();
    },
  });
  return { fetchClient, query: createQueryClient(fetchClient), session };
}

export type ApiClient = ReturnType<typeof createApiClient>;
