import { type components } from '@fcc/api-client';
import { cleanup, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactNode } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { ok, renderScreen } from '../test-support';
import { ConnectedAccountsPage, ConnectionCallbackPage } from './connected-accounts';
import { JobAlertsPage } from './job-alerts';

type PlatformConnection = components['schemas']['PlatformConnection'];

const api = vi.hoisted(() => ({ GET: vi.fn(), POST: vi.fn(), PUT: vi.fn(), DELETE: vi.fn() }));
const navigate = vi.hoisted(() => vi.fn());
vi.mock('../api', () => ({ api: { fetchClient: api } }));
vi.mock('@tanstack/react-router', () => ({
  Link: ({ to, children }: { to: string; children: ReactNode }) => <a href={to}>{children}</a>,
  useNavigate: () => navigate,
}));

const problem = (status: number, body: unknown) => ({
  error: body,
  response: new Response(null, { status }),
});

afterEach(() => {
  cleanup();
  vi.resetAllMocks();
});

describe('JobAlertsPage', () => {
  it('adds normalized keywords and saves them with the chosen platforms', async () => {
    api.GET.mockResolvedValue(ok({ keywords: ['java'], platforms: ['freelancer'] }));
    api.PUT.mockImplementation(async (_path: string, { body }: { body: unknown }) => ok(body));
    await renderScreen(<JobAlertsPage />);

    await userEvent.type(await screen.findByLabelText('Keywords'), '  Spring   Boot ');
    await userEvent.click(screen.getByRole('button', { name: 'Add' }));
    await userEvent.click(screen.getByLabelText('Upwork'));
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));

    expect(api.PUT).toHaveBeenCalledWith('/api/v1/job-filter', {
      body: { keywords: ['java', 'spring boot'], platforms: ['freelancer', 'upwork'] },
    });
    expect(await screen.findByText('Saved.')).toBeTruthy();
  });

  it('does not add a keyword twice and warns when none are left', async () => {
    api.GET.mockResolvedValue(ok({ keywords: ['java'], platforms: [] }));
    await renderScreen(<JobAlertsPage />);

    await userEvent.type(await screen.findByLabelText('Keywords'), 'JAVA');
    expect(screen.getByRole('button', { name: 'Add' }).hasAttribute('disabled')).toBe(true);

    await userEvent.click(screen.getByRole('button', { name: 'Remove java' }));

    expect(screen.getByText(/Add at least one keyword/)).toBeTruthy();
  });
});

describe('ConnectedAccountsPage', () => {
  const freelancer: PlatformConnection = {
    platform: 'freelancer',
    status: 'active',
    accountName: 'ada_dev',
    connectedAt: '2026-11-20T10:00:00Z',
  };
  const upwork: PlatformConnection = { platform: 'upwork', status: 'not_connected' };

  it('shows each connectable platform with its state', async () => {
    api.GET.mockResolvedValue(ok({ items: [freelancer, upwork], nextCursor: null }));
    await renderScreen(<ConnectedAccountsPage connected="freelancer" />);

    const [first, second] = await screen.findAllByRole('listitem');
    expect(within(first).getByText('Connected')).toBeTruthy();
    expect(within(first).getByText(/ada_dev · connected on/)).toBeTruthy();
    expect(within(second).getByRole('button', { name: 'Connect Upwork' })).toBeTruthy();
    expect(screen.getByText(/Freelancer.com is connected/)).toBeTruthy();
  });

  it('explains why connecting is refused before the e-mail address is confirmed', async () => {
    api.GET.mockResolvedValue(ok({ items: [upwork], nextCursor: null }));
    api.POST.mockResolvedValue(problem(403, { code: 'platform.email_not_verified' }));
    await renderScreen(<ConnectedAccountsPage />);

    await userEvent.click(await screen.findByRole('button', { name: 'Connect Upwork' }));

    expect(await screen.findByText(/Confirm your e-mail address first/)).toBeTruthy();
  });

  it('disconnects only after confirmation', async () => {
    api.GET.mockResolvedValue(ok({ items: [freelancer], nextCursor: null }));
    api.DELETE.mockResolvedValue(ok());
    await renderScreen(<ConnectedAccountsPage />);

    await userEvent.click(await screen.findByRole('button', { name: 'Disconnect' }));
    expect(api.DELETE).not.toHaveBeenCalled();
    await userEvent.click(screen.getByRole('button', { name: 'Yes, disconnect' }));

    expect(api.DELETE).toHaveBeenCalledWith('/api/v1/platform-connections/{platform}', {
      params: { path: { platform: 'freelancer' } },
    });
  });
});

describe('ConnectionCallbackPage', () => {
  it('completes the connection with the returned code and state, then shows the accounts', async () => {
    api.POST.mockResolvedValue(ok({ platform: 'freelancer', status: 'active' }));
    await renderScreen(<ConnectionCallbackPage platform="freelancer" code="code-1" state="state-1" />);

    await vi.waitFor(() =>
      expect(navigate).toHaveBeenCalledWith({
        to: '/accounts',
        search: { connected: 'freelancer' },
        replace: true,
      }),
    );
    expect(api.POST).toHaveBeenCalledExactlyOnceWith('/api/v1/platform-connections/{platform}', {
      params: { path: { platform: 'freelancer' } },
      body: { code: 'code-1', state: 'state-1' },
    });
  });

  it('reports a cancelled consent without calling the API', async () => {
    await renderScreen(<ConnectionCallbackPage platform="upwork" />);

    expect(screen.getByText('Connecting Upwork was cancelled.')).toBeTruthy();
    expect(api.POST).not.toHaveBeenCalled();
  });
});
