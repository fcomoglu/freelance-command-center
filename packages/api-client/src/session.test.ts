import type { Client } from 'openapi-fetch';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { ApiError } from './problem';
import type { paths } from './schema';
import { type RefreshTokenStore, Session } from './session';

const NOW = Date.parse('2026-11-21T10:00:00Z');
const inMinutes = (minutes: number) => new Date(NOW + minutes * 60_000).toISOString();

type Answer = { status: number; body?: unknown } | 'offline';

/** Answers the auth endpoints the session calls, in order, and records every call. */
function authApi(...answers: Answer[]) {
  const calls: { path: string; body: unknown }[] = [];
  const POST = vi.fn(async (path: string, options?: { body?: unknown }) => {
    calls.push({ path, body: options?.body });
    const answer = answers.shift() ?? 'offline';
    if (answer === 'offline') throw new TypeError('Failed to fetch');
    const response = new Response(null, { status: answer.status });
    return response.ok ? { data: answer.body, response } : { error: answer.body, response };
  });
  return { client: { POST } as unknown as Client<paths>, calls };
}

const session = (accessToken: string, refreshToken?: string) => ({
  status: 200,
  body: { accessToken, accessTokenExpiresAt: inMinutes(15), refreshToken },
});

function memoryStore(token: string | null): RefreshTokenStore & { token: string | null } {
  return {
    token,
    async load() {
      return this.token;
    },
    async save(value) {
      this.token = value;
    },
  };
}

const runDirectly = <T>(task: () => Promise<T>) => task();

beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] });
  vi.setSystemTime(NOW);
});

afterEach(() => {
  vi.useRealTimers();
});

describe('Session', () => {
  it('is signed out without asking the API when no refresh token is stored', async () => {
    const api = authApi();
    const subject = new Session(api.client, memoryStore(null), runDirectly);

    await subject.restore();

    expect(subject.getStatus()).toBe('signedOut');
    expect(api.calls).toEqual([]);
  });

  it('resumes a session with the stored refresh token and keeps the rotated one', async () => {
    const api = authApi(session('access-1', 'refresh-2'));
    const store = memoryStore('refresh-1');
    const subject = new Session(api.client, store, runDirectly);

    await subject.restore();

    expect(api.calls).toEqual([{ path: '/api/v1/auth/refresh', body: { refreshToken: 'refresh-1' } }]);
    expect(subject.getStatus()).toBe('signedIn');
    expect(await subject.getAccessToken()).toBe('access-1');
    expect(store.token).toBe('refresh-2');
  });

  it('refreshes once for parallel requests shortly before the access token expires', async () => {
    const api = authApi(session('access-1'), session('access-2'));
    const subject = new Session(api.client, undefined, runDirectly);
    await subject.restore();

    vi.setSystemTime(NOW + 14.75 * 60_000);
    const tokens = await Promise.all([subject.getAccessToken(), subject.getAccessToken()]);

    expect(tokens).toEqual(['access-2', 'access-2']);
    expect(api.calls.map((call) => call.path)).toEqual(['/api/v1/auth/refresh', '/api/v1/auth/refresh']);
  });

  it('ends the session when the refresh token is rejected', async () => {
    const api = authApi(session('access-1'), { status: 401, body: { code: 'identity.invalid_token' } });
    const store = memoryStore('refresh-1');
    const subject = new Session(api.client, store, runDirectly);
    await subject.restore();

    vi.setSystemTime(NOW + 15 * 60_000);

    expect(await subject.getAccessToken()).toBeUndefined();
    expect(subject.getStatus()).toBe('signedOut');
    expect(store.token).toBeNull();
  });

  it('keeps the session through an outage and retries later', async () => {
    const api = authApi(session('access-1'), 'offline', { status: 503 }, session('access-2'));
    const store = memoryStore('refresh-1');
    const subject = new Session(api.client, store, runDirectly);
    await subject.restore();
    vi.setSystemTime(NOW + 15 * 60_000);

    await subject.getAccessToken();
    await subject.getAccessToken();

    expect(subject.getStatus()).toBe('signedIn');
    expect(store.token).toBe('refresh-1');
    expect(await subject.getAccessToken()).toBe('access-2');
  });

  it('starts signed out, keeping the refresh token, when the API is unreachable on start', async () => {
    const store = memoryStore('refresh-1');
    const subject = new Session(authApi('offline').client, store, runDirectly);

    await subject.restore();

    expect(subject.getStatus()).toBe('signedOut');
    expect(store.token).toBe('refresh-1');
  });

  it('reports a failed sign-in with the problem the API returned', async () => {
    const api = authApi({ status: 401, body: { code: 'identity.invalid_credentials' } });
    const subject = new Session(api.client, undefined, runDirectly);

    const failure = await subject.signIn('ada@example.com', 'wrong').catch((error: unknown) => error);

    expect(failure).toBeInstanceOf(ApiError);
    expect((failure as ApiError).problem).toEqual({ status: 401, code: 'identity.invalid_credentials' });
    expect(subject.getStatus()).toBe('loading');
  });

  it('signs out locally even when the API is unreachable, and tells subscribers', async () => {
    const api = authApi(session('access-1', 'refresh-1'), 'offline');
    const store = memoryStore(null);
    const subject = new Session(api.client, store, runDirectly);
    const changes: string[] = [];
    subject.subscribe(() => changes.push(subject.getStatus()));

    await subject.signIn('ada@example.com', 'correct horse battery');
    await subject.signOut();

    expect(changes).toEqual(['signedIn', 'signedOut']);
    expect(api.calls[1]).toEqual({ path: '/api/v1/auth/sign-out', body: { refreshToken: 'refresh-1' } });
    expect(store.token).toBeNull();
    expect(await subject.getAccessToken()).toBeUndefined();
  });

  it('runs every refresh through the exclusive runner', async () => {
    let runs = 0;
    const runExclusive = <T>(task: () => Promise<T>) => {
      runs++;
      return task();
    };
    const subject = new Session(authApi(session('access-1')).client, undefined, runExclusive);

    await subject.restore();

    expect(runs).toBe(1);
  });
});
