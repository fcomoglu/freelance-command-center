import { unwrap } from '@fcc/api-client';
import { resolveLanguage } from '@fcc/i18n';
import { useMutation } from '@tanstack/react-query';
import { Link } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import { api } from '@/api';
import { AuthScreen } from '@/components/auth-screen';
import { Button, Notice, TextField } from '@/components/form';
import { ThemedText } from '@/components/themed-text';
import { describeError } from '@/problem';

export default function SignUpScreen() {
  const { t, i18n } = useTranslation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const signUp = useMutation({
    mutationFn: () =>
      unwrap(
        api.fetchClient.POST('/api/v1/auth/sign-up', {
          body: { email: email.trim(), password, language: resolveLanguage(i18n.resolvedLanguage) },
        }),
      ),
  });
  const error = signUp.isError ? describeError(signUp.error, t) : undefined;

  if (signUp.isSuccess) {
    return (
      <AuthScreen title={t('auth.signUp.checkEmailTitle')}>
        <Notice tone="success">{t('auth.signUp.checkEmail', { email: email.trim() })}</Notice>
        <Link href="/sign-in">
          <ThemedText type="linkPrimary">{t('auth.backToSignIn')}</ThemedText>
        </Link>
      </AuthScreen>
    );
  }

  return (
    <AuthScreen title={t('auth.signUp.title')}>
      {error && <Notice tone="error">{error.message}</Notice>}
      <TextField
        label={t('auth.fields.email')}
        value={email}
        onChangeText={setEmail}
        autoCapitalize="none"
        autoComplete="email"
        keyboardType="email-address"
        textContentType="username"
        error={error?.fields.email}
      />
      <TextField
        label={t('auth.fields.password')}
        value={password}
        onChangeText={setPassword}
        secureTextEntry
        autoComplete="new-password"
        textContentType="newPassword"
        maxLength={128}
        hint={t('auth.fields.passwordHint')}
        error={error?.fields.password}
      />
      <Button title={t('auth.signUp.submit')} loading={signUp.isPending} onPress={() => signUp.mutate()} />
      <Link href="/sign-in">
        <ThemedText type="small">
          {t('auth.signUp.haveAccount')} <ThemedText type="linkPrimary">{t('auth.signUp.signIn')}</ThemedText>
        </ThemedText>
      </Link>
    </AuthScreen>
  );
}
