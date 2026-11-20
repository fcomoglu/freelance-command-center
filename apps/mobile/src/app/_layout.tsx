import '@/i18n';

import { QueryClientProvider } from '@tanstack/react-query';
import { DarkTheme, DefaultTheme, Stack, ThemeProvider } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { useEffect } from 'react';
import { useColorScheme } from 'react-native';

import { api, queryClient } from '@/api';
import { AnimatedSplashOverlay } from '@/components/animated-icon';
import { usePushNotifications } from '@/hooks/use-push-notifications';
import { useSessionStatus } from '@/hooks/use-session-status';

SplashScreen.preventAutoHideAsync();

// Data of a previous user never outlives their session.
api.session.subscribe(() => {
  if (api.session.getStatus() === 'signedOut') queryClient.clear();
});

export default function RootLayout() {
  const colorScheme = useColorScheme();
  const status = useSessionStatus();
  usePushNotifications(status === 'signedIn');

  useEffect(() => {
    void api.session.restore();
  }, []);

  // The native splash screen stays up until it is known which side of the guard to show.
  if (status === 'loading') return null;

  const signedIn = status === 'signedIn';
  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider value={colorScheme === 'dark' ? DarkTheme : DefaultTheme}>
        <AnimatedSplashOverlay />
        {/* Explicit targets: the default fallback is the group itself, and (auth) has no index screen. */}
        <Stack screenOptions={{ headerShown: false }}>
          <Stack.Protected guard={signedIn} redirectTo="/sign-in">
            <Stack.Screen name="(app)" />
          </Stack.Protected>
          <Stack.Protected guard={!signedIn} redirectTo="/">
            <Stack.Screen name="(auth)" />
          </Stack.Protected>
        </Stack>
      </ThemeProvider>
    </QueryClientProvider>
  );
}
