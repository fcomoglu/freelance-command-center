import { type components } from '@fcc/api-client';
import { cleanup, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { ok, renderScreen } from '../test-support';
import { MessagesPage } from './messages';

type Conversation = components['schemas']['Conversation'];
type Message = components['schemas']['Message'];

const api = vi.hoisted(() => ({ GET: vi.fn(), POST: vi.fn() }));
const navigate = vi.hoisted(() => vi.fn());
vi.mock('../api', () => ({ api: { fetchClient: api } }));
vi.mock('@tanstack/react-router', () => ({ useNavigate: () => navigate }));

const minutesAgo = (minutes: number) => new Date(Date.now() - minutes * 60_000).toISOString();

const fromEmail: Conversation = {
  id: 'c-1',
  platform: 'fiverr',
  counterpart: 'Grace H.',
  detail: 'notice',
  preview: 'Could you start on Monday?',
  unreadCount: 2,
  lastMessageAt: minutesAgo(5),
  url: 'https://www.fiverr.com/inbox/grace',
};
const fromApi: Conversation = {
  id: 'c-2',
  platform: 'upwork',
  counterpart: 'Alan T.',
  detail: 'full',
  preview: '',
  unreadCount: 0,
  lastMessageAt: minutesAgo(90),
  url: 'https://www.upwork.com/ab/messages/rooms/room-2',
};
const messages: Message[] = [
  { id: 'm-2', direction: 'outbound', detail: 'full', body: 'Yes, Monday works.', sentAt: minutesAgo(80) },
  {
    id: 'm-1',
    direction: 'inbound',
    detail: 'full',
    body: 'Could you start on Monday?',
    sentAt: minutesAgo(90),
  },
];

/** Answers the conversation list and the messages of a conversation. */
function serve(conversations: Conversation[], conversationMessages: Message[] = []) {
  api.GET.mockImplementation(async (path: string) =>
    path === '/api/v1/conversations'
      ? ok({ items: conversations, nextCursor: null })
      : ok({ items: conversationMessages, nextCursor: null }),
  );
  api.POST.mockResolvedValue(ok());
}

afterEach(() => {
  cleanup();
  vi.resetAllMocks();
});

describe('MessagesPage', () => {
  it('lists conversations from every platform with their unread count', async () => {
    serve([fromEmail, fromApi]);
    await renderScreen(<MessagesPage />);

    const [first, second] = await screen.findAllByRole('listitem');
    expect(within(first).getByText('Fiverr')).toBeTruthy();
    expect(within(first).getByText('Grace H.')).toBeTruthy();
    expect(within(first).getByText('2 new')).toBeTruthy();
    expect(within(second).getByText('Alan T.')).toBeTruthy();
    expect(within(second).getByText('The notification showed no text.')).toBeTruthy();
    expect(within(second).queryByText(/new$/)).toBeNull();
    expect(screen.getByText('Choose a conversation to read it.')).toBeTruthy();
  });

  it('opens a conversation through the address, so it can be linked and reloaded', async () => {
    serve([fromEmail, fromApi]);
    await renderScreen(<MessagesPage />);

    await userEvent.click(await screen.findByRole('button', { name: /Alan T\./ }));

    expect(navigate).toHaveBeenCalledWith({ to: '/messages', search: { conversation: 'c-2' } });
  });

  it('shows the selected conversation oldest first and links to the reply on the platform', async () => {
    serve([fromEmail, fromApi], messages);
    await renderScreen(<MessagesPage selectedId="c-2" />);

    const thread = (await screen.findByText('Yes, Monday works.')).closest('ol') as HTMLElement;
    const bubbles = within(thread).getAllByRole('listitem');
    expect(bubbles.map((item) => item.querySelector('p')?.textContent)).toEqual([
      'Could you start on Monday?',
      'Yes, Monday works.',
    ]);
    expect(screen.getByRole('link', { name: 'Reply on Upwork' }).getAttribute('href')).toBe(fromApi.url);
    expect(screen.getByText('Replies are written on Upwork.')).toBeTruthy();
    expect(api.POST).not.toHaveBeenCalled();
  });

  it('reads an unread conversation when it is opened and explains e-mail based ones', async () => {
    serve([fromEmail, fromApi], []);
    await renderScreen(<MessagesPage selectedId="c-1" />);

    expect(await screen.findByText(/Fiverr is read from its notification e-mails/)).toBeTruthy();
    await vi.waitFor(() =>
      expect(api.POST).toHaveBeenCalledWith('/api/v1/conversations/{conversationId}/read', {
        params: { path: { conversationId: 'c-1' } },
      }),
    );
  });

  it('explains an empty inbox', async () => {
    serve([]);
    await renderScreen(<MessagesPage />);

    expect(
      await screen.findByText('No messages yet. Messages from your platforms appear here as they arrive.'),
    ).toBeTruthy();
  });
});
