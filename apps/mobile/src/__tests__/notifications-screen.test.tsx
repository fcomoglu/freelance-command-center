import type { components } from '@fcc/api-client';
import { fireEvent, screen, waitFor, within } from '@testing-library/react-native';
import { Linking } from 'react-native';

import { api } from '@/api';
import NotificationsScreen from '@/app/(app)/notifications';
import { ok, renderScreen } from '@/test-support';

type Notification = components['schemas']['Notification'];

jest.mock('@/api', () => ({ api: { fetchClient: { GET: jest.fn(), POST: jest.fn() } } }));

const fetchClient = jest.mocked(api.fetchClient);

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

async function showFeed(items: Notification[]) {
  fetchClient.GET.mockResolvedValue(ok({ items, nextCursor: null }));
  await renderScreen(<NotificationsScreen />);
}

beforeEach(() => {
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
