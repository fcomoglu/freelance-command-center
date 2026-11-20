import { useMutation } from '@tanstack/react-query';
import { Link } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import { api } from '@/api';
import { AuthScreen } from '@/components/auth-screen';
import { Button, Notice, TextField } from '@/components/form';
import { ThemedText } from '@/components/themed-text';
import { describeError } from '@/problem';

/** Entry screen when signed out; the root guard switches to the app once the session starts. */
export default function SignInScreen() {
  const { t } = useTranslation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const signIn = useMutation({ mutationFn: () => api.session.signIn(email.trim(), password) });
  const error = signIn.isError ? describeError(signIn.error, t) : undefined;

  return (
    <AuthScreen title={t('auth.signIn.title')}>
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
        autoCapitalize="none"
        autoCorrect={false}
        autoComplete="current-password"
        textContentType="password"
        error={error?.fields.password}
      />
      <Button title={t('auth.signIn.submit')} loading={signIn.isPending} onPress={() => signIn.mutate()} />
      <Link href="/forgot-password">
        <ThemedText type="linkPrimary">{t('auth.signIn.forgotPassword')}</ThemedText>
      </Link>
      <Link href="/sign-up">
        <ThemedText type="small">
          {t('auth.signIn.noAccount')}{' '}
          <ThemedText type="linkPrimary">{t('auth.signIn.createAccount')}</ThemedText>
        </ThemedText>
      </Link>
    </AuthScreen>
  );
}
