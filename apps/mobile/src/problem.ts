import { ApiError } from '@fcc/api-client';
import { errorKey } from '@fcc/i18n';
import type { TFunction } from 'i18next';

/** Translated message and per-field messages for a failed API call. */
export function describeError(
  error: unknown,
  t: TFunction,
): { message: string; fields: Record<string, string> } {
  if (!(error instanceof ApiError)) return { message: t('errors.unknown'), fields: {} };
  const fields = Object.fromEntries(
    (error.problem.errors ?? []).map(({ field, code }) => [field, t(errorKey(code))]),
  );
  return { message: t(errorKey(error.problem.code)), fields };
}
