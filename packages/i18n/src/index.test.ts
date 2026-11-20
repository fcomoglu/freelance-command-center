import { describe, expect, it } from 'vitest';

import { errorKey, resolveLanguage } from './index';

describe('resolveLanguage', () => {
  it('maps a regional tag to its supported base language', () => {
    expect(resolveLanguage('tr-TR')).toBe('tr');
    expect(resolveLanguage('EN-gb')).toBe('en');
  });

  it('falls back to English for unsupported or missing tags', () => {
    expect(resolveLanguage('de-DE')).toBe('en');
    expect(resolveLanguage(null)).toBe('en');
    expect(resolveLanguage(undefined)).toBe('en');
  });
});

describe('errorKey', () => {
  it('finds the translation of a known API error code', () => {
    expect(errorKey('identity.invalid_token')).toBe('errors.identity.invalid_token');
    expect(errorKey('platform.email_not_verified')).toBe('errors.platform.email_not_verified');
  });

  it('uses the generic message for an unknown code or a code that names a group', () => {
    expect(errorKey('identity.something_new')).toBe('errors.unknown');
    expect(errorKey('identity')).toBe('errors.unknown');
  });
});
