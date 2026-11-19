import { unwrap } from '@fcc/api-client';
import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import { api } from '../api';
import { Alert, Button, TextField } from '../components/ui';
import { describeError } from '../problem';
import { AuthTitle, BackToSignIn } from './auth-layout';

export function ForgotPasswordPage() {
  const { t } = useTranslation();
  const [email, setEmail] = useState('');
  const request = useMutation({
    mutationFn: () => unwrap(api.fetchClient.POST('/api/v1/auth/password-reset', { body: { email } })),
  });
  const error = request.isError ? describeError(request.error, t) : undefined;

  return (
    <>
      <AuthTitle>{t('auth.forgotPassword.title')}</AuthTitle>
      {request.isSuccess ? (
        <Alert tone="success">{t('auth.forgotPassword.sent', { email })}</Alert>
      ) : (
        <form
          className="flex flex-col gap-4"
          onSubmit={(event) => {
            event.preventDefault();
            request.mutate();
          }}
        >
          <p className="text-sm text-gray-600 dark:text-gray-400">{t('auth.forgotPassword.intro')}</p>
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
          <Button type="submit" loading={request.isPending}>
            {t('auth.forgotPassword.submit')}
          </Button>
        </form>
      )}
      <BackToSignIn />
    </>
  );
}
