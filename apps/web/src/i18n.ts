import { createInstance } from 'i18next';
import { initReactI18next } from 'react-i18next';
import {
  defaultLanguage,
  defaultNamespace,
  resolveLanguage,
  resources,
  supportedLanguages,
  type SupportedLanguage,
} from '@fcc/i18n';

const STORAGE_KEY = 'fcc.language';

function readStoredLanguage(): string | null {
  try {
    return window.localStorage.getItem(STORAGE_KEY);
  } catch {
    return null;
  }
}

/** Persists the user's language choice and switches the UI to it. */
export function changeLanguage(language: SupportedLanguage): Promise<unknown> {
  try {
    window.localStorage.setItem(STORAGE_KEY, language);
  } catch {
    // Storage can be unavailable (private mode); the choice then lasts for this session only.
  }
  document.documentElement.lang = language;
  return i18n.changeLanguage(language);
}

const initialLanguage = resolveLanguage(readStoredLanguage() ?? navigator.language);
document.documentElement.lang = initialLanguage;

const i18n = createInstance();

void i18n.use(initReactI18next).init({
  resources,
  lng: initialLanguage,
  fallbackLng: defaultLanguage,
  supportedLngs: [...supportedLanguages],
  defaultNS: defaultNamespace,
  interpolation: { escapeValue: false },
});

export default i18n;
