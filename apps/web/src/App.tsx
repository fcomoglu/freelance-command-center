import { useTranslation } from 'react-i18next';
import { supportedLanguages, type SupportedLanguage } from '@fcc/i18n';
import { changeLanguage } from './i18n';

const navigationKeys = ['jobs', 'proposals', 'platforms', 'profile', 'settings'] as const;

export default function App() {
  const { t, i18n } = useTranslation();

  return (
    <div className="app">
      <header className="app-header">
        <strong>{t('app.name')}</strong>
        <label className="language-select">
          {t('settings.language')}
          <select
            value={i18n.resolvedLanguage}
            onChange={(event) => void changeLanguage(event.target.value as SupportedLanguage)}
          >
            {supportedLanguages.map((language) => (
              <option key={language} value={language}>
                {t(`language.${language}`)}
              </option>
            ))}
          </select>
        </label>
      </header>
      <nav className="app-nav">
        {navigationKeys.map((key) => (
          <a key={key} href={`#${key}`}>
            {t(`nav.${key}`)}
          </a>
        ))}
      </nav>
      <main className="app-main">
        <h1>{t('app.name')}</h1>
        <p>{t('app.tagline')}</p>
      </main>
    </div>
  );
}
