import type { components } from '@fcc/api-client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react-native';
import { Linking } from 'react-native';

import { api } from '@/api';
import NotificationsScreen from '@/app/(app)/notifications';
import i18n from '@/i18n';

type Notification = components['schemas']['Notification'];

jest.mock('@/api', () => ({ api: { fetchClient: { GET: jest.fn(), POST: jest.fn() } } }));
jest.mock(
  'react-native-safe-area-context',
  () => jest.requireActual('react-native-safe-area-context/jest/mock').default,
);

const fetchClient = jest.mocked(api.fetchClient);
const ok = (data?: unknown) =>
  ({ data, response: new Response(null, { status: data ? 200 : 204 }) }) as never;

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
  kind: 'proposal_rejected',
  title: 'Spring Boot REST API',
  body: '',
  url: 'https://www.freelancer.com/projects/java/spring-boot-rest-api',
  occurredAt: new Date(Date.now() - 3 * 3600_000).toISOString(),
  read: true,
};

// Garbage collection timers would keep Jest running after the last test.
const testQueryClient = () =>
  new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: Infinity }, mutations: { gcTime: Infinity } },
  });

async function showFeed(items: Notification[]) {
  fetchClient.GET.mockResolvedValue(ok({ items, nextCursor: null }));
  await render(
    <QueryClientProvider client={testQueryClient()}>
      <NotificationsScreen />
    </QueryClientProvider>,
  );
}

beforeEach(async () => {
  await i18n.changeLanguage('en');
  jest.spyOn(Linking, 'openURL').mockResolvedValue(undefined);
});

afterEach(() => {
  jest.restoreAllMocks();
  jest.clearAllMocks();
});

describe('NotificationsScreen', () => {
  it('shows a notice with the platform text and a decision with its translated kind', async () => {
    await showFeed([notice, decision]);

    const [first, second] = await screen.findAllByRole('link');
    expect(within(first).getByText('Fiverr · Notice · 5 minutes ago · New')).toBeOnTheScreen();
    expect(within(first).getByText('You received a new order')).toBeOnTheScreen();
    expect(within(first).getByText('Grace ordered Logo design.')).toBeOnTheScreen();
    expect(within(second).getByText('Freelancer.com · 3 hours ago')).toBeOnTheScreen();
    expect(within(second).getByText('Your proposal was rejected')).toBeOnTheScreen();
    expect(within(second).getByText('Spring Boot REST API')).toBeOnTheScreen();
  });

  it('opens a tapped notification on its platform and marks it read', async () => {
    await showFeed([notice]);
    fetchClient.POST.mockResolvedValue(ok());

    await fireEvent.press(await screen.findByText('You received a new order'));

    expect(Linking.openURL).toHaveBeenCalledWith('https://www.fiverr.com');
    await waitFor(() =>
      expect(fetchClient.POST).toHaveBeenCalledWith('/api/v1/notifications/{notificationId}/read', {
        params: { path: { notificationId: 'n-1' } },
      }),
    );
  });

  it('offers to mark everything read only while something is unread', async () => {
    await showFeed([decision]);

    expect(await screen.findByText('Your proposal was rejected')).toBeOnTheScreen();
    expect(screen.queryByText('Mark all as read')).not.toBeOnTheScreen();
  });

  it('explains an empty feed', async () => {
    await showFeed([]);

    expect(
      await screen.findByText('No notifications yet. They appear here as your platforms send them.'),
    ).toBeOnTheScreen();
  });
});
