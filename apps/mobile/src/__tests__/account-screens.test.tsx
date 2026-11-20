import { fireEvent, screen } from '@testing-library/react-native';
import type { ReactNode } from 'react';

import { api } from '@/api';
import ForgotPasswordScreen from '@/app/(auth)/forgot-password';
import SignUpScreen from '@/app/(auth)/sign-up';
import { ok, renderScreen } from '@/test-support';

jest.mock('@/api', () => ({ api: { fetchClient: { POST: jest.fn() } } }));
jest.mock('expo-router', () => ({ Link: ({ children }: { children: ReactNode }) => children }));

const POST = jest.mocked(api.fetchClient.POST);

afterEach(() => {
  jest.clearAllMocks();
});

describe('SignUpScreen', () => {
  async function submit(email: string, password: string) {
    await fireEvent.changeText(screen.getByLabelText('E-mail'), email);
    await fireEvent.changeText(screen.getByLabelText('Password'), password);
    await fireEvent.press(screen.getByRole('button', { name: 'Create account' }));
  }

  it('creates the account with the trimmed address and asks to confirm it', async () => {
    POST.mockResolvedValue(ok({}) as never);
    await renderScreen(<SignUpScreen />);

    await submit(' ada@example.com ', 'correct horse battery');

    expect(POST).toHaveBeenCalledWith('/api/v1/auth/sign-up', {
      body: { email: 'ada@example.com', password: 'correct horse battery', language: 'en' },
    });
    expect(await screen.findByText('Check your inbox')).toBeOnTheScreen();
  });

  it('shows the translated reason when the API refuses', async () => {
    POST.mockResolvedValue({
      error: { code: 'identity.invalid_email' },
      response: new Response(null, { status: 400 }),
    } as never);
    await renderScreen(<SignUpScreen />);

    await submit('ada@', 'correct horse battery');

    expect(await screen.findByText('Enter a valid e-mail address.')).toBeOnTheScreen();
    expect(screen.queryByText('Check your inbox')).toBeNull();
  });
});

describe('ForgotPasswordScreen', () => {
  it('requests the reset link without revealing whether the account exists', async () => {
    POST.mockResolvedValue(ok() as never);
    await renderScreen(<ForgotPasswordScreen />);

    await fireEvent.changeText(screen.getByLabelText('E-mail'), 'ada@example.com ');
    await fireEvent.press(screen.getByRole('button', { name: 'Send link' }));

    expect(POST).toHaveBeenCalledWith('/api/v1/auth/password-reset', { body: { email: 'ada@example.com' } });
    expect(await screen.findByText(/If an account exists for ada@example.com/)).toBeOnTheScreen();
  });
});
