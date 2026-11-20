import { unwrap } from '@fcc/api-client';
import { useMutation } from '@tanstack/react-query';
import { Link, Outlet } from '@tanstack/react-router';
import { useTranslation } from 'react-i18next';

import { api } from '../api';
import { LanguageSelect } from '../components/language-select';
import { Alert, Button } from '../components/ui';
import { describeError } from '../problem';

export function AppLayout() {
  const { t } = useTranslation();
  const me = api.query.useQuery('get', '/api/v1/me');
  const signOut = useMutation({ mutationFn: () => api.session.signOut() });

  return (
    <div className="min-h-screen">
      <header className="flex flex-wrap items-center justify-between gap-4 border-b border-gray-200 bg-white px-6 py-3 dark:border-gray-800 dark:bg-gray-900">
        <strong>{t('app.name')}</strong>
        <div className="flex flex-wrap items-center gap-4">
          <LanguageSelect />
          {me.data && (
            <span className="text-sm text-gray-500 dark:text-gray-400">
              {t('account.signedInAs', { email: me.data.email })}
            </span>
          )}
          <Button variant="secondary" loading={signOut.isPending} onClick={() => signOut.mutate()}>
            {t('account.signOut')}
          </Button>
        </div>
      </header>
      <nav className="flex gap-4 border-b border-gray-200 px-6 py-2 text-sm dark:border-gray-800">
        <Link
          to="/"
          className="text-gray-600 hover:underline dark:text-gray-400"
          activeProps={{ className: 'font-semibold text-gray-900 dark:text-gray-100' }}
        >
          {t('nav.jobs')}
        </Link>
        <Link
          to="/messages"
          className="text-gray-600 hover:underline dark:text-gray-400"
          activeProps={{ className: 'font-semibold text-gray-900 dark:text-gray-100' }}
        >
          {t('nav.messages')}
        </Link>
        <Link
          to="/job-alerts"
          className="text-gray-600 hover:underline dark:text-gray-400"
          activeProps={{ className: 'font-semibold text-gray-900 dark:text-gray-100' }}
        >
          {t('nav.jobAlerts')}
        </Link>
        <Link
          to="/platform-mail"
          className="text-gray-600 hover:underline dark:text-gray-400"
          activeProps={{ className: 'font-semibold text-gray-900 dark:text-gray-100' }}
        >
          {t('nav.platformMail')}
        </Link>
      </nav>
      {me.data && !me.data.emailVerified && <VerifyEmailBanner />}
      <main className="px-6 py-8">
        <Outlet />
      </main>
    </div>
  );
}

function VerifyEmailBanner() {
  const { t } = useTranslation();
  const resend = useMutation({
    mutationFn: () => unwrap(api.fetchClient.POST('/api/v1/me/email-verification')),
  });

  return (
    <div className="mx-6 mt-4 flex flex-col gap-2">
      <Alert tone="info">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <span>{t('account.unverified')}</span>
          {!resend.isSuccess && (
            <Button variant="secondary" loading={resend.isPending} onClick={() => resend.mutate()}>
              {t('account.resend')}
            </Button>
          )}
        </div>
      </Alert>
      {resend.isSuccess && <Alert tone="success">{t('account.resent')}</Alert>}
      {resend.isError && <Alert tone="error">{describeError(resend.error, t).message}</Alert>}
    </div>
  );
}
