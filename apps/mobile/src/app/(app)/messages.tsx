import { type components, unwrap } from '@fcc/api-client';
import { formatRelativeTime } from '@fcc/i18n';
import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { FlatList, Linking, Pressable, RefreshControl, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { api } from '@/api';
import { Notice } from '@/components/form';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { MaxContentWidth, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { describeError } from '@/problem';

type Conversation = components['schemas']['Conversation'];

const PAGE_SIZE = 20;
// Messages from e-mail arrive within seconds of the platform's notification; a short poll shows them promptly.
const REFRESH_INTERVAL_MS = 30_000;
const conversationsKey = ['conversations'];

/**
 * The inbox: conversations from every platform, newest first. Replies happen on the platform, so tapping a
 * conversation marks it read and opens it there.
 */
export default function MessagesScreen() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
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
  const markRead = useMutation({
    mutationFn: (conversationId: string) =>
      unwrap(
        api.fetchClient.POST('/api/v1/conversations/{conversationId}/read', {
          params: { path: { conversationId } },
        }),
      ),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: conversationsKey }),
  });
  const items = conversations.data?.pages.flatMap((page) => page.items) ?? [];

  function open(conversation: Conversation) {
    if (conversation.unreadCount > 0) markRead.mutate(conversation.id);
    void Linking.openURL(conversation.url);
  }

  return (
    <ThemedView style={styles.container}>
      <SafeAreaView style={styles.content} edges={['top', 'left', 'right']}>
        <FlatList
          data={items}
          keyExtractor={(conversation) => conversation.id}
          renderItem={({ item }) => (
            <ConversationCard
              conversation={item}
              now={conversations.dataUpdatedAt}
              onPress={() => open(item)}
            />
          )}
          contentContainerStyle={styles.list}
          ListHeaderComponent={
            <View style={styles.header}>
              <ThemedText type="subtitle">{t('messages.title')}</ThemedText>
              <ThemedText type="small" themeColor="textSecondary">
                {t('messages.mobileHint')}
              </ThemedText>
              {conversations.isError && (
                <Notice tone="error">{describeError(conversations.error, t).message}</Notice>
              )}
              {markRead.isError && <Notice tone="error">{describeError(markRead.error, t).message}</Notice>}
              {conversations.isSuccess && items.length === 0 && (
                <Notice tone="info">{t('messages.empty')}</Notice>
              )}
            </View>
          }
          refreshControl={
            <RefreshControl
              refreshing={conversations.isRefetching && !conversations.isFetchingNextPage}
              onRefresh={() => void conversations.refetch()}
            />
          }
          onEndReachedThreshold={0.5}
          onEndReached={() => {
            if (conversations.hasNextPage && !conversations.isFetchingNextPage)
              void conversations.fetchNextPage();
          }}
        />
      </SafeAreaView>
    </ThemedView>
  );
}

/** `now` is the time of the last refresh, so relative times advance with every poll. */
function ConversationCard({
  conversation,
  now,
  onPress,
}: {
  conversation: Conversation;
  now: number;
  onPress: () => void;
}) {
  const { t, i18n } = useTranslation();
  const theme = useTheme();
  const language = i18n.resolvedLanguage ?? 'en';
  const platform = t(`platforms.${conversation.platform}`);
  const unread = conversation.unreadCount > 0;
  const meta = [platform, formatRelativeTime(new Date(conversation.lastMessageAt), new Date(now), language)];
  if (unread) meta.push(t('messages.unread', { count: conversation.unreadCount }));

  return (
    <Pressable
      accessibilityRole="link"
      accessibilityHint={t('messages.openOn', { platform })}
      onPress={onPress}
      style={({ pressed }) => [
        styles.card,
        { backgroundColor: theme.backgroundElement },
        pressed && styles.pressed,
      ]}
    >
      <ThemedText type={unread ? 'smallBold' : 'small'} themeColor={unread ? 'text' : 'textSecondary'}>
        {meta.join(' · ')}
      </ThemedText>
      <ThemedText type="smallBold" style={styles.counterpart}>
        {conversation.counterpart}
      </ThemedText>
      <ThemedText type="small" themeColor="textSecondary" numberOfLines={2}>
        {conversation.preview || t('messages.noText')}
      </ThemedText>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, alignItems: 'center' },
  content: { flex: 1, width: '100%', maxWidth: MaxContentWidth },
  list: { padding: Spacing.three, gap: Spacing.three },
  header: { gap: Spacing.three },
  card: { borderRadius: Spacing.three, padding: Spacing.three, gap: Spacing.one },
  counterpart: { fontSize: 16, lineHeight: 22 },
  pressed: { opacity: 0.7 },
});
