import { Link, Outlet } from '@tanstack/react-router';
import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';

import { LanguageSelect } from '../components/language-select';
import { linkClass } from '../components/ui';

export function AuthLayout() {
  const { t } = useTranslation();
  return (
    <div className="flex min-h-screen flex-col">
      <header className="flex items-center justify-between px-6 py-4">
        <span className="font-semibold">{t('app.name')}</span>
        <LanguageSelect />
      </header>
      <main className="flex flex-1 justify-center px-4 pt-12 pb-16">
        <div className="h-fit w-full max-w-sm rounded-xl border border-gray-200 bg-white p-6 shadow-sm dark:border-gray-800 dark:bg-gray-900">
          <Outlet />
        </div>
      </main>
    </div>
  );
}

export function AuthTitle({ children }: { children: ReactNode }) {
  return <h1 className="mb-6 text-xl font-semibold">{children}</h1>;
}

export function BackToSignIn() {
  const { t } = useTranslation();
  return (
    <p className="mt-6 text-sm">
      <Link to="/sign-in" className={linkClass}>
        {t('auth.backToSignIn')}
      </Link>
    </p>
  );
}
