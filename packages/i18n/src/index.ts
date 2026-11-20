import enCommon from '../locales/en/common.json';
import trCommon from '../locales/tr/common.json';

/** Languages the clients can display. `en` is the source and fallback language. */
export const supportedLanguages = ['en', 'tr'] as const;
export type SupportedLanguage = (typeof supportedLanguages)[number];

export const defaultLanguage: SupportedLanguage = 'en';
export const defaultNamespace = 'common';

/** i18next resources, keyed by language and namespace. */
export const resources = {
  en: { common: enCommon },
  tr: { common: trCommon },
} as const;

/** Returns a supported language for a BCP 47 tag such as `tr-TR`, falling back to the default language. */
export function resolveLanguage(tag: string | null | undefined): SupportedLanguage {
  const base = tag?.toLowerCase().split('-')[0];
  return supportedLanguages.find((language) => language === base) ?? defaultLanguage;
}

const hasKey = (object: unknown, path: string[]): boolean =>
  path.length === 0
    ? typeof object === 'string'
    : object !== null &&
      typeof object === 'object' &&
      hasKey((object as Record<string, unknown>)[path[0]], path.slice(1));

/** Translation key for an API error code such as `identity.invalid_token`; unknown codes get a generic message. */
export function errorKey(code: string): string {
  const key = `errors.${code}`;
  return hasKey(resources[defaultLanguage][defaultNamespace], key.split('.')) ? key : 'errors.unknown';
}

export { formatBudget, formatMoney, formatRelativeTime, type BudgetValue, type MoneyValue } from './format';
