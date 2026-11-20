import { type components, unwrap } from '@fcc/api-client';
import { formatRelativeTime } from '@fcc/i18n';
import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';

import { api } from '../api';
import { Alert, Button } from '../components/ui';
import { describeError } from '../problem';

type Notification = components['schemas']['Notification'];

const PAGE_SIZE = 20;
// Notices from e-mail arrive within seconds; bid decisions with the next sync of the connected account.
const REFRESH_INTERVAL_MS = 30_000;
const notificationsKey = ['notifications'];

/** The feed of platform notifications, newest first; opening one on its platform marks it read. */
export function NotificationsPage() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const notifications = useInfiniteQuery({
    queryKey: notificationsKey,
    queryFn: ({ pageParam, signal }) =>
      unwrap(
        api.fetchClient.GET('/api/v1/notifications', {
          params: { query: { limit: PAGE_SIZE, cursor: pageParam } },
          signal,
        }),
      ),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (page) => page.nextCursor,
    refetchInterval: REFRESH_INTERVAL_MS,
  });
  const markAllRead = useMutation({
    mutationFn: () => unwrap(api.fetchClient.POST('/api/v1/notifications/read')),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: notificationsKey }),
  });
  const items = notifications.data?.pages.flatMap((page) => page.items) ?? [];

  return (
    <section className="mx-auto flex max-w-3xl flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold">{t('notifications.title')}</h1>
        {items.some((notification) => !notification.read) && (
          <Button variant="secondary" loading={markAllRead.isPending} onClick={() => markAllRead.mutate()}>
            {t('notifications.markAllRead')}
          </Button>
        )}
      </div>
      <p className="text-sm text-gray-600 dark:text-gray-400">{t('notifications.intro')}</p>
      {notifications.isError && <Alert tone="error">{describeError(notifications.error, t).message}</Alert>}
      {markAllRead.isError && <Alert tone="error">{describeError(markAllRead.error, t).message}</Alert>}
      {notifications.isSuccess && items.length === 0 && <Alert tone="info">{t('notifications.empty')}</Alert>}
      {items.length > 0 && (
        <ul className="flex flex-col divide-y divide-gray-200 rounded-lg border border-gray-200 bg-white dark:divide-gray-800 dark:border-gray-800 dark:bg-gray-900">
          {items.map((notification) => (
            <NotificationRow
              key={notification.id}
              notification={notification}
              now={notifications.dataUpdatedAt}
            />
          ))}
        </ul>
      )}
      {notifications.hasNextPage && (
        <Button
          variant="secondary"
          loading={notifications.isFetchingNextPage}
          onClick={() => void notifications.fetchNextPage()}
          className="self-center"
        >
          {t('notifications.loadMore')}
        </Button>
      )}
    </section>
  );
}

function NotificationRow({ notification, now }: { notification: Notification; now: number }) {
  const { t, i18n } = useTranslation();
  const language = i18n.resolvedLanguage ?? 'en';
  const queryClient = useQueryClient();
  const platform = t(`platforms.${notification.platform}`);
  const markRead = useMutation({
    mutationFn: () =>
      unwrap(
        api.fetchClient.POST('/api/v1/notifications/{notificationId}/read', {
          params: { path: { notificationId: notification.id } },
        }),
      ),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: notificationsKey }),
  });
  const notice = notification.kind === 'notice';
  const heading = notice
    ? notification.title || t('notifications.noSubject')
    : t(`notifications.kind.${notification.kind}`);

  return (
    <li className="flex flex-col gap-2 px-4 py-3">
      <span className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-gray-500 dark:text-gray-400">
        <span className="rounded bg-gray-100 px-2 py-0.5 font-medium text-gray-700 dark:bg-gray-800 dark:text-gray-300">
          {platform}
        </span>
        {notice && <span>{t('notifications.kind.notice')}</span>}
        <time dateTime={notification.occurredAt}>
          {formatRelativeTime(new Date(notification.occurredAt), new Date(now), language)}
        </time>
        {!notification.read && (
          <span className="rounded-full bg-blue-600 px-2 py-0.5 font-medium text-white dark:bg-blue-500">
            {t('notifications.new')}
          </span>
        )}
      </span>
      <span className={`text-sm ${notification.read ? 'font-medium' : 'font-semibold'}`}>{heading}</span>
      {!notice && notification.title && (
        <span className="text-sm text-gray-700 dark:text-gray-300">{notification.title}</span>
      )}
      {notification.body && (
        <p className="text-sm break-words text-gray-600 dark:text-gray-400">{notification.body}</p>
      )}
      <div className="flex flex-wrap items-center gap-3">
        <a
          href={notification.url}
          target="_blank"
          rel="noopener noreferrer"
          onClick={() => {
            if (!notification.read) markRead.mutate();
          }}
          className="text-sm font-medium text-blue-600 hover:underline dark:text-blue-400"
        >
          {t('notifications.openOn', { platform })}
        </a>
        {markRead.isError && (
          <span className="text-sm text-red-600 dark:text-red-400">
            {describeError(markRead.error, t).message}
          </span>
        )}
      </div>
    </li>
  );
}
