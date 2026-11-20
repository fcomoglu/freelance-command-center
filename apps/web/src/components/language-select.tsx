import { supportedLanguages, type SupportedLanguage } from '@fcc/i18n';
import { useTranslation } from 'react-i18next';

import { changeLanguage } from '../i18n';

export function LanguageSelect() {
  const { t, i18n } = useTranslation();
  return (
    <label className="flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400">
      {t('settings.language')}
      <select
        value={i18n.resolvedLanguage}
        onChange={(event) => void changeLanguage(event.target.value as SupportedLanguage)}
        className="rounded-md border border-gray-300 bg-white px-2 py-1 text-gray-900 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-100"
      >
        {supportedLanguages.map((language) => (
          <option key={language} value={language}>
            {t(`language.${language}`)}
          </option>
        ))}
      </select>
    </label>
  );
}
