import { type components, unwrap } from '@fcc/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Pressable, StyleSheet, Switch, View } from 'react-native';

import { api } from '@/api';
import { Button, Notice, TextField } from '@/components/form';
import { ThemedText } from '@/components/themed-text';
import { Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { describeError } from '@/problem';
import { disablePush, enablePush, pushEnabled, type PushResult } from '@/push';

type JobFilter = components['schemas']['JobFilter'];
type Platform = JobFilter['platforms'][number];

const platforms: Platform[] = ['freelancer', 'upwork', 'fiverr', 'bionluk', 'arc_dev'];
const MAX_KEYWORDS = 20;
const MAX_KEYWORD_LENGTH = 50;
const filterKey = ['job-filter'];
const pushKey = ['push-enabled'];

/** Push on this phone, and the workspace's job filter that decides which new jobs are pushed. */
export function JobAlertsSection() {
  const { t } = useTranslation();
  const filter = useQuery({
    queryKey: filterKey,
    queryFn: ({ signal }) => unwrap(api.fetchClient.GET('/api/v1/job-filter', { signal })),
  });

  return (
    <View style={styles.section}>
      <ThemedText type="subtitle">{t('jobAlerts.title')}</ThemedText>
      <ThemedText themeColor="textSecondary">{t('jobAlerts.intro')}</ThemedText>
      <PushToggle />
      {filter.isError && <Notice tone="error">{describeError(filter.error, t).message}</Notice>}
      {filter.data && <JobFilterForm initial={filter.data} />}
    </View>
  );
}

function PushToggle() {
  const { t, i18n } = useTranslation();
  const queryClient = useQueryClient();
  const enabled = useQuery({ queryKey: pushKey, queryFn: pushEnabled });
  const [result, setResult] = useState<PushResult>();
  const toggle = useMutation({
    mutationFn: async (on: boolean) => {
      if (!on) {
        await disablePush();
        return undefined;
      }
      return enablePush(i18n.resolvedLanguage ?? 'en', t);
    },
    onSuccess: (outcome) => {
      setResult(outcome);
      void queryClient.invalidateQueries({ queryKey: pushKey });
    },
  });

  if (enabled.data === undefined) return null;
  return (
    <View style={styles.group}>
      <Notice tone={enabled.data ? 'success' : 'info'}>
        {t(enabled.data ? 'jobAlerts.pushOn' : 'jobAlerts.pushOff')}
      </Notice>
      {result === 'denied' && <Notice tone="error">{t('jobAlerts.pushDenied')}</Notice>}
      {result === 'unsupported' && <Notice tone="error">{t('jobAlerts.pushUnsupported')}</Notice>}
      {toggle.isError && <Notice tone="error">{describeError(toggle.error, t).message}</Notice>}
      <Button
        variant={enabled.data ? 'secondary' : 'primary'}
        title={t(enabled.data ? 'jobAlerts.disablePush' : 'jobAlerts.enablePush')}
        loading={toggle.isPending}
        onPress={() => toggle.mutate(!enabled.data)}
      />
    </View>
  );
}

/** Edits a copy of the saved filter; mounted once the filter has loaded. */
function JobFilterForm({ initial }: { initial: JobFilter }) {
  const { t } = useTranslation();
  const theme = useTheme();
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

  const term = draft.trim().replace(/\s+/g, ' ').toLowerCase();
  const canAdd = term.length > 0 && !keywords.includes(term) && keywords.length < MAX_KEYWORDS;

  function addKeyword() {
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
    <View style={styles.group}>
      <TextField
        label={t('jobAlerts.keywords')}
        hint={t('jobAlerts.keywordsHint', { used: keywords.length, max: MAX_KEYWORDS })}
        placeholder={t('jobAlerts.keywordPlaceholder')}
        value={draft}
        maxLength={MAX_KEYWORD_LENGTH}
        autoCapitalize="none"
        autoCorrect={false}
        returnKeyType="done"
        submitBehavior="submit"
        onChangeText={setDraft}
        onSubmitEditing={addKeyword}
      />
      {canAdd && <Button variant="secondary" title={t('jobAlerts.add')} onPress={addKeyword} />}
      {keywords.length === 0 ? (
        <Notice tone="info">{t('jobAlerts.noKeywords')}</Notice>
      ) : (
        <View style={styles.chips}>
          {keywords.map((keyword) => (
            <Pressable
              key={keyword}
              accessibilityRole="button"
              accessibilityLabel={t('jobAlerts.remove', { keyword })}
              onPress={() => {
                setKeywords(keywords.filter((k) => k !== keyword));
                save.reset();
              }}
              style={[styles.chip, { backgroundColor: theme.backgroundSelected }]}
            >
              <ThemedText type="small">{keyword} ×</ThemedText>
            </Pressable>
          ))}
        </View>
      )}

      <ThemedText type="smallBold">{t('jobAlerts.platforms')}</ThemedText>
      {platforms.map((code) => (
        <View key={code} style={styles.platform}>
          <ThemedText>{t(`platforms.${code}`)}</ThemedText>
          <Switch
            accessibilityLabel={t(`platforms.${code}`)}
            value={selected.includes(code)}
            onValueChange={() => togglePlatform(code)}
          />
        </View>
      ))}

      {save.isError && <Notice tone="error">{describeError(save.error, t).message}</Notice>}
      {save.isSuccess && <Notice tone="success">{t('jobAlerts.saved')}</Notice>}
      <Button title={t('jobAlerts.save')} loading={save.isPending} onPress={() => save.mutate()} />
    </View>
  );
}

const styles = StyleSheet.create({
  section: { gap: Spacing.two },
  group: { gap: Spacing.two },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: Spacing.one },
  chip: { borderRadius: 999, paddingHorizontal: Spacing.two, paddingVertical: Spacing.one },
  platform: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
});
