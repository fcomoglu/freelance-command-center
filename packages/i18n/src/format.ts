/** Locale-aware formatting shared by the clients. */

/** The API's money representation: a decimal string and an ISO 4217 code. */
export interface MoneyValue {
  amount: string;
  currency: string;
}

/** `$250`, `₺1.500`, `12,50 €`; whole amounts without decimals. */
export function formatMoney({ amount, currency }: MoneyValue, language: string): string {
  const value = Number(amount);
  const fractionDigits = Number.isInteger(value) ? 0 : 2;
  try {
    return new Intl.NumberFormat(language, {
      style: 'currency',
      currency,
      minimumFractionDigits: fractionDigits,
      maximumFractionDigits: fractionDigits,
    }).format(value);
  } catch {
    return `${amount} ${currency}`;
  }
}

const relativeUnits: [Intl.RelativeTimeFormatUnit, number][] = [
  ['year', 365 * 24 * 3600],
  ['month', 30 * 24 * 3600],
  ['week', 7 * 24 * 3600],
  ['day', 24 * 3600],
  ['hour', 3600],
  ['minute', 60],
];

/** `5 minutes ago`, `yesterday`; an absolute date where the runtime has no `Intl.RelativeTimeFormat`. */
export function formatRelativeTime(date: Date, now: Date, language: string): string {
  if (typeof Intl.RelativeTimeFormat !== 'function') return date.toLocaleString(language);
  const seconds = Math.round((date.getTime() - now.getTime()) / 1000);
  const format = new Intl.RelativeTimeFormat(language, { numeric: 'auto' });
  for (const [unit, size] of relativeUnits) {
    if (Math.abs(seconds) >= size) return format.format(Math.trunc(seconds / size), unit);
  }
  return format.format(0, 'minute');
}

/** The API's job budget: a fixed price or an hourly rate, as a range when `maximum` is set. */
export interface BudgetValue {
  type: 'fixed' | 'hourly';
  minimum: MoneyValue;
  maximum?: MoneyValue;
}

/** `$250 – $750`, `$25/hr`; uses the `jobs.budget.*` translations. */
export function formatBudget(
  budget: BudgetValue,
  language: string,
  translate: (key: string, values: Record<string, string>) => string,
): string {
  const minimum = formatMoney(budget.minimum, language);
  const amount = budget.maximum
    ? translate('jobs.budget.range', { minimum, maximum: formatMoney(budget.maximum, language) })
    : minimum;
  return budget.type === 'hourly' ? translate('jobs.budget.hourly', { amount }) : amount;
}
