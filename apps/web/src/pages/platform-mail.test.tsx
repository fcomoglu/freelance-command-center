import { type components } from '@fcc/api-client';
import { cleanup, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { ok, renderScreen } from '../test-support';
import { PlatformMailPage } from './platform-mail';

type InboundMail = components['schemas']['InboundMail'];

const api = vi.hoisted(() => ({ GET: vi.fn(), POST: vi.fn() }));
vi.mock('../api', () => ({ api: { fetchClient: api } }));

const fromFiverr: InboundMail = {
  id: 'm-1',
  platform: 'fiverr',
  sender: 'noreply@fiverr.com',
  subject: 'You received a new order',
  status: 'unrecognized',
  receivedAt: new Date(Date.now() - 60_000).toISOString(),
};
const fromElsewhere: InboundMail = {
  id: 'm-2',
  sender: 'newsletter@example.com',
  subject: '',
  status: 'unknown_sender',
  receivedAt: new Date(Date.now() - 3600_000).toISOString(),
};

function serve(mails: InboundMail[]) {
  api.GET.mockImplementation(async (path: string) =>
    path === '/api/v1/mail-inbox'
      ? ok({ address: 'ws-1a2b3c@inbound.example.com' })
      : ok({ items: mails, nextCursor: null }),
  );
}

afterEach(() => {
  cleanup();
  vi.resetAllMocks();
});

describe('PlatformMailPage', () => {
  it('shows the forwarding address and copies it', async () => {
    serve([]);
    // user-event replaces the clipboard with one the test can read.
    const user = userEvent.setup();
    await renderScreen(<PlatformMailPage />);

    expect(await screen.findByText('ws-1a2b3c@inbound.example.com')).toBeTruthy();
    await user.click(screen.getByRole('button', { name: 'Copy' }));

    expect(await navigator.clipboard.readText()).toBe('ws-1a2b3c@inbound.example.com');
    expect(await screen.findByRole('button', { name: 'Copied' })).toBeTruthy();
  });

  it('replaces a leaked address', async () => {
    serve([]);
    api.POST.mockResolvedValue(ok({ address: 'ws-9z8y7x@inbound.example.com' }));
    await renderScreen(<PlatformMailPage />);
    await screen.findByText('ws-1a2b3c@inbound.example.com');

    await userEvent.click(screen.getByRole('button', { name: 'Replace address' }));

    expect(api.POST).toHaveBeenCalledWith('/api/v1/mail-inbox/renewal');
    expect(await screen.findByText('ws-9z8y7x@inbound.example.com')).toBeTruthy();
  });

  it('lists received e-mails with what became of them', async () => {
    serve([fromFiverr, fromElsewhere]);
    await renderScreen(<PlatformMailPage />);

    const list = (await screen.findByText('You received a new order')).closest('ul') as HTMLElement;
    const [first, second] = within(list).getAllByRole('listitem');
    expect(within(first).getByText('Fiverr')).toBeTruthy();
    expect(within(first).getByText('Shown as a notice')).toBeTruthy();
    expect(within(second).getByText('Not from a platform')).toBeTruthy();
    expect(within(second).getByText('(no subject)')).toBeTruthy();
  });
});
