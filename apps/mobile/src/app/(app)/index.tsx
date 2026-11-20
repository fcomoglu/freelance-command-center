import { type components, unwrap } from '@fcc/api-client';
import { formatBudget, formatRelativeTime } from '@fcc/i18n';
import { useInfiniteQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { FlatList, Linking, Pressable, RefreshControl, ScrollView, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { api } from '@/api';
import { Notice } from '@/components/form';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { MaxContentWidth, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { describeError } from '@/problem';

type Job = components['schemas']['Job'];
type Platform = Job['platform'];

const platforms: Platform[] = ['freelancer', 'upwork', 'fiverr', 'bionluk', 'arc_dev'];
const PAGE_SIZE = 20;
// The server syncs platforms every few minutes; polling more often would only repeat the same data.
const REFRESH_INTERVAL_MS = 60_000;

/** The job feed: newest jobs first; pull to refresh, scroll to load more, tap to open the job on its platform. */
export default function JobsScreen() {
  const { t } = useTranslation();
  const [platform, setPlatform] = useState<Platform | undefined>();
  const feed = useInfiniteQuery({
    queryKey: ['jobs', { platform }],
    queryFn: ({ pageParam, signal }) =>
      unwrap(
        api.fetchClient.GET('/api/v1/jobs', {
          params: { query: { limit: PAGE_SIZE, platform, cursor: pageParam } },
          signal,
        }),
      ),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (page) => page.nextCursor,
    refetchInterval: REFRESH_INTERVAL_MS,
  });
  const jobs = feed.data?.pages.flatMap((page) => page.items) ?? [];

  return (
    <ThemedView style={styles.container}>
      <SafeAreaView style={styles.content} edges={['top', 'left', 'right']}>
        <FlatList
          data={jobs}
          keyExtractor={(job) => job.id}
          renderItem={({ item }) => <JobCard job={item} now={feed.dataUpdatedAt} />}
          contentContainerStyle={styles.list}
          ListHeaderComponent={
            <View style={styles.header}>
              <ThemedText type="subtitle">{t('jobs.title')}</ThemedText>
              <PlatformFilter value={platform} onChange={setPlatform} />
              {feed.isError && <Notice tone="error">{describeError(feed.error, t).message}</Notice>}
              {feed.isSuccess && jobs.length === 0 && <Notice tone="info">{t('jobs.empty')}</Notice>}
            </View>
          }
          refreshControl={
            <RefreshControl
              refreshing={feed.isRefetching && !feed.isFetchingNextPage}
              onRefresh={() => void feed.refetch()}
            />
          }
          onEndReachedThreshold={0.5}
          onEndReached={() => {
            if (feed.hasNextPage && !feed.isFetchingNextPage) void feed.fetchNextPage();
          }}
        />
      </SafeAreaView>
    </ThemedView>
  );
}

function PlatformFilter({
  value,
  onChange,
}: {
  value: Platform | undefined;
  onChange: (platform: Platform | undefined) => void;
}) {
  const { t } = useTranslation();
  const options: { key: string; platform: Platform | undefined; label: string }[] = [
    { key: 'all', platform: undefined, label: t('jobs.allPlatforms') },
    ...platforms.map((code) => ({ key: code, platform: code, label: t(`platforms.${code}`) })),
  ];
  return (
    <ScrollView
      horizontal
      showsHorizontalScrollIndicator={false}
      accessibilityLabel={t('jobs.platformFilter')}
      contentContainerStyle={styles.chips}
    >
      {options.map((option) => (
        <Chip
          key={option.key}
          label={option.label}
          selected={option.platform === value}
          onPress={() => onChange(option.platform)}
        />
      ))}
    </ScrollView>
  );
}

function Chip({ label, selected, onPress }: { label: string; selected: boolean; onPress: () => void }) {
  const theme = useTheme();
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityState={{ selected }}
      onPress={onPress}
      style={[
        styles.chip,
        { backgroundColor: selected ? theme.backgroundSelected : theme.backgroundElement },
      ]}
    >
      <ThemedText type={selected ? 'smallBold' : 'small'}>{label}</ThemedText>
    </Pressable>
  );
}

/** `now` is the time of the last refresh, so relative times advance with every poll. */
function JobCard({ job, now }: { job: Job; now: number }) {
  const { t, i18n } = useTranslation();
  const theme = useTheme();
  const language = i18n.resolvedLanguage ?? 'en';
  const platformName = t(`platforms.${job.platform}`);
  const meta = [
    platformName,
    formatRelativeTime(new Date(job.postedAt), new Date(now), language),
    job.bidCount != null ? t('jobs.bids', { count: job.bidCount }) : undefined,
  ].filter(Boolean);

  return (
    <Pressable
      accessibilityRole="link"
      accessibilityHint={t('jobs.openOn', { platform: platformName })}
      onPress={() => void Linking.openURL(job.url)}
      style={({ pressed }) => [
        styles.card,
        { backgroundColor: theme.backgroundElement },
        pressed && styles.pressed,
      ]}
    >
      <ThemedText type="small" themeColor="textSecondary">
        {meta.join(' · ')}
      </ThemedText>
      <ThemedText type="smallBold" style={styles.title}>
        {job.title}
      </ThemedText>
      {job.budget && (
        <ThemedText type="smallBold" style={styles.budget}>
          {formatBudget(job.budget, language, t)}
        </ThemedText>
      )}
      {job.description ? (
        <ThemedText type="small" themeColor="textSecondary" numberOfLines={3}>
          {job.description}
        </ThemedText>
      ) : null}
      {job.skills.length > 0 && (
        <ThemedText type="small" themeColor="textSecondary" numberOfLines={1}>
          {job.skills.join(' · ')}
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
  chips: { gap: Spacing.two },
  chip: { borderRadius: 999, paddingHorizontal: Spacing.three, paddingVertical: Spacing.two },
  card: { borderRadius: Spacing.three, padding: Spacing.three, gap: Spacing.one },
  title: { fontSize: 16, lineHeight: 22 },
  budget: { color: '#16a34a' },
  pressed: { opacity: 0.7 },
});
