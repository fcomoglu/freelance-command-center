import { unwrap } from '@fcc/api-client';
import { useMutation } from '@tanstack/react-query';
import { Link } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import { api } from '@/api';
import { AuthScreen } from '@/components/auth-screen';
import { Button, Notice, TextField } from '@/components/form';
import { ThemedText } from '@/components/themed-text';
import { describeError } from '@/problem';

/** Requests the reset e-mail; its link opens the web app, where the new password is set. */
export default function ForgotPasswordScreen() {
  const { t } = useTranslation();
  const [email, setEmail] = useState('');
  const request = useMutation({
    mutationFn: () =>
      unwrap(api.fetchClient.POST('/api/v1/auth/password-reset', { body: { email: email.trim() } })),
  });
  const error = request.isError ? describeError(request.error, t) : undefined;

  return (
    <AuthScreen title={t('auth.forgotPassword.title')}>
      {request.isSuccess ? (
        <Notice tone="success">{t('auth.forgotPassword.sent', { email: email.trim() })}</Notice>
      ) : (
        <>
          <ThemedText themeColor="textSecondary">{t('auth.forgotPassword.intro')}</ThemedText>
          {error && <Notice tone="error">{error.message}</Notice>}
          <TextField
            label={t('auth.fields.email')}
            value={email}
            onChangeText={setEmail}
            autoCapitalize="none"
            autoComplete="email"
            keyboardType="email-address"
            error={error?.fields.email}
          />
          <Button
            title={t('auth.forgotPassword.submit')}
            loading={request.isPending}
            onPress={() => request.mutate()}
          />
        </>
      )}
      <Link href="/sign-in">
        <ThemedText type="linkPrimary">{t('auth.backToSignIn')}</ThemedText>
      </Link>
    </AuthScreen>
  );
}
