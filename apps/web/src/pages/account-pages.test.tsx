import { cleanup, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactNode } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { ok, renderScreen } from '../test-support';
import { ForgotPasswordPage } from './forgot-password';
import { ResetPasswordPage } from './reset-password';
import { SignUpPage } from './sign-up';
import { VerifyEmailPage } from './verify-email';

const api = vi.hoisted(() => ({
  fetchClient: { POST: vi.fn() },
  session: { getStatus: vi.fn(), signOut: vi.fn() },
}));
const search = vi.hoisted(() => ({ token: '' }));
vi.mock('../api', () => ({ api, queryClient: { invalidateQueries: vi.fn() } }));
vi.mock('@tanstack/react-router', () => ({
  Link: ({ to, children }: { to: string; children: ReactNode }) => <a href={to}>{children}</a>,
  Outlet: () => null,
  useSearch: () => search,
}));

const problem = (status: number, body: unknown) => ({
  error: body,
  response: new Response(null, { status }),
});

afterEach(() => {
  cleanup();
  vi.resetAllMocks();
  search.token = '';
});

describe('SignUpPage', () => {
  it('creates the account in the current language and asks to confirm the address', async () => {
    api.fetchClient.POST.mockResolvedValue(ok({}));
    await renderScreen(<SignUpPage />);

    await userEvent.type(screen.getByLabelText('E-mail'), 'ada@example.com');
    await userEvent.type(screen.getByLabelText('Password'), 'correct horse battery');
    await userEvent.click(screen.getByRole('button', { name: 'Create account' }));

    expect(api.fetchClient.POST).toHaveBeenCalledWith('/api/v1/auth/sign-up', {
      body: { email: 'ada@example.com', password: 'correct horse battery', language: 'en' },
    });
    expect(await screen.findByText('Check your inbox')).toBeTruthy();
    expect(screen.getByText(/We sent a confirmation link to ada@example.com/)).toBeTruthy();
  });

  it('shows why the password was refused next to the field', async () => {
    api.fetchClient.POST.mockResolvedValue(
      problem(400, {
        code: 'validation.failed',
        errors: [{ field: 'password', code: 'identity.weak_password' }],
      }),
    );
    await renderScreen(<SignUpPage />);

    await userEvent.type(screen.getByLabelText('E-mail'), 'ada@example.com');
    await userEvent.type(screen.getByLabelText('Password'), 'password12');
    await userEvent.click(screen.getByRole('button', { name: 'Create account' }));

    expect(await screen.findByText('Please check the highlighted fields.')).toBeTruthy();
    expect(screen.getByLabelText('Password').getAttribute('aria-invalid')).toBe('true');
  });
});

describe('ForgotPasswordPage', () => {
  it('confirms the request without revealing whether the account exists', async () => {
    api.fetchClient.POST.mockResolvedValue(ok());
    await renderScreen(<ForgotPasswordPage />);

    await userEvent.type(screen.getByLabelText('E-mail'), 'ada@example.com');
    await userEvent.click(screen.getByRole('button', { name: 'Send link' }));

    expect(api.fetchClient.POST).toHaveBeenCalledWith('/api/v1/auth/password-reset', {
      body: { email: 'ada@example.com' },
    });
    expect(await screen.findByText(/If an account exists for ada@example.com/)).toBeTruthy();
  });
});

describe('ResetPasswordPage', () => {
  it('asks for a new link when the token is missing', async () => {
    await renderScreen(<ResetPasswordPage />);

    expect(screen.getByText('This link is incomplete. Request a new one.')).toBeTruthy();
    expect(screen.queryByLabelText('New password')).toBeNull();
  });

  it('saves the new password and ends the session of this browser too', async () => {
    search.token = 'reset-token';
    api.fetchClient.POST.mockResolvedValue(ok());
    api.session.getStatus.mockReturnValue('signedIn');
    api.session.signOut.mockResolvedValue(undefined);
    await renderScreen(<ResetPasswordPage />);

    await userEvent.type(screen.getByLabelText('New password'), 'a much longer password');
    await userEvent.click(screen.getByRole('button', { name: 'Save password' }));

    expect(api.fetchClient.POST).toHaveBeenCalledWith('/api/v1/auth/password-reset/confirmation', {
      body: { token: 'reset-token', newPassword: 'a much longer password' },
    });
    expect(await screen.findByText(/Your password has been changed/)).toBeTruthy();
    expect(api.session.signOut).toHaveBeenCalledOnce();
  });
});

describe('VerifyEmailPage', () => {
  it('confirms the address with the token of the link', async () => {
    search.token = 'verify-token';
    api.fetchClient.POST.mockResolvedValue(ok());
    await renderScreen(<VerifyEmailPage />);

    expect(await screen.findByText('Your e-mail address is confirmed.')).toBeTruthy();
    expect(api.fetchClient.POST).toHaveBeenCalledWith('/api/v1/auth/email-verification', {
      body: { token: 'verify-token' },
    });
  });

  it('explains a link that was already used or expired', async () => {
    search.token = 'spent-token';
    api.fetchClient.POST.mockResolvedValue(problem(401, { code: 'identity.invalid_token' }));
    await renderScreen(<VerifyEmailPage />);

    expect(await screen.findByRole('alert')).toBeTruthy();
    expect(screen.queryByText('Your e-mail address is confirmed.')).toBeNull();
  });
});
