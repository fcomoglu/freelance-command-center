// Fails when a translation key exists in one locale but not in another (see docs/adr/0005).
import { readdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';

const localesDir = new URL('../locales/', import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1');
const languages = readdirSync(localesDir);
const sourceLanguage = 'en';

const flatten = (object, prefix = '') =>
  Object.entries(object).flatMap(([key, value]) =>
    value !== null && typeof value === 'object' ? flatten(value, `${prefix}${key}.`) : [`${prefix}${key}`],
  );

const keysOf = (language, namespace) =>
  new Set(flatten(JSON.parse(readFileSync(join(localesDir, language, namespace), 'utf8'))));

let problems = 0;
for (const namespace of readdirSync(join(localesDir, sourceLanguage))) {
  const sourceKeys = keysOf(sourceLanguage, namespace);
  for (const language of languages.filter((l) => l !== sourceLanguage)) {
    const keys = keysOf(language, namespace);
    for (const key of sourceKeys)
      if (!keys.has(key)) (problems++, console.error(`[${language}/${namespace}] missing: ${key}`));
    for (const key of keys)
      if (!sourceKeys.has(key)) (problems++, console.error(`[${language}/${namespace}] unknown: ${key}`));
  }
}

if (problems > 0) process.exit(1);
console.log(`Locales are consistent: ${languages.join(', ')}`);
