import type { Client } from 'openapi-fetch';

import { ApiError, toProblem } from './problem';
import type { components, paths } from './schema';

type SessionResponse = components['schemas']['SessionResponse'];

/**
 * Keeps the refresh token for clients that receive it in the response body (mobile, extension). Browsers on an
 * allowed web origin get it as an HttpOnly cookie instead and pass no store.
 */
export interface RefreshTokenStore {
  load(): Promise<string | null>;
  save(token: string | null): Promise<void>;
}

/** Runs `task` exclusively across every instance of the client, e.g. all browser tabs via the Web Locks API. */
export type ExclusiveRunner = <T>(task: () => Promise<T>) => Promise<T>;

export type SessionStatus = 'loading' | 'signedIn' | 'signedOut';

/** Refresh this long before the access token expires, so requests never carry an expired token. */
const REFRESH_MARGIN_MS = 30_000;

/**
 * Holds the access token in memory and keeps it fresh. Refreshes are single-flight: refresh tokens work once, so
 * two parallel refreshes would look like a replay and end the session on every device.
 */
export class Session {
  private status: SessionStatus = 'loading';
  private accessToken: string | undefined;
  private accessTokenExpiresAt = 0;
  private refreshing: Promise<boolean> | undefined;
  private restoring: Promise<void> | undefined;
  private readonly listeners = new Set<() => void>();
  private readonly authClient: Client<paths>;
  private readonly store: RefreshTokenStore | undefined;
  private readonly runExclusive: ExclusiveRunner;

  constructor(
    authClient: Client<paths>,
    store: RefreshTokenStore | undefined,
    runExclusive: ExclusiveRunner,
  ) {
    this.authClient = authClient;
    this.store = store;
    this.runExclusive = runExclusive;
  }

  getStatus = (): SessionStatus => this.status;

  subscribe = (listener: () => void): (() => void) => {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  };

  /** Resumes an earlier session if a refresh token is available. Safe to call repeatedly. */
  restore(): Promise<void> {
    this.restoring ??= (async () => {
      if (this.store && !(await this.store.load())) {
        this.setStatus('signedOut');
        return;
      }
      await this.refresh();
    })();
    return this.restoring;
  }

  async signIn(email: string, password: string): Promise<void> {
    const { data, error, response } = await this.call(() =>
      this.authClient.POST('/api/v1/auth/sign-in', { body: { email, password }, credentials: 'include' }),
    );
    if (!data) throw new ApiError(toProblem(error, response));
    await this.apply(data);
  }

  /** Ends the session on this device. Always succeeds locally, even when the API is unreachable. */
  async signOut(): Promise<void> {
    const refreshToken = (await this.store?.load()) ?? undefined;
    await this.call(() =>
      this.authClient.POST('/api/v1/auth/sign-out', {
        body: refreshToken ? { refreshToken } : undefined,
        credentials: 'include',
      }),
    ).catch(() => undefined);
    await this.clear();
  }

  /** A valid access token, refreshed first when it is about to expire; `undefined` when signed out. */
  async getAccessToken(): Promise<string | undefined> {
    if (this.status !== 'signedIn') return undefined;
    if (Date.now() > this.accessTokenExpiresAt - REFRESH_MARGIN_MS) await this.refresh();
    return this.accessToken;
  }

  /** Called when the API rejects the access token, e.g. after the account was signed out elsewhere. */
  async handleUnauthorized(): Promise<void> {
    await this.clear();
  }

  private refresh(): Promise<boolean> {
    this.refreshing ??= this.runExclusive(async () => {
      const refreshToken = (await this.store?.load()) ?? undefined;
      const { data, response } = await this.call(() =>
        this.authClient.POST('/api/v1/auth/refresh', {
          body: refreshToken ? { refreshToken } : undefined,
          credentials: 'include',
        }),
      );
      if (data) {
        await this.apply(data);
        return true;
      }
      // Only a rejected refresh token ends the session; outages and server errors keep it for a later retry.
      if (response?.status === 401) await this.clear();
      else if (this.status === 'loading') this.setStatus('signedOut');
      return false;
    }).finally(() => {
      this.refreshing = undefined;
    });
    return this.refreshing;
  }

  private async apply(session: SessionResponse): Promise<void> {
    this.accessToken = session.accessToken;
    this.accessTokenExpiresAt = Date.parse(session.accessTokenExpiresAt);
    if (this.store && session.refreshToken) await this.store.save(session.refreshToken);
    this.setStatus('signedIn');
  }

  private async clear(): Promise<void> {
    this.accessToken = undefined;
    this.accessTokenExpiresAt = 0;
    await this.store?.save(null);
    this.setStatus('signedOut');
  }

  /** Network failures come back as a result without data or response instead of an exception. */
  private async call<T extends { data?: unknown; error?: unknown; response: Response }>(
    request: () => Promise<T>,
  ): Promise<{ data?: T['data']; error?: unknown; response?: Response }> {
    try {
      return await request();
    } catch {
      return {};
    }
  }

  private setStatus(status: SessionStatus): void {
    if (this.status === status) return;
    this.status = status;
    this.listeners.forEach((listener) => listener());
  }
}
