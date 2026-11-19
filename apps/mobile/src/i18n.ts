import { getLocales } from 'expo-localization';
import { createInstance } from 'i18next';
import { initReactI18next } from 'react-i18next';
import { defaultLanguage, defaultNamespace, resolveLanguage, resources, supportedLanguages } from '@fcc/i18n';

// The device language is used until the user's saved preference is loaded from the API.
const deviceLanguage = getLocales()[0]?.languageTag;

const i18n = createInstance();

void i18n.use(initReactI18next).init({
  resources,
  lng: resolveLanguage(deviceLanguage),
  fallbackLng: defaultLanguage,
  supportedLngs: [...supportedLanguages],
  defaultNS: defaultNamespace,
  interpolation: { escapeValue: false },
});

export default i18n;
