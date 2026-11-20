import { ApiError } from '@fcc/api-client';
import { cleanup, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactNode } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { renderScreen } from '../test-support';
import { SignInPage } from './sign-in';

const session = vi.hoisted(() => ({ signIn: vi.fn() }));
vi.mock('../api', () => ({ api: { session } }));
vi.mock('@tanstack/react-router', () => ({
  Link: ({ to, children }: { to: string; children: ReactNode }) => <a href={to}>{children}</a>,
}));

async function signIn(email: string, password: string) {
  await userEvent.type(screen.getByLabelText('E-mail'), email);
  await userEvent.type(screen.getByLabelText('Password'), password);
  await userEvent.click(screen.getByRole('button', { name: 'Sign in' }));
}

afterEach(() => {
  cleanup();
  vi.resetAllMocks();
});

describe('SignInPage', () => {
  it('signs in with the entered address and password', async () => {
    session.signIn.mockResolvedValue(undefined);
    await renderScreen(<SignInPage />);

    await signIn('ada@example.com', 'correct horse battery');

    expect(session.signIn).toHaveBeenCalledWith('ada@example.com', 'correct horse battery');
    expect(screen.queryByRole('alert')).toBeNull();
  });

  it('shows the translated reason when the API refuses', async () => {
    session.signIn.mockRejectedValue(new ApiError({ status: 401, code: 'identity.invalid_credentials' }));
    await renderScreen(<SignInPage />);

    await signIn('ada@example.com', 'wrong password');

    expect(await screen.findByText('E-mail or password is wrong.')).toBeTruthy();
  });

  it('marks the fields the API rejected', async () => {
    session.signIn.mockRejectedValue(
      new ApiError({
        status: 400,
        code: 'validation.failed',
        errors: [{ field: 'email', code: 'identity.invalid_email' }],
      }),
    );
    await renderScreen(<SignInPage />);

    await signIn('ada@example', 'correct horse battery');

    expect(await screen.findByText('Please check the highlighted fields.')).toBeTruthy();
    expect(screen.getByLabelText('E-mail').getAttribute('aria-invalid')).toBe('true');
    expect(screen.getByText('Enter a valid e-mail address.')).toBeTruthy();
  });

  it('links to password reset and sign-up', async () => {
    await renderScreen(<SignInPage />);

    expect(screen.getByRole('link', { name: 'Forgot your password?' }).getAttribute('href')).toBe(
      '/forgot-password',
    );
    expect(screen.getByRole('link', { name: 'Create one' }).getAttribute('href')).toBe('/sign-up');
  });
});
