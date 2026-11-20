import { unwrap } from '@fcc/api-client';
import { resolveLanguage } from '@fcc/i18n';
import Constants, { ExecutionEnvironment } from 'expo-constants';
import * as Device from 'expo-device';
import * as Notifications from 'expo-notifications';
import { AndroidImportance } from 'expo-notifications';
import * as SecureStore from 'expo-secure-store';
import type { TFunction } from 'i18next';
import { Platform } from 'react-native';

import { api } from '@/api';

/** Id of this phone at the API while it receives push notifications; absent when they are off. */
const DEVICE_ID_KEY = 'fcc.deviceId';
/** Android channels of new-job alerts, new messages and platform notifications; the API sends them with these ids. */
const JOBS_CHANNEL = 'jobs';
const MESSAGES_CHANNEL = 'messages';
const NOTIFICATIONS_CHANNEL = 'notifications';

export type PushResult = 'enabled' | 'denied' | 'unsupported';

// Alerts also show while the app is open.
Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowBanner: true,
    shouldShowList: true,
    shouldPlaySound: true,
    shouldSetBadge: false,
  }),
});

/** Remote push needs a real phone and a development or release build; Expo Go on Android cannot receive it. */
export function pushSupported(): boolean {
  return Device.isDevice && Constants.executionEnvironment !== ExecutionEnvironment.StoreClient;
}

export async function pushEnabled(): Promise<boolean> {
  return (await SecureStore.getItemAsync(DEVICE_ID_KEY)) !== null;
}

/**
 * Asks for permission when needed and registers this phone for the signed-in workspace. Safe to repeat: the API
 * keeps one device per push token, so this also refreshes a rotated token or a changed language.
 */
export async function enablePush(language: string, t: TFunction): Promise<PushResult> {
  if (!pushSupported()) return 'unsupported';
  // Android 13+ only issues a push token once a channel exists.
  if (Platform.OS === 'android') {
    await Notifications.setNotificationChannelAsync(JOBS_CHANNEL, {
      name: t('notifications.jobsChannel'),
      importance: AndroidImportance.HIGH,
    });
    await Notifications.setNotificationChannelAsync(MESSAGES_CHANNEL, {
      name: t('notifications.messagesChannel'),
      importance: AndroidImportance.HIGH,
    });
    await Notifications.setNotificationChannelAsync(NOTIFICATIONS_CHANNEL, {
      name: t('notifications.feedChannel'),
      importance: AndroidImportance.HIGH,
    });
  }
  let permission = await Notifications.getPermissionsAsync();
  if (!permission.granted && permission.canAskAgain) {
    permission = await Notifications.requestPermissionsAsync();
  }
  if (!permission.granted) return 'denied';

  const projectId = Constants.expoConfig?.extra?.eas?.projectId ?? Constants.easConfig?.projectId;
  const pushToken = (await Notifications.getExpoPushTokenAsync({ projectId })).data;
  const device = await unwrap(
    api.fetchClient.POST('/api/v1/devices', { body: { pushToken, language: resolveLanguage(language) } }),
  );
  await SecureStore.setItemAsync(DEVICE_ID_KEY, device.id);
  return 'enabled';
}

/** Stops push to this phone. Runs before sign-out, while the session can still authenticate the request. */
export async function disablePush(): Promise<void> {
  const deviceId = await SecureStore.getItemAsync(DEVICE_ID_KEY);
  if (deviceId === null) return;
  await unwrap(api.fetchClient.DELETE('/api/v1/devices/{deviceId}', { params: { path: { deviceId } } }));
  await SecureStore.deleteItemAsync(DEVICE_ID_KEY);
}

/** The platform page a tapped notification points to: the job listing, the conversation or the notification. */
export function notificationUrl(response: Notifications.NotificationResponse): string | undefined {
  const url = response.notification.request.content.data?.url;
  return typeof url === 'string' && url.startsWith('https://') ? url : undefined;
}

/** The feed entry of a tapped platform notification, which opening it marks read. */
export function feedNotificationId(response: Notifications.NotificationResponse): string | undefined {
  const data = response.notification.request.content.data;
  return data?.type === 'notification' && typeof data.notificationId === 'string'
    ? data.notificationId
    : undefined;
}
