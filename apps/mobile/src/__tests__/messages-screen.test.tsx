import type { components } from '@fcc/api-client';
import { fireEvent, screen, waitFor } from '@testing-library/react-native';
import { Linking } from 'react-native';

import { api } from '@/api';
import MessagesScreen from '@/app/(app)/messages';
import { ok, renderScreen } from '@/test-support';

type Conversation = components['schemas']['Conversation'];

jest.mock('@/api', () => ({ api: { fetchClient: { GET: jest.fn(), POST: jest.fn() } } }));

const fetchClient = jest.mocked(api.fetchClient);

const unread: Conversation = {
  id: 'c-1',
  platform: 'fiverr',
  counterpart: 'Grace H.',
  detail: 'notice',
  preview: 'Could you start on Monday?',
  unreadCount: 2,
  lastMessageAt: new Date(Date.now() - 5 * 60_000).toISOString(),
  url: 'https://www.fiverr.com/inbox/grace',
};
const read: Conversation = {
  id: 'c-2',
  platform: 'upwork',
  counterpart: 'Alan T.',
  detail: 'full',
  preview: 'Thanks!',
  unreadCount: 0,
  lastMessageAt: new Date(Date.now() - 90 * 60_000).toISOString(),
  url: 'https://www.upwork.com/ab/messages/rooms/room-2',
};

beforeEach(() => {
  jest.spyOn(Linking, 'openURL').mockResolvedValue(undefined);
  fetchClient.GET.mockResolvedValue(ok({ items: [unread, read], nextCursor: null }));
  fetchClient.POST.mockResolvedValue(ok());
});

afterEach(() => {
  jest.restoreAllMocks();
  jest.clearAllMocks();
});

describe('MessagesScreen', () => {
  it('lists conversations with their platform and unread count', async () => {
    await renderScreen(<MessagesScreen />);

    expect(await screen.findByText('Grace H.')).toBeOnTheScreen();
    expect(screen.getByText('Could you start on Monday?')).toBeOnTheScreen();
    expect(screen.getByText(/^Fiverr · 5 minutes ago · 2 new$/)).toBeOnTheScreen();
    expect(screen.getByText(/^Upwork · 1 hour ago$/)).toBeOnTheScreen();
  });

  it('opens a tapped conversation on its platform and marks it read', async () => {
    await renderScreen(<MessagesScreen />);

    await fireEvent.press(await screen.findByText('Grace H.'));

    expect(Linking.openURL).toHaveBeenCalledWith(unread.url);
    await waitFor(() =>
      expect(fetchClient.POST).toHaveBeenCalledWith('/api/v1/conversations/{conversationId}/read', {
        params: { path: { conversationId: 'c-1' } },
      }),
    );
  });

  it('does not mark an already read conversation again', async () => {
    await renderScreen(<MessagesScreen />);

    await fireEvent.press(await screen.findByText('Alan T.'));

    expect(Linking.openURL).toHaveBeenCalledWith(read.url);
    expect(fetchClient.POST).not.toHaveBeenCalled();
  });
});
