import { unwrap } from '@fcc/api-client';
import { useMutation } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { StyleSheet, View } from 'react-native';

import { api } from '@/api';
import { Button, Notice } from '@/components/form';
import { ThemedText } from '@/components/themed-text';
import { Spacing } from '@/constants/theme';
import { describeError } from '@/problem';
import { disablePush } from '@/push';

/** Signed-in account: address, verification state and sign-out. */
export function AccountSection() {
  const { t } = useTranslation();
  const me = api.query.useQuery('get', '/api/v1/me');
  const resend = useMutation({
    mutationFn: () => unwrap(api.fetchClient.POST('/api/v1/me/email-verification')),
  });
  const signOut = useMutation({
    mutationFn: async () => {
      // Best effort: a phone left registered moves to the next user who signs in on it.
      await disablePush().catch(() => undefined);
      await api.session.signOut();
    },
  });

  return (
    <View style={styles.section}>
      {me.data && (
        <ThemedText themeColor="textSecondary">
          {t('account.signedInAs', { email: me.data.email })}
        </ThemedText>
      )}
      {me.data && !me.data.emailVerified && (
        <>
          <Notice tone="info">{t('account.unverified')}</Notice>
          {resend.isSuccess ? (
            <Notice tone="success">{t('account.resent')}</Notice>
          ) : (
            <Button
              variant="secondary"
              title={t('account.resend')}
              loading={resend.isPending}
              onPress={() => resend.mutate()}
            />
          )}
          {resend.isError && <Notice tone="error">{describeError(resend.error, t).message}</Notice>}
        </>
      )}
      <Button
        variant="secondary"
        title={t('account.signOut')}
        loading={signOut.isPending}
        onPress={() => signOut.mutate()}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  section: { gap: Spacing.two, marginTop: Spacing.four },
});
