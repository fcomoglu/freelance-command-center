import { unwrap } from '@fcc/api-client';
import { useQuery } from '@tanstack/react-query';
import { Link, useSearch } from '@tanstack/react-router';
import { useTranslation } from 'react-i18next';

import { api, queryClient } from '../api';
import { Alert, linkClass } from '../components/ui';
import { describeError } from '../problem';
import { AuthTitle } from './auth-layout';

export function VerifyEmailPage() {
  const { t } = useTranslation();
  const { token } = useSearch({ from: '/auth/verify-email' });
  // A query rather than a mutation: tokens work once, and queries are deduplicated when the page mounts twice.
  const verification = useQuery({
    queryKey: ['email-verification', token],
    queryFn: async () => {
      await unwrap(api.fetchClient.POST('/api/v1/auth/email-verification', { body: { token } }));
      await queryClient.invalidateQueries({ queryKey: ['get', '/api/v1/me'] });
      return true;
    },
    enabled: token !== '',
    retry: false,
    staleTime: Infinity,
    gcTime: Infinity,
  });

  return (
    <>
      <AuthTitle>{t('auth.verifyEmail.title')}</AuthTitle>
      {!token ? (
        <Alert tone="error">{t('errors.identity.invalid_token')}</Alert>
      ) : verification.isPending ? (
        <p className="text-sm text-gray-600 dark:text-gray-400">{t('auth.verifyEmail.verifying')}</p>
      ) : verification.isError ? (
        <Alert tone="error">{describeError(verification.error, t).message}</Alert>
      ) : (
        <Alert tone="success">{t('auth.verifyEmail.success')}</Alert>
      )}
      <p className="mt-6 text-sm">
        <Link to="/" className={linkClass}>
          {t('auth.verifyEmail.continue')}
        </Link>
      </p>
    </>
  );
}
