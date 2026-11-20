import { useSyncExternalStore } from 'react';

import { api } from '@/api';

/** Re-renders when the user signs in or out. */
export function useSessionStatus() {
  return useSyncExternalStore(api.session.subscribe, api.session.getStatus);
}
