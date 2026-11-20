import { type components } from '@fcc/api-client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { I18nextProvider } from 'react-i18next';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import i18n from '../i18n';
import { NotificationsPage } from './notifications';

type Notification = components['schemas']['Notification'];

const api = vi.hoisted(() => ({ GET: vi.fn(), POST: vi.fn() }));
vi.mock('../api', () => ({ api: { fetchClient: api } }));

const ok = (data?: unknown) => ({ data, response: new Response(null, { status: data ? 200 : 204 }) });

const notice: Notification = {
  id: 'n-1',
  platform: 'fiverr',
  kind: 'notice',
  title: 'You received a new order',
  body: 'Grace ordered Logo design.',
  url: 'https://www.fiverr.com',
  occurredAt: new Date(Date.now() - 5 * 60_000).toISOString(),
  read: false,
};
const decision: Notification = {
  id: 'n-2',
  platform: 'freelancer',
  kind: 'proposal_awarded',
  title: 'Spring Boot REST API',
  body: '',
  url: 'https://www.freelancer.com/projects/java/spring-boot-rest-api',
  occurredAt: new Date(Date.now() - 3 * 3600_000).toISOString(),
  read: true,
};

function showFeed(...pages: Notification[][]) {
  pages.forEach((items, index) =>
    api.GET.mockResolvedValueOnce(
      ok({ items, nextCursor: index < pages.length - 1 ? `page-${index + 1}` : null }),
    ),
  );
  render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <I18nextProvider i18n={i18n}>
        <NotificationsPage />
      </I18nextProvider>
    </QueryClientProvider>,
  );
}

beforeEach(async () => {
  await i18n.changeLanguage('en');
});

afterEach(() => {
  cleanup();
  vi.resetAllMocks();
});

describe('NotificationsPage', () => {
  it('shows a notice with the platform text and a decision with its translated kind', async () => {
    showFeed([notice, decision]);

    const [first, second] = await screen.findAllByRole('listitem');
    expect(within(first).getByText('You received a new order')).toBeTruthy();
    expect(within(first).getByText('Grace ordered Logo design.')).toBeTruthy();
    expect(within(first).getByText('New')).toBeTruthy();
    expect(within(first).getByText('5 minutes ago')).toBeTruthy();
    expect(within(second).getByText('Your proposal was awarded')).toBeTruthy();
    expect(within(second).getByText('Spring Boot REST API')).toBeTruthy();
    expect(within(second).queryByText('New')).toBeNull();
    expect(within(second).getByRole('link', { name: 'Open on Freelancer.com' }).getAttribute('href')).toBe(
      decision.url,
    );
  });

  it('marks an unread notification read when it is opened on its platform', async () => {
    showFeed([notice]);
    api.POST.mockResolvedValue(ok());
    api.GET.mockResolvedValue(ok({ items: [{ ...notice, read: true }], nextCursor: null }));

    const link = await screen.findByRole('link', { name: 'Open on Fiverr' });
    link.addEventListener('click', (event) => event.preventDefault());
    await userEvent.click(link);

    expect(api.POST).toHaveBeenCalledWith('/api/v1/notifications/{notificationId}/read', {
      params: { path: { notificationId: 'n-1' } },
    });
    await vi.waitFor(() => expect(screen.queryByText('New')).toBeNull());
  });

  it('marks everything read, offering it only while something is unread', async () => {
    showFeed([notice, decision]);
    api.POST.mockResolvedValue(ok());
    api.GET.mockResolvedValue(ok({ items: [{ ...notice, read: true }, decision], nextCursor: null }));

    await userEvent.click(await screen.findByRole('button', { name: 'Mark all as read' }));

    expect(api.POST).toHaveBeenCalledWith('/api/v1/notifications/read');
    await vi.waitFor(() => expect(screen.queryByRole('button', { name: 'Mark all as read' })).toBeNull());
  });

  it('loads older notifications page by page', async () => {
    showFeed([notice], [decision]);

    await userEvent.click(await screen.findByRole('button', { name: 'Show more' }));

    expect(await screen.findByText('Your proposal was awarded')).toBeTruthy();
    expect(api.GET).toHaveBeenLastCalledWith(
      '/api/v1/notifications',
      expect.objectContaining({
        params: { query: { limit: 20, cursor: 'page-1' } },
      }),
    );
    expect(screen.queryByRole('button', { name: 'Show more' })).toBeNull();
  });

  it('explains an empty feed', async () => {
    showFeed([]);

    expect(
      await screen.findByText('No notifications yet. They appear here as your platforms send them.'),
    ).toBeTruthy();
  });

  it('shows the translated error when the feed cannot be loaded', async () => {
    api.GET.mockRejectedValue(new TypeError('Failed to fetch'));
    showFeed();

    expect(
      await screen.findByText('The server cannot be reached. Check your connection and try again.'),
    ).toBeTruthy();
  });
});
