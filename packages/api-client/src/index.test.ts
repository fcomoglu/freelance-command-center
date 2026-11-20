import { afterEach, describe, expect, it, vi } from 'vitest';

import { createApiClient } from './index';

const json = (status: number, body: unknown) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });

/** Routes requests by path; records the Authorization header of every request. */
function stubFetch(routes: Record<string, () => Response>) {
  const seen: { path: string; authorization: string | null }[] = [];
  vi.stubGlobal(
    'fetch',
    vi.fn(async (request: Request) => {
      const path = new URL(request.url).pathname;
      seen.push({ path, authorization: request.headers.get('Authorization') });
      return routes[path]?.() ?? json(404, { code: 'not_found' });
    }),
  );
  return seen;
}

const signedIn = () =>
  json(200, {
    accessToken: 'access-1',
    accessTokenExpiresAt: new Date(Date.now() + 15 * 60_000).toISOString(),
  });

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('createApiClient', () => {
  it('sends the access token with API requests but not with auth requests', async () => {
    const seen = stubFetch({
      '/api/v1/auth/sign-in': signedIn,
      '/api/v1/me': () => json(200, {}),
    });
    const api = createApiClient({ baseUrl: 'http://api.test' });

    await api.session.signIn('ada@example.com', 'correct horse battery');
    await api.fetchClient.GET('/api/v1/me');

    expect(seen).toEqual([
      { path: '/api/v1/auth/sign-in', authorization: null },
      { path: '/api/v1/me', authorization: 'Bearer access-1' },
    ]);
  });

  it('ends the session when the API rejects the access token', async () => {
    stubFetch({
      '/api/v1/auth/sign-in': signedIn,
      '/api/v1/me': () => json(401, { code: 'identity.unauthenticated' }),
    });
    const api = createApiClient({ baseUrl: 'http://api.test' });
    await api.session.signIn('ada@example.com', 'correct horse battery');

    await api.fetchClient.GET('/api/v1/me');

    expect(api.session.getStatus()).toBe('signedOut');
  });

  it('keeps the session when an auth endpoint answers 401, e.g. a spent e-mail link', async () => {
    stubFetch({
      '/api/v1/auth/sign-in': signedIn,
      '/api/v1/auth/email-verification': () => json(401, { code: 'identity.invalid_token' }),
    });
    const api = createApiClient({ baseUrl: 'http://api.test' });
    await api.session.signIn('ada@example.com', 'correct horse battery');

    await api.fetchClient.POST('/api/v1/auth/email-verification', { body: { token: 'spent' } });

    expect(api.session.getStatus()).toBe('signedIn');
  });
});
