import { createApiClient } from '@fcc/api-client';
import { QueryClient } from '@tanstack/react-query';
import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

const REFRESH_TOKEN_KEY = 'fcc.refreshToken';

// The Android emulator reaches the host machine at 10.0.2.2; physical devices need EXPO_PUBLIC_API_URL set to the
// host's LAN address.
const localApi = Platform.OS === 'android' ? 'http://10.0.2.2:8080' : 'http://localhost:8080';

/** Native requests carry no Origin header, so the API returns the refresh token in the body; it lives in the keychain. */
export const api = createApiClient({
  baseUrl: process.env.EXPO_PUBLIC_API_URL ?? localApi,
  refreshTokenStore: {
    load: () => SecureStore.getItemAsync(REFRESH_TOKEN_KEY),
    save: (token) =>
      token
        ? SecureStore.setItemAsync(REFRESH_TOKEN_KEY, token)
        : SecureStore.deleteItemAsync(REFRESH_TOKEN_KEY),
  },
});

export const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1 } },
});
