import { type components, unwrap } from '@fcc/api-client';
import { formatRelativeTime } from '@fcc/i18n';
import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { FlatList, Linking, Pressable, RefreshControl, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { api } from '@/api';
import { Button, Notice } from '@/components/form';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { MaxContentWidth, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { describeError } from '@/problem';

type Notification = components['schemas']['Notification'];

const PAGE_SIZE = 20;
// Notices from e-mail arrive within seconds; bid decisions with the next sync of the connected account.
const REFRESH_INTERVAL_MS = 30_000;
const notificationsKey = ['notifications'];

/** The feed of platform notifications, newest first; tapping one marks it read and opens it on its platform. */
export default function NotificationsScreen() {
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
  const markRead = useMutation({
    mutationFn: (notificationId: string) =>
      unwrap(
        api.fetchClient.POST('/api/v1/notifications/{notificationId}/read', {
          params: { path: { notificationId } },
        }),
      ),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: notificationsKey }),
  });
  const markAllRead = useMutation({
    mutationFn: () => unwrap(api.fetchClient.POST('/api/v1/notifications/read')),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: notificationsKey }),
  });
  const items = notifications.data?.pages.flatMap((page) => page.items) ?? [];

  function open(notification: Notification) {
    if (!notification.read) markRead.mutate(notification.id);
    void Linking.openURL(notification.url);
  }

  return (
    <ThemedView style={styles.container}>
      <SafeAreaView style={styles.content} edges={['top', 'left', 'right']}>
        <FlatList
          data={items}
          keyExtractor={(notification) => notification.id}
          renderItem={({ item }) => (
            <NotificationCard
              notification={item}
              now={notifications.dataUpdatedAt}
              onPress={() => open(item)}
            />
          )}
          contentContainerStyle={styles.list}
          ListHeaderComponent={
            <View style={styles.header}>
              <ThemedText type="subtitle">{t('notifications.title')}</ThemedText>
              <ThemedText type="small" themeColor="textSecondary">
                {t('notifications.mobileHint')}
              </ThemedText>
              {items.some((notification) => !notification.read) && (
                <Button
                  title={t('notifications.markAllRead')}
                  variant="secondary"
                  loading={markAllRead.isPending}
                  onPress={() => markAllRead.mutate()}
                />
              )}
              {notifications.isError && (
                <Notice tone="error">{describeError(notifications.error, t).message}</Notice>
              )}
              {markRead.isError && <Notice tone="error">{describeError(markRead.error, t).message}</Notice>}
              {markAllRead.isError && (
                <Notice tone="error">{describeError(markAllRead.error, t).message}</Notice>
              )}
              {notifications.isSuccess && items.length === 0 && (
                <Notice tone="info">{t('notifications.empty')}</Notice>
              )}
            </View>
          }
          refreshControl={
            <RefreshControl
              refreshing={notifications.isRefetching && !notifications.isFetchingNextPage}
              onRefresh={() => void notifications.refetch()}
            />
          }
          onEndReachedThreshold={0.5}
          onEndReached={() => {
            if (notifications.hasNextPage && !notifications.isFetchingNextPage)
              void notifications.fetchNextPage();
          }}
        />
      </SafeAreaView>
    </ThemedView>
  );
}

/** `now` is the time of the last refresh, so relative times advance with every poll. */
function NotificationCard({
  notification,
  now,
  onPress,
}: {
  notification: Notification;
  now: number;
  onPress: () => void;
}) {
  const { t, i18n } = useTranslation();
  const theme = useTheme();
  const language = i18n.resolvedLanguage ?? 'en';
  const platform = t(`platforms.${notification.platform}`);
  const notice = notification.kind === 'notice';
  const meta = [platform, formatRelativeTime(new Date(notification.occurredAt), new Date(now), language)];
  if (notice) meta.splice(1, 0, t('notifications.kind.notice'));
  if (!notification.read) meta.push(t('notifications.new'));
  const heading = notice
    ? notification.title || t('notifications.noSubject')
    : t(`notifications.kind.${notification.kind}`);
  const detail = notice ? notification.body : notification.title;

  return (
    <Pressable
      accessibilityRole="link"
      accessibilityHint={t('notifications.openOn', { platform })}
      onPress={onPress}
      style={({ pressed }) => [
        styles.card,
        { backgroundColor: theme.backgroundElement },
        pressed && styles.pressed,
      ]}
    >
      <ThemedText
        type={notification.read ? 'small' : 'smallBold'}
        themeColor={notification.read ? 'textSecondary' : 'text'}
      >
        {meta.join(' · ')}
      </ThemedText>
      <ThemedText type="smallBold" style={styles.heading}>
        {heading}
      </ThemedText>
      {detail !== '' && (
        <ThemedText type="small" themeColor="textSecondary" numberOfLines={3}>
          {detail}
        </ThemedText>
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, alignItems: 'center' },
  content: { flex: 1, width: '100%', maxWidth: MaxContentWidth },
  list: { padding: Spacing.three, gap: Spacing.three },
  header: { gap: Spacing.three },
  card: { borderRadius: Spacing.three, padding: Spacing.three, gap: Spacing.one },
  heading: { fontSize: 16, lineHeight: 22 },
  pressed: { opacity: 0.7 },
});
