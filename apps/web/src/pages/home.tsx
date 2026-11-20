import { useTranslation } from 'react-i18next';

export function HomePage() {
  const { t } = useTranslation();
  return (
    <>
      <h1 className="text-2xl font-semibold">{t('app.name')}</h1>
      <p className="mt-2 text-gray-600 dark:text-gray-400">{t('app.tagline')}</p>
    </>
  );
}
