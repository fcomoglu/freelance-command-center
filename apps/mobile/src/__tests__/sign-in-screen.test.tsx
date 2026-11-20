import { ApiError } from '@fcc/api-client';
import { fireEvent, screen } from '@testing-library/react-native';
import type { ReactNode } from 'react';

import { api } from '@/api';
import SignInScreen from '@/app/(auth)/sign-in';
import { renderScreen } from '@/test-support';

jest.mock('@/api', () => ({ api: { session: { signIn: jest.fn() } } }));
jest.mock('expo-router', () => ({ Link: ({ children }: { children: ReactNode }) => children }));

const signIn = jest.mocked(api.session.signIn);

async function submit(email: string, password: string) {
  await fireEvent.changeText(screen.getByLabelText('E-mail'), email);
  await fireEvent.changeText(screen.getByLabelText('Password'), password);
  await fireEvent.press(screen.getByRole('button', { name: 'Sign in' }));
}

afterEach(() => {
  jest.clearAllMocks();
});

describe('SignInScreen', () => {
  it('signs in with the trimmed address, as phone keyboards add trailing spaces', async () => {
    signIn.mockResolvedValue(undefined);
    await renderScreen(<SignInScreen />);

    await submit(' ada@example.com ', 'correct horse battery');

    expect(signIn).toHaveBeenCalledWith('ada@example.com', 'correct horse battery');
  });

  it('shows the translated reason when the API refuses', async () => {
    signIn.mockRejectedValue(new ApiError({ status: 401, code: 'identity.invalid_credentials' }));
    await renderScreen(<SignInScreen />);

    await submit('ada@example.com', 'wrong password');

    expect(await screen.findByText('E-mail or password is wrong.')).toBeOnTheScreen();
  });

  it('explains an unreachable server', async () => {
    signIn.mockRejectedValue(new ApiError({ status: 0, code: 'network.unreachable' }));
    await renderScreen(<SignInScreen />);

    await submit('ada@example.com', 'correct horse battery');

    expect(
      await screen.findByText('The server cannot be reached. Check your connection and try again.'),
    ).toBeOnTheScreen();
  });
});
