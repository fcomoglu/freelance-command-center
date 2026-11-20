import { useSyncExternalStore } from 'react';

import { api } from './api';

/** Re-renders when the user signs in or out. */
export function useSessionStatus() {
  return useSyncExternalStore(api.session.subscribe, api.session.getStatus);
}

/** Only same-site paths are followed after sign-in, so the parameter cannot send users to another site. */
export function safeRedirect(value: unknown): string | undefined {
  return typeof value === 'string' && value.startsWith('/') && !value.startsWith('//') ? value : undefined;
}
