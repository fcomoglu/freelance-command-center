import { Stack } from 'expo-router';

// Signed-out users land here when the root guard removes the app screens.
export const unstable_settings = { initialRouteName: 'sign-in' };

/** Signed-out screens. E-mail links (verification, password reset) open the web app. */
export default function AuthLayout() {
  return <Stack screenOptions={{ headerShown: false }} />;
}
