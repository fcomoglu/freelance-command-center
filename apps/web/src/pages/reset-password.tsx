import { unwrap } from '@fcc/api-client';
import { useMutation } from '@tanstack/react-query';
import { useSearch } from '@tanstack/react-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import { api } from '../api';
import { Alert, Button, TextField } from '../components/ui';
import { describeError } from '../problem';
import { AuthTitle, BackToSignIn } from './auth-layout';

export function ResetPasswordPage() {
  const { t } = useTranslation();
  const { token } = useSearch({ from: '/auth/reset-password' });
  const [newPassword, setNewPassword] = useState('');
  const reset = useMutation({
    mutationFn: async () => {
      await unwrap(
        api.fetchClient.POST('/api/v1/auth/password-reset/confirmation', { body: { token, newPassword } }),
      );
      // The API has revoked every session; drop this browser's one too.
      if (api.session.getStatus() === 'signedIn') await api.session.signOut();
    },
  });
  const error = reset.isError ? describeError(reset.error, t) : undefined;

  return (
    <>
      <AuthTitle>{t('auth.resetPassword.title')}</AuthTitle>
      {!token ? (
        <Alert tone="error">{t('auth.resetPassword.missingToken')}</Alert>
      ) : reset.isSuccess ? (
        <Alert tone="success">{t('auth.resetPassword.success')}</Alert>
      ) : (
        <form
          className="flex flex-col gap-4"
          onSubmit={(event) => {
            event.preventDefault();
            reset.mutate();
          }}
        >
          {error && <Alert tone="error">{error.message}</Alert>}
          <TextField
            label={t('auth.fields.newPassword')}
            type="password"
            autoComplete="new-password"
            required
            minLength={10}
            maxLength={128}
            hint={t('auth.fields.passwordHint')}
            value={newPassword}
            onChange={(event) => setNewPassword(event.target.value)}
            error={error?.fields.newPassword}
          />
          <Button type="submit" loading={reset.isPending}>
            {t('auth.resetPassword.submit')}
          </Button>
        </form>
      )}
      <BackToSignIn />
    </>
  );
}
