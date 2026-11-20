import { type components, unwrap } from '@fcc/api-client';
import { formatRelativeTime } from '@fcc/i18n';
import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from '@tanstack/react-router';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';

import { api } from '../api';
import { Alert, Button } from '../components/ui';
import { describeError } from '../problem';

type Conversation = components['schemas']['Conversation'];

const PAGE_SIZE = 20;
// Messages from e-mail arrive within seconds of the platform's notification; a short poll shows them promptly.
const REFRESH_INTERVAL_MS = 30_000;
const conversationsKey = ['conversations'];

/** The inbox: conversations from every platform, newest first; the selected one is shown next to the list. */
export function MessagesPage({ selectedId }: { selectedId?: string }) {
  const { t } = useTranslation();
  const conversations = useInfiniteQuery({
    queryKey: conversationsKey,
    queryFn: ({ pageParam, signal }) =>
      unwrap(
        api.fetchClient.GET('/api/v1/conversations', {
          params: { query: { limit: PAGE_SIZE, cursor: pageParam } },
          signal,
        }),
      ),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (page) => page.nextCursor,
    refetchInterval: REFRESH_INTERVAL_MS,
  });
  const items = conversations.data?.pages.flatMap((page) => page.items) ?? [];
  const selected = items.find((conversation) => conversation.id === selectedId);

  return (
    <section className="mx-auto flex max-w-5xl flex-col gap-4">
      <h1 className="text-2xl font-semibold">{t('messages.title')}</h1>
      {conversations.isError && <Alert tone="error">{describeError(conversations.error, t).message}</Alert>}
      {conversations.isSuccess && items.length === 0 && <Alert tone="info">{t('messages.empty')}</Alert>}
      {items.length > 0 && (
        <div className="grid gap-4 md:grid-cols-[minmax(0,2fr)_minmax(0,3fr)]">
          <div className="flex flex-col gap-3">
            <ul className="flex flex-col divide-y divide-gray-200 rounded-lg border border-gray-200 bg-white dark:divide-gray-800 dark:border-gray-800 dark:bg-gray-900">
              {items.map((conversation) => (
                <ConversationRow
                  key={conversation.id}
                  conversation={conversation}
                  selected={conversation.id === selectedId}
                  now={conversations.dataUpdatedAt}
                />
              ))}
            </ul>
            {conversations.hasNextPage && (
              <Button
                variant="secondary"
                loading={conversations.isFetchingNextPage}
                onClick={() => void conversations.fetchNextPage()}
                className="self-center"
              >
                {t('messages.loadMore')}
              </Button>
            )}
          </div>
          {selected ? (
            <ConversationView key={selected.id} conversation={selected} />
          ) : (
            <p className="text-sm text-gray-500 dark:text-gray-400">{t('messages.choose')}</p>
          )}
        </div>
      )}
    </section>
  );
}

function ConversationRow({
  conversation,
  selected,
  now,
}: {
  conversation: Conversation;
  selected: boolean;
  now: number;
}) {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const language = i18n.resolvedLanguage ?? 'en';
  const unread = conversation.unreadCount > 0;

  return (
    <li>
      <button
        type="button"
        aria-current={selected ? 'true' : undefined}
        onClick={() => void navigate({ to: '/messages', search: { conversation: conversation.id } })}
        className={`flex w-full flex-col gap-1 px-4 py-3 text-left hover:bg-gray-50 dark:hover:bg-gray-800 ${selected ? 'bg-gray-100 dark:bg-gray-800' : ''}`}
      >
        <span className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-gray-500 dark:text-gray-400">
          <span className="rounded bg-gray-100 px-2 py-0.5 font-medium text-gray-700 dark:bg-gray-800 dark:text-gray-300">
            {t(`platforms.${conversation.platform}`)}
          </span>
          <time dateTime={conversation.lastMessageAt}>
            {formatRelativeTime(new Date(conversation.lastMessageAt), new Date(now), language)}
          </time>
          {unread && (
            <span className="rounded-full bg-blue-600 px-2 py-0.5 font-medium text-white dark:bg-blue-500">
              {t('messages.unread', { count: conversation.unreadCount })}
            </span>
          )}
        </span>
        <span className={`text-sm ${unread ? 'font-semibold' : 'font-medium'}`}>
          {conversation.counterpart}
        </span>
        <span className="truncate text-sm text-gray-600 dark:text-gray-400">
          {conversation.preview || t('messages.noText')}
        </span>
      </button>
    </li>
  );
}

function ConversationView({ conversation }: { conversation: Conversation }) {
  const { t, i18n } = useTranslation();
  const language = i18n.resolvedLanguage ?? 'en';
  const queryClient = useQueryClient();
  const platform = t(`platforms.${conversation.platform}`);
  const messages = useInfiniteQuery({
    queryKey: ['conversation-messages', conversation.id],
    queryFn: ({ pageParam, signal }) =>
      unwrap(
        api.fetchClient.GET('/api/v1/conversations/{conversationId}/messages', {
          params: {
            path: { conversationId: conversation.id },
            query: { limit: PAGE_SIZE, cursor: pageParam },
          },
          signal,
        }),
      ),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (page) => page.nextCursor,
    refetchInterval: REFRESH_INTERVAL_MS,
  });
  const markRead = useMutation({
    mutationFn: () =>
      unwrap(
        api.fetchClient.POST('/api/v1/conversations/{conversationId}/read', {
          params: { path: { conversationId: conversation.id } },
        }),
      ),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: conversationsKey }),
  });
  const { mutate: read } = markRead;

  // Opening a conversation reads it; new messages that arrive while it is open are read the same way.
  useEffect(() => {
    if (conversation.unreadCount > 0) read();
  }, [conversation.unreadCount, read]);

  // Oldest first on screen; pages arrive newest first.
  const items = (messages.data?.pages.flatMap((page) => page.items) ?? []).toReversed();

  return (
    <div className="flex flex-col gap-4 rounded-lg border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-gray-900">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-lg font-semibold">{conversation.counterpart}</h2>
        <a
          href={conversation.url}
          target="_blank"
          rel="noopener noreferrer"
          className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 dark:bg-blue-500 dark:hover:bg-blue-400"
        >
          {t('messages.openOn', { platform })}
        </a>
      </div>
      <p className="text-xs text-gray-500 dark:text-gray-400">
        {conversation.detail === 'notice'
          ? t('messages.noticeHint', { platform })
          : t('messages.replyHint', { platform })}
      </p>
      {messages.isError && <Alert tone="error">{describeError(messages.error, t).message}</Alert>}
      {markRead.isError && <Alert tone="error">{describeError(markRead.error, t).message}</Alert>}
      {messages.hasNextPage && (
        <Button
          variant="secondary"
          loading={messages.isFetchingNextPage}
          onClick={() => void messages.fetchNextPage()}
          className="self-center"
        >
          {t('messages.older')}
        </Button>
      )}
      <ol className="flex flex-col gap-3">
        {items.map((message) => (
          <li
            key={message.id}
            className={`flex max-w-[85%] flex-col gap-1 rounded-lg px-3 py-2 ${
              message.direction === 'outbound'
                ? 'self-end bg-blue-50 dark:bg-blue-950'
                : 'self-start bg-gray-100 dark:bg-gray-800'
            }`}
          >
            <span className="text-xs text-gray-500 dark:text-gray-400">
              {message.direction === 'outbound' ? t('messages.you') : conversation.counterpart} ·{' '}
              <time dateTime={message.sentAt}>
                {formatRelativeTime(new Date(message.sentAt), new Date(messages.dataUpdatedAt), language)}
              </time>
            </span>
            <p className="text-sm break-words whitespace-pre-line">{message.body || t('messages.noText')}</p>
          </li>
        ))}
      </ol>
    </div>
  );
}
