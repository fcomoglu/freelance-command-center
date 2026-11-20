import { type components, unwrap } from '@fcc/api-client';
import { formatRelativeTime } from '@fcc/i18n';
import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import { api } from '../api';
import { Alert, Button } from '../components/ui';
import { describeError } from '../problem';

type InboundMail = components['schemas']['InboundMail'];

const PAGE_SIZE = 20;
// Forwarded mail arrives within seconds; a short poll lets the user see a test forward land.
const REFRESH_INTERVAL_MS = 15_000;
const inboxKey = ['mail-inbox'];
const mailsKey = ['inbound-mails'];

const statusTones: Record<InboundMail['status'], string> = {
  processed: 'bg-green-50 text-green-800 dark:bg-green-950 dark:text-green-200',
  unrecognized: 'bg-amber-50 text-amber-800 dark:bg-amber-950 dark:text-amber-200',
  unknown_sender: 'bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300',
};

export function PlatformMailPage() {
  const { t } = useTranslation();

  return (
    <section className="mx-auto flex max-w-3xl flex-col gap-4">
      <h1 className="text-2xl font-semibold">{t('platformMail.title')}</h1>
      <p className="text-sm text-gray-600 dark:text-gray-400">{t('platformMail.intro')}</p>
      <InboxAddress />
      <RecentMails />
    </section>
  );
}

function InboxAddress() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [copied, setCopied] = useState(false);
  const inbox = useQuery({
    queryKey: inboxKey,
    queryFn: ({ signal }) => unwrap(api.fetchClient.GET('/api/v1/mail-inbox', { signal })),
  });
  const renew = useMutation({
    mutationFn: () => unwrap(api.fetchClient.POST('/api/v1/mail-inbox/renewal')),
    onSuccess: (renewed) => {
      queryClient.setQueryData(inboxKey, renewed);
      setCopied(false);
    },
  });
  const error = inbox.error ?? renew.error;

  async function copy(address: string) {
    await navigator.clipboard.writeText(address);
    setCopied(true);
  }

  return (
    <div className="flex flex-col gap-4 rounded-lg border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-gray-900">
      <h2 className="text-sm font-medium">{t('platformMail.address')}</h2>
      {inbox.data && (
        <div className="flex flex-wrap items-center gap-2">
          <code className="rounded bg-gray-100 px-3 py-2 text-sm break-all dark:bg-gray-800">
            {inbox.data.address}
          </code>
          <Button variant="secondary" onClick={() => void copy(inbox.data.address)}>
            {copied ? t('platformMail.copied') : t('platformMail.copy')}
          </Button>
        </div>
      )}
      <ol className="flex list-decimal flex-col gap-1 pl-5 text-sm text-gray-600 dark:text-gray-400">
        <li>{t('platformMail.steps.rule')}</li>
        <li>{t('platformMail.steps.senders')}</li>
        <li>{t('platformMail.steps.check')}</li>
      </ol>
      {error && <Alert tone="error">{describeError(error, t).message}</Alert>}
      <div className="flex flex-col gap-1 border-t border-gray-200 pt-4 dark:border-gray-800">
        <p className="text-xs text-gray-500 dark:text-gray-400">{t('platformMail.renewHint')}</p>
        <Button
          variant="secondary"
          loading={renew.isPending}
          disabled={!inbox.data}
          onClick={() => renew.mutate()}
          className="self-start"
        >
          {t('platformMail.renew')}
        </Button>
      </div>
    </div>
  );
}

function RecentMails() {
  const { t, i18n } = useTranslation();
  const language = i18n.resolvedLanguage ?? 'en';
  const mails = useInfiniteQuery({
    queryKey: mailsKey,
    queryFn: ({ pageParam, signal }) =>
      unwrap(
        api.fetchClient.GET('/api/v1/inbound-mails', {
          params: { query: { limit: PAGE_SIZE, cursor: pageParam } },
          signal,
        }),
      ),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (page) => page.nextCursor,
    refetchInterval: REFRESH_INTERVAL_MS,
  });
  const items = mails.data?.pages.flatMap((page) => page.items) ?? [];

  return (
    <div className="flex flex-col gap-3">
      <h2 className="text-lg font-semibold">{t('platformMail.recent')}</h2>
      {mails.isError && <Alert tone="error">{describeError(mails.error, t).message}</Alert>}
      {mails.isSuccess && items.length === 0 && <Alert tone="info">{t('platformMail.empty')}</Alert>}
      <ul className="flex flex-col divide-y divide-gray-200 rounded-lg border border-gray-200 bg-white dark:divide-gray-800 dark:border-gray-800 dark:bg-gray-900">
        {items.map((mail) => (
          <li key={mail.id} className="flex flex-col gap-1 px-4 py-3">
            <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-gray-500 dark:text-gray-400">
              {mail.platform && (
                <span className="rounded bg-gray-100 px-2 py-0.5 font-medium text-gray-700 dark:bg-gray-800 dark:text-gray-300">
                  {t(`platforms.${mail.platform}`)}
                </span>
              )}
              <span className={`rounded px-2 py-0.5 font-medium ${statusTones[mail.status]}`}>
                {t(`platformMail.status.${mail.status}`)}
              </span>
              <time dateTime={mail.receivedAt}>
                {formatRelativeTime(new Date(mail.receivedAt), new Date(mails.dataUpdatedAt), language)}
              </time>
              <span className="break-all">{mail.sender}</span>
            </div>
            <p className="text-sm break-words">{mail.subject || t('platformMail.noSubject')}</p>
          </li>
        ))}
      </ul>
      {mails.hasNextPage && (
        <Button
          variant="secondary"
          loading={mails.isFetchingNextPage}
          onClick={() => void mails.fetchNextPage()}
          className="self-center"
        >
          {t('platformMail.loadMore')}
        </Button>
      )}
    </div>
  );
}
