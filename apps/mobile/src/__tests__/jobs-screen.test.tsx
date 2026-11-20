import type { components } from '@fcc/api-client';
import { fireEvent, screen, waitFor } from '@testing-library/react-native';
import { Linking } from 'react-native';

import { api } from '@/api';
import JobsScreen from '@/app/(app)/index';
import { ok, renderScreen } from '@/test-support';

type Job = components['schemas']['Job'];

jest.mock('@/api', () => ({ api: { fetchClient: { GET: jest.fn() } } }));

const fetchClient = jest.mocked(api.fetchClient);

const job: Job = {
  id: 'j-1',
  platform: 'freelancer',
  title: 'Spring Boot REST API',
  description: 'Build a REST API for an inventory system.',
  postedAt: new Date(Date.now() - 2 * 3600_000).toISOString(),
  url: 'https://www.freelancer.com/projects/java/spring-boot-rest-api',
  skills: ['Java', 'Spring Boot'],
  bidCount: 12,
  budget: { type: 'hourly', minimum: { amount: '25', currency: 'USD' } },
};

beforeEach(() => {
  jest.spyOn(Linking, 'openURL').mockResolvedValue(undefined);
  fetchClient.GET.mockResolvedValue(ok({ items: [job], nextCursor: null }));
});

afterEach(() => {
  jest.restoreAllMocks();
  jest.clearAllMocks();
});

describe('JobsScreen', () => {
  it('shows a job with its platform, age, bids, budget and skills', async () => {
    await renderScreen(<JobsScreen />);

    expect(await screen.findByText('Spring Boot REST API')).toBeOnTheScreen();
    expect(screen.getByText('Freelancer.com · 2 hours ago · 12 bids')).toBeOnTheScreen();
    expect(screen.getByText('$25/hr')).toBeOnTheScreen();
    expect(screen.getByText('Java · Spring Boot')).toBeOnTheScreen();
  });

  it('opens a tapped job on its platform', async () => {
    await renderScreen(<JobsScreen />);

    await fireEvent.press(await screen.findByText('Spring Boot REST API'));

    expect(Linking.openURL).toHaveBeenCalledWith(job.url);
  });

  it('filters the feed by platform', async () => {
    await renderScreen(<JobsScreen />);
    await screen.findByText('Spring Boot REST API');

    await fireEvent.press(screen.getByRole('button', { name: 'Upwork' }));

    await waitFor(() =>
      expect(fetchClient.GET).toHaveBeenLastCalledWith(
        '/api/v1/jobs',
        expect.objectContaining({ params: { query: { limit: 20, platform: 'upwork', cursor: undefined } } }),
      ),
    );
  });
});
