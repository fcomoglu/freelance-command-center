import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import { api } from '@/api';
import SettingsScreen from '@/app/(app)/settings';
import i18n from '@/i18n';
import { disablePush, enablePush, pushEnabled } from '@/push';
import { ok, renderScreen } from '@/test-support';

jest.mock('@/api', () => ({
  api: {
    fetchClient: { GET: jest.fn(), PUT: jest.fn(), POST: jest.fn() },
    query: { useQuery: jest.fn() },
    session: { signOut: jest.fn() },
  },
}));
jest.mock('@/push', () => ({ pushEnabled: jest.fn(), enablePush: jest.fn(), disablePush: jest.fn() }));

const mocked = {
  fetchClient: jest.mocked(api.fetchClient),
  me: jest.mocked(api.query.useQuery),
  signOut: jest.mocked(api.session.signOut),
  pushEnabled: jest.mocked(pushEnabled),
  enablePush: jest.mocked(enablePush),
  disablePush: jest.mocked(disablePush),
};

function account(emailVerified: boolean) {
  mocked.me.mockReturnValue({ data: { email: 'ada@example.com', emailVerified } } as never);
}

beforeEach(() => {
  mocked.fetchClient.GET.mockResolvedValue(ok({ keywords: ['java'], platforms: ['freelancer'] }));
  mocked.pushEnabled.mockResolvedValue(false);
  account(true);
});

afterEach(() => {
  jest.clearAllMocks();
});

describe('SettingsScreen', () => {
  it('turns on notifications for this phone', async () => {
    mocked.enablePush.mockResolvedValue('enabled');
    await renderScreen(<SettingsScreen />);

    expect(await screen.findByText('Notifications on this phone are off.')).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Turn on notifications' }));

    expect(mocked.enablePush).toHaveBeenCalledWith('en', expect.any(Function));
  });

  it('explains notifications blocked in the phone settings', async () => {
    mocked.enablePush.mockResolvedValue('denied');
    await renderScreen(<SettingsScreen />);

    await fireEvent.press(await screen.findByRole('button', { name: 'Turn on notifications' }));

    expect(await screen.findByText(/Notifications are blocked/)).toBeOnTheScreen();
  });

  it('saves the job filter with an added keyword', async () => {
    mocked.fetchClient.PUT.mockImplementation((async (_path: string, { body }: { body: unknown }) =>
      ok(body)) as never);
    await renderScreen(<SettingsScreen />);

    await fireEvent.changeText(await screen.findByLabelText('Keywords'), ' Spring Boot ');
    await fireEvent.press(screen.getByRole('button', { name: 'Add' }));
    await fireEvent.press(screen.getByRole('button', { name: 'Save' }));

    expect(mocked.fetchClient.PUT).toHaveBeenCalledWith('/api/v1/job-filter', {
      body: { keywords: ['java', 'spring boot'], platforms: ['freelancer'] },
    });
    expect(await screen.findByText('Saved.')).toBeOnTheScreen();
  });

  it('switches the language', async () => {
    await renderScreen(<SettingsScreen />);

    await fireEvent.press(await screen.findByText('Turkish'));

    await waitFor(() => expect(i18n.resolvedLanguage).toBe('tr'));
    expect(await screen.findByText('Dil')).toBeOnTheScreen();
  });

  it('sends a new confirmation link for an unconfirmed address', async () => {
    account(false);
    mocked.fetchClient.POST.mockResolvedValue(ok());
    await renderScreen(<SettingsScreen />);

    await fireEvent.press(await screen.findByRole('button', { name: 'Send a new link' }));

    expect(mocked.fetchClient.POST).toHaveBeenCalledWith('/api/v1/me/email-verification');
    expect(await screen.findByText('A new confirmation link has been sent.')).toBeOnTheScreen();
  });

  it('stops push to this phone before signing out', async () => {
    const order: string[] = [];
    mocked.disablePush.mockImplementation(async () => void order.push('push'));
    mocked.signOut.mockImplementation(async () => void order.push('session'));
    await renderScreen(<SettingsScreen />);

    await fireEvent.press(await screen.findByRole('button', { name: 'Sign out' }));

    await waitFor(() => expect(order).toEqual(['push', 'session']));
  });
});
