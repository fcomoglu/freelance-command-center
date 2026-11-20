import { describe, expect, it } from 'vitest';

import { formatBudget, formatMoney, formatRelativeTime } from './format';

// Intl separates amount and symbol with a no-break space where the locale puts one.
const nbsp = ' ';

describe('formatMoney', () => {
  it('shows whole amounts without decimals and others with two', () => {
    expect(formatMoney({ amount: '250.00', currency: 'USD' }, 'en')).toBe('$250');
    expect(formatMoney({ amount: '1500', currency: 'TRY' }, 'tr')).toBe('₺1.500');
    expect(formatMoney({ amount: '12.5', currency: 'EUR' }, 'de')).toBe(`12,50${nbsp}€`);
  });

  it('falls back to the raw amount for a currency Intl does not accept', () => {
    expect(formatMoney({ amount: '10', currency: 'not-a-code' }, 'en')).toBe('10 not-a-code');
  });
});

describe('formatRelativeTime', () => {
  const now = new Date('2026-11-21T12:00:00Z');
  const ago = (seconds: number) => new Date(now.getTime() - seconds * 1000);

  it('uses the largest unit that fits', () => {
    expect(formatRelativeTime(ago(30), now, 'en')).toBe('this minute');
    expect(formatRelativeTime(ago(5 * 60), now, 'en')).toBe('5 minutes ago');
    expect(formatRelativeTime(ago(3 * 3600), now, 'en')).toBe('3 hours ago');
    expect(formatRelativeTime(ago(26 * 3600), now, 'en')).toBe('yesterday');
    expect(formatRelativeTime(ago(400 * 24 * 3600), now, 'en')).toBe('last year');
  });

  it('speaks the requested language', () => {
    expect(formatRelativeTime(ago(5 * 60), now, 'tr')).toBe('5 dakika önce');
    expect(formatRelativeTime(ago(26 * 3600), now, 'tr')).toBe('dün');
  });
});

describe('formatBudget', () => {
  const translate = (key: string, values: Record<string, string>) =>
    key === 'jobs.budget.range' ? `${values.minimum} – ${values.maximum}` : `${values.amount}/hr`;

  it('shows a fixed price range and an hourly rate', () => {
    expect(
      formatBudget(
        {
          type: 'fixed',
          minimum: { amount: '250', currency: 'USD' },
          maximum: { amount: '750', currency: 'USD' },
        },
        'en',
        translate,
      ),
    ).toBe('$250 – $750');
    expect(
      formatBudget({ type: 'hourly', minimum: { amount: '25', currency: 'USD' } }, 'en', translate),
    ).toBe('$25/hr');
  });
});
