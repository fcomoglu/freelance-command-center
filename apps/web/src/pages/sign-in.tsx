import { useMutation } from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import { api } from '../api';
import { Alert, Button, linkClass, TextField } from '../components/ui';
import { describeError } from '../problem';
import { AuthTitle } from './auth-layout';

export function SignInPage() {
  const { t } = useTranslation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const signIn = useMutation({ mutationFn: () => api.session.signIn(email, password) });
  const error = signIn.isError ? describeError(signIn.error, t) : undefined;

  return (
    <>
      <AuthTitle>{t('auth.signIn.title')}</AuthTitle>
      <form
        className="flex flex-col gap-4"
        onSubmit={(event) => {
          event.preventDefault();
          signIn.mutate();
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
          autoComplete="current-password"
          required
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          error={error?.fields.password}
        />
        <Button type="submit" loading={signIn.isPending}>
          {t('auth.signIn.submit')}
        </Button>
      </form>
      <div className="mt-6 flex flex-col gap-2 text-sm">
        <Link to="/forgot-password" className={linkClass}>
          {t('auth.signIn.forgotPassword')}
        </Link>
        <p>
          {t('auth.signIn.noAccount')}{' '}
          <Link to="/sign-up" className={linkClass}>
            {t('auth.signIn.createAccount')}
          </Link>
        </p>
      </div>
    </>
  );
}
