import { type components, unwrap } from '@fcc/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { type FormEvent, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { api } from '../api';
import { Alert, Button, TextField } from '../components/ui';
import { describeError } from '../problem';

type JobFilter = components['schemas']['JobFilter'];
type Platform = JobFilter['platforms'][number];

const platforms: Platform[] = ['freelancer', 'upwork', 'fiverr', 'bionluk', 'arc_dev'];
const MAX_KEYWORDS = 20;
const MAX_KEYWORD_LENGTH = 50;
const filterKey = ['job-filter'];

export function JobAlertsPage() {
  const { t } = useTranslation();
  const filter = useQuery({
    queryKey: filterKey,
    queryFn: ({ signal }) => unwrap(api.fetchClient.GET('/api/v1/job-filter', { signal })),
  });

  return (
    <section className="mx-auto flex max-w-2xl flex-col gap-4">
      <h1 className="text-2xl font-semibold">{t('jobAlerts.title')}</h1>
      <p className="text-sm text-gray-600 dark:text-gray-400">{t('jobAlerts.intro')}</p>
      <Alert tone="info">{t('jobAlerts.mobileHint')}</Alert>
      {filter.isError && <Alert tone="error">{describeError(filter.error, t).message}</Alert>}
      {filter.data && <JobFilterForm initial={filter.data} />}
    </section>
  );
}

/** Edits a copy of the saved filter; mounted once the filter has loaded. */
function JobFilterForm({ initial }: { initial: JobFilter }) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [keywords, setKeywords] = useState(initial.keywords);
  const [selected, setSelected] = useState<Platform[]>(initial.platforms);
  const [draft, setDraft] = useState('');

  const save = useMutation({
    mutationFn: () =>
      unwrap(api.fetchClient.PUT('/api/v1/job-filter', { body: { keywords, platforms: selected } })),
    onSuccess: (saved) => {
      queryClient.setQueryData(filterKey, saved);
      setKeywords(saved.keywords);
    },
  });
  const error = save.isError ? describeError(save.error, t) : undefined;

  const term = draft.trim().replace(/\s+/g, ' ').toLowerCase();
  const canAdd = term.length > 0 && !keywords.includes(term) && keywords.length < MAX_KEYWORDS;

  function addKeyword(event: FormEvent) {
    event.preventDefault();
    if (!canAdd) return;
    setKeywords([...keywords, term]);
    setDraft('');
    save.reset();
  }

  function togglePlatform(code: Platform) {
    setSelected(selected.includes(code) ? selected.filter((p) => p !== code) : [...selected, code]);
    save.reset();
  }

  return (
    <div className="flex flex-col gap-6 rounded-lg border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-gray-900">
      <form onSubmit={addKeyword} className="flex flex-col gap-3">
        <div className="flex items-end gap-2">
          <div className="flex-1">
            <TextField
              label={t('jobAlerts.keywords')}
              hint={t('jobAlerts.keywordsHint', { used: keywords.length, max: MAX_KEYWORDS })}
              placeholder={t('jobAlerts.keywordPlaceholder')}
              value={draft}
              maxLength={MAX_KEYWORD_LENGTH}
              onChange={(event) => setDraft(event.target.value)}
            />
          </div>
          <Button type="submit" variant="secondary" disabled={!canAdd} className="mb-6">
            {t('jobAlerts.add')}
          </Button>
        </div>
        {keywords.length === 0 ? (
          <p className="text-sm text-amber-700 dark:text-amber-400">{t('jobAlerts.noKeywords')}</p>
        ) : (
          <ul className="flex flex-wrap gap-2">
            {keywords.map((keyword) => (
              <li
                key={keyword}
                className="flex items-center gap-1 rounded-full bg-blue-50 py-1 pr-1 pl-3 text-sm text-blue-800 dark:bg-blue-950 dark:text-blue-200"
              >
                {keyword}
                <button
                  type="button"
                  aria-label={t('jobAlerts.remove', { keyword })}
                  onClick={() => {
                    setKeywords(keywords.filter((k) => k !== keyword));
                    save.reset();
                  }}
                  className="rounded-full px-1.5 hover:bg-blue-100 dark:hover:bg-blue-900"
                >
                  ×
                </button>
              </li>
            ))}
          </ul>
        )}
      </form>

      <fieldset className="flex flex-col gap-2">
        <legend className="mb-2 text-sm font-medium">{t('jobAlerts.platforms')}</legend>
        {platforms.map((code) => (
          <label key={code} className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={selected.includes(code)}
              onChange={() => togglePlatform(code)}
              className="size-4 accent-blue-600"
            />
            {t(`platforms.${code}`)}
          </label>
        ))}
      </fieldset>

      {error && <Alert tone="error">{error.message}</Alert>}
      {save.isSuccess && <Alert tone="success">{t('jobAlerts.saved')}</Alert>}
      <Button loading={save.isPending} onClick={() => save.mutate()} className="self-start">
        {t('jobAlerts.save')}
      </Button>
    </div>
  );
}
