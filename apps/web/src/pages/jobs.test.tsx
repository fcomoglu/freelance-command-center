import { type components } from '@fcc/api-client';
import { cleanup, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { ok, renderScreen } from '../test-support';
import { JobsPage } from './jobs';

type Job = components['schemas']['Job'];

const api = vi.hoisted(() => ({ GET: vi.fn() }));
vi.mock('../api', () => ({ api: { fetchClient: api } }));

const fixedPrice: Job = {
  id: 'j-1',
  platform: 'freelancer',
  title: 'Spring Boot REST API',
  description: 'Build a REST API for an inventory system.',
  postedAt: new Date(Date.now() - 2 * 3600_000).toISOString(),
  url: 'https://www.freelancer.com/projects/java/spring-boot-rest-api',
  skills: ['Java', 'Spring Boot'],
  bidCount: 12,
  budget: {
    type: 'fixed',
    minimum: { amount: '250', currency: 'USD' },
    maximum: { amount: '750', currency: 'USD' },
  },
};
const hourly: Job = {
  id: 'j-2',
  platform: 'upwork',
  title: 'Symfony developer',
  description: '',
  postedAt: new Date(Date.now() - 10 * 60_000).toISOString(),
  url: 'https://www.upwork.com/jobs/~021',
  skills: [],
  budget: { type: 'hourly', minimum: { amount: '25', currency: 'USD' } },
};

afterEach(() => {
  cleanup();
  vi.resetAllMocks();
});

describe('JobsPage', () => {
  it('shows each job with its platform, budget, bids and skills', async () => {
    api.GET.mockResolvedValue(ok({ items: [fixedPrice, hourly], nextCursor: null }));
    await renderScreen(<JobsPage />);

    const [first, second] = await screen.findAllByRole('article');
    expect(within(first).getByRole('link', { name: 'Spring Boot REST API' }).getAttribute('href')).toBe(
      fixedPrice.url,
    );
    expect(within(first).getByText('$250 – $750')).toBeTruthy();
    expect(within(first).getByText('12 bids')).toBeTruthy();
    expect(within(first).getByText('2 hours ago')).toBeTruthy();
    expect(within(first).getByText('Spring Boot')).toBeTruthy();
    expect(within(second).getByText('Upwork')).toBeTruthy();
    expect(within(second).getByText('$25/hr')).toBeTruthy();
  });

  it('filters the feed by platform', async () => {
    api.GET.mockResolvedValue(ok({ items: [fixedPrice], nextCursor: null }));
    await renderScreen(<JobsPage />);
    await screen.findByText('Spring Boot REST API');

    await userEvent.selectOptions(screen.getByLabelText('Platform'), 'upwork');

    await vi.waitFor(() =>
      expect(api.GET).toHaveBeenLastCalledWith(
        '/api/v1/jobs',
        expect.objectContaining({
          params: { query: { limit: 20, platform: 'upwork', cursor: undefined } },
        }),
      ),
    );
  });

  it('explains an empty feed', async () => {
    api.GET.mockResolvedValue(ok({ items: [], nextCursor: null }));
    await renderScreen(<JobsPage />);

    expect(
      await screen.findByText('No jobs yet. New jobs from your platforms appear here within a few minutes.'),
    ).toBeTruthy();
  });
});
