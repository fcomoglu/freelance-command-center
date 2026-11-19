import { createRootRoute, createRoute, createRouter, Outlet, redirect } from '@tanstack/react-router';

import { api } from './api';
import { AppLayout } from './pages/app-layout';
import { AuthLayout } from './pages/auth-layout';
import { ForgotPasswordPage } from './pages/forgot-password';
import { HomePage } from './pages/home';
import { ResetPasswordPage } from './pages/reset-password';
import { SignInPage } from './pages/sign-in';
import { SignUpPage } from './pages/sign-up';
import { VerifyEmailPage } from './pages/verify-email';
import { safeRedirect } from './session';

/** For pages that only make sense signed out; signed-in users continue to `target`. */
async function requireGuest(target = '/') {
  await api.session.restore();
  if (api.session.getStatus() === 'signedIn') throw redirect({ href: target });
}

/** For the app itself; signed-out users go to sign-in and come back afterwards. */
async function requireSession(currentHref: string) {
  await api.session.restore();
  if (api.session.getStatus() !== 'signedIn') {
    throw redirect({ to: '/sign-in', search: { redirect: currentHref } });
  }
}

const tokenSearch = (search: Record<string, unknown>): { token: string } => ({
  token: typeof search.token === 'string' ? search.token : '',
});

const rootRoute = createRootRoute({ component: Outlet });

const authLayoutRoute = createRoute({ getParentRoute: () => rootRoute, id: 'auth', component: AuthLayout });

const signInRoute = createRoute({
  getParentRoute: () => authLayoutRoute,
  path: '/sign-in',
  validateSearch: (search: Record<string, unknown>): { redirect?: string } => ({
    redirect: safeRedirect(search.redirect),
  }),
  // Also runs after a successful sign-in and then forwards to the page the user came from.
  beforeLoad: ({ search }) => requireGuest(search.redirect),
  component: SignInPage,
});

const signUpRoute = createRoute({
  getParentRoute: () => authLayoutRoute,
  path: '/sign-up',
  beforeLoad: () => requireGuest(),
  component: SignUpPage,
});

const forgotPasswordRoute = createRoute({
  getParentRoute: () => authLayoutRoute,
  path: '/forgot-password',
  beforeLoad: () => requireGuest(),
  component: ForgotPasswordPage,
});

// Targets of the links in identity e-mails (see SmtpIdentityMailer); they work signed in or out.
const verifyEmailRoute = createRoute({
  getParentRoute: () => authLayoutRoute,
  path: '/verify-email',
  validateSearch: tokenSearch,
  component: VerifyEmailPage,
});

const resetPasswordRoute = createRoute({
  getParentRoute: () => authLayoutRoute,
  path: '/reset-password',
  validateSearch: tokenSearch,
  component: ResetPasswordPage,
});

const appLayoutRoute = createRoute({
  getParentRoute: () => rootRoute,
  id: 'app',
  beforeLoad: ({ location }) => requireSession(location.href),
  component: AppLayout,
});

const homeRoute = createRoute({ getParentRoute: () => appLayoutRoute, path: '/', component: HomePage });

const routeTree = rootRoute.addChildren([
  authLayoutRoute.addChildren([
    signInRoute,
    signUpRoute,
    forgotPasswordRoute,
    verifyEmailRoute,
    resetPasswordRoute,
  ]),
  appLayoutRoute.addChildren([homeRoute]),
]);

export const router = createRouter({ routeTree });

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router;
  }
}
