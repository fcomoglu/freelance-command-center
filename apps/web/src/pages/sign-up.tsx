import { unwrap } from '@fcc/api-client';
import { resolveLanguage } from '@fcc/i18n';
import { useMutation } from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import { api } from '../api';
import { Alert, Button, linkClass, TextField } from '../components/ui';
import { describeError } from '../problem';
import { AuthTitle, BackToSignIn } from './auth-layout';

export function SignUpPage() {
  const { t, i18n } = useTranslation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const signUp = useMutation({
    mutationFn: () =>
      unwrap(
        api.fetchClient.POST('/api/v1/auth/sign-up', {
          body: { email, password, language: resolveLanguage(i18n.resolvedLanguage) },
        }),
      ),
  });
  const error = signUp.isError ? describeError(signUp.error, t) : undefined;

  if (signUp.isSuccess) {
    return (
      <>
        <AuthTitle>{t('auth.signUp.checkEmailTitle')}</AuthTitle>
        <Alert tone="success">{t('auth.signUp.checkEmail', { email })}</Alert>
        <BackToSignIn />
      </>
    );
  }

  return (
    <>
      <AuthTitle>{t('auth.signUp.title')}</AuthTitle>
      <form
        className="flex flex-col gap-4"
        onSubmit={(event) => {
          event.preventDefault();
          signUp.mutate();
        }}
      >
        {error && <Alert tone="error">{error.message}</Alert>}
        <TextField
          label={t('auth.fields.email')}
          type="email"
          autoComplete="email"
          required
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          error={error?.fields.email}
        />
        <TextField
          label={t('auth.fields.password')}
          type="password"
          autoComplete="new-password"
          required
          minLength={10}
          maxLength={128}
          hint={t('auth.fields.passwordHint')}
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          error={error?.fields.password}
        />
        <Button type="submit" loading={signUp.isPending}>
          {t('auth.signUp.submit')}
        </Button>
      </form>
      <p className="mt-6 text-sm">
        {t('auth.signUp.haveAccount')}{' '}
        <Link to="/sign-in" className={linkClass}>
          {t('auth.signUp.signIn')}
        </Link>
      </p>
    </>
  );
}
