import { type components, unwrap } from '@fcc/api-client';
import { formatBudget, formatRelativeTime } from '@fcc/i18n';
import { useInfiniteQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import { api } from '../api';
import { Alert, Button } from '../components/ui';
import { describeError } from '../problem';

type Job = components['schemas']['Job'];
type Platform = Job['platform'];

const platforms: Platform[] = ['freelancer', 'upwork', 'fiverr', 'bionluk', 'arc_dev'];
const PAGE_SIZE = 20;
// The server syncs platforms every few minutes; polling more often would only repeat the same data.
const REFRESH_INTERVAL_MS = 60_000;

export function JobsPage() {
  const { t } = useTranslation();
  const [platform, setPlatform] = useState<Platform | undefined>();
  // Plain TanStack Query instead of the openapi-react-query helper: the helper sends `cursor=0` for the first page and
  // throws raw problem bodies, while `unwrap` throws ApiError for describeError.
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
    <section className="mx-auto flex max-w-3xl flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold">{t('jobs.title')}</h1>
        <label className="flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400">
          {t('jobs.platformFilter')}
          <select
            value={platform ?? ''}
            onChange={(event) => setPlatform((event.target.value || undefined) as Platform | undefined)}
            className="rounded-md border border-gray-300 bg-white px-2 py-1 text-gray-900 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-100"
          >
            <option value="">{t('jobs.allPlatforms')}</option>
            {platforms.map((code) => (
              <option key={code} value={code}>
                {t(`platforms.${code}`)}
              </option>
            ))}
          </select>
        </label>
      </div>

      {feed.isError && <Alert tone="error">{describeError(feed.error, t).message}</Alert>}
      {feed.isSuccess && jobs.length === 0 && <Alert tone="info">{t('jobs.empty')}</Alert>}

      <ul className="flex flex-col gap-3">
        {jobs.map((job) => (
          <li key={job.id}>
            <JobCard job={job} now={feed.dataUpdatedAt} />
          </li>
        ))}
      </ul>

      {feed.hasNextPage && (
        <Button
          variant="secondary"
          loading={feed.isFetchingNextPage}
          onClick={() => void feed.fetchNextPage()}
          className="self-center"
        >
          {t('jobs.loadMore')}
        </Button>
      )}
    </section>
  );
}

/** `now` is the time of the last refresh, so relative times advance with every poll. */
function JobCard({ job, now }: { job: Job; now: number }) {
  const { t, i18n } = useTranslation();
  const language = i18n.resolvedLanguage ?? 'en';
  const platformName = t(`platforms.${job.platform}`);

  return (
    <article className="flex flex-col gap-2 rounded-lg border border-gray-200 bg-white p-4 dark:border-gray-800 dark:bg-gray-900">
      <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-gray-500 dark:text-gray-400">
        <span className="rounded bg-gray-100 px-2 py-0.5 font-medium text-gray-700 dark:bg-gray-800 dark:text-gray-300">
          {platformName}
        </span>
        <time dateTime={job.postedAt}>
          {formatRelativeTime(new Date(job.postedAt), new Date(now), language)}
        </time>
        {job.bidCount != null && <span>{t('jobs.bids', { count: job.bidCount })}</span>}
      </div>
      <h2 className="text-base font-semibold">
        <a
          href={job.url}
          target="_blank"
          rel="noopener noreferrer"
          title={t('jobs.openOn', { platform: platformName })}
          className="hover:underline"
        >
          {job.title}
        </a>
      </h2>
      {job.budget && (
        <p className="text-sm font-medium text-green-700 dark:text-green-400">
          {formatBudget(job.budget, language, t)}
        </p>
      )}
      {job.description && (
        <p className="line-clamp-3 text-sm whitespace-pre-line text-gray-700 dark:text-gray-300">
          {job.description}
        </p>
      )}
      {job.skills.length > 0 && (
        <ul className="flex flex-wrap gap-1.5">
          {job.skills.map((skill) => (
            <li
              key={skill}
              className="rounded-full border border-gray-200 px-2 py-0.5 text-xs text-gray-600 dark:border-gray-700 dark:text-gray-400"
            >
              {skill}
            </li>
          ))}
        </ul>
      )}
    </article>
  );
}
