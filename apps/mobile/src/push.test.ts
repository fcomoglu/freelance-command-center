import type { NotificationResponse } from 'expo-notifications';
import * as Notifications from 'expo-notifications';
import * as SecureStore from 'expo-secure-store';
import type { TFunction } from 'i18next';
import { Platform } from 'react-native';

import { api } from '@/api';
import { disablePush, enablePush, feedNotificationId, notificationUrl } from '@/push';

jest.mock('@/api', () => ({ api: { fetchClient: { POST: jest.fn(), DELETE: jest.fn() } } }));
jest.mock('expo-device', () => ({ isDevice: true }));
jest.mock('expo-constants', () => ({
  __esModule: true,
  ExecutionEnvironment: { StoreClient: 'storeClient', Standalone: 'standalone' },
  default: { executionEnvironment: 'standalone', expoConfig: { extra: { eas: { projectId: 'project-1' } } } },
}));
jest.mock('expo-notifications', () => ({
  AndroidImportance: { HIGH: 4 },
  setNotificationHandler: jest.fn(),
  setNotificationChannelAsync: jest.fn(),
  getPermissionsAsync: jest.fn(),
  requestPermissionsAsync: jest.fn(),
  getExpoPushTokenAsync: jest.fn(),
}));
jest.mock('expo-secure-store', () => ({
  getItemAsync: jest.fn(),
  setItemAsync: jest.fn(),
  deleteItemAsync: jest.fn(),
}));

const notifications = jest.mocked(Notifications);
const secureStore = jest.mocked(SecureStore);
const fetchClient = jest.mocked(api.fetchClient);
const t = ((key: string) => key) as unknown as TFunction;
const ok = (data?: unknown) => ({ data, response: new Response(null, { status: data ? 200 : 204 }) });

/** A tapped push carrying `data`, as the API sends it. */
const tapped = (data: Record<string, unknown>) =>
  ({ notification: { request: { content: { data } } } }) as unknown as NotificationResponse;

afterEach(() => {
  jest.clearAllMocks();
});

describe('notificationUrl', () => {
  it('opens only https links', () => {
    expect(notificationUrl(tapped({ url: 'https://www.fiverr.com' }))).toBe('https://www.fiverr.com');
    expect(notificationUrl(tapped({ url: 'http://example.com' }))).toBeUndefined();
    expect(notificationUrl(tapped({ url: 'javascript:alert(1)' }))).toBeUndefined();
    expect(notificationUrl(tapped({}))).toBeUndefined();
  });
});

describe('feedNotificationId', () => {
  it('names the feed entry of a platform notification only', () => {
    expect(feedNotificationId(tapped({ type: 'notification', notificationId: 'n-1' }))).toBe('n-1');
    expect(feedNotificationId(tapped({ type: 'job', notificationId: 'n-1' }))).toBeUndefined();
    expect(feedNotificationId(tapped({ type: 'notification' }))).toBeUndefined();
  });
});

describe('enablePush', () => {
  beforeEach(() => {
    jest.replaceProperty(Platform, 'OS', 'android');
  });

  it('creates the Android channels and registers the phone with its language', async () => {
    notifications.getPermissionsAsync.mockResolvedValue({ granted: true } as never);
    notifications.getExpoPushTokenAsync.mockResolvedValue({ data: 'ExponentPushToken[1]' } as never);
    fetchClient.POST.mockResolvedValue(ok({ id: 'device-1' }) as never);

    await expect(enablePush('tr-TR', t)).resolves.toBe('enabled');

    expect(notifications.setNotificationChannelAsync.mock.calls.map(([id]) => id)).toEqual([
      'jobs',
      'messages',
      'notifications',
    ]);
    expect(notifications.getExpoPushTokenAsync).toHaveBeenCalledWith({ projectId: 'project-1' });
    expect(fetchClient.POST).toHaveBeenCalledWith('/api/v1/devices', {
      body: { pushToken: 'ExponentPushToken[1]', language: 'tr' },
    });
    expect(secureStore.setItemAsync).toHaveBeenCalledWith('fcc.deviceId', 'device-1');
  });

  it('asks once and registers nothing when permission is denied', async () => {
    notifications.getPermissionsAsync.mockResolvedValue({ granted: false, canAskAgain: true } as never);
    notifications.requestPermissionsAsync.mockResolvedValue({ granted: false, canAskAgain: false } as never);

    await expect(enablePush('en', t)).resolves.toBe('denied');

    expect(notifications.requestPermissionsAsync).toHaveBeenCalledTimes(1);
    expect(fetchClient.POST).not.toHaveBeenCalled();
    expect(secureStore.setItemAsync).not.toHaveBeenCalled();
  });
});

describe('disablePush', () => {
  it('removes the registered phone at the API and forgets it', async () => {
    secureStore.getItemAsync.mockResolvedValue('device-1');
    fetchClient.DELETE.mockResolvedValue(ok() as never);

    await disablePush();

    expect(fetchClient.DELETE).toHaveBeenCalledWith('/api/v1/devices/{deviceId}', {
      params: { path: { deviceId: 'device-1' } },
    });
    expect(secureStore.deleteItemAsync).toHaveBeenCalledWith('fcc.deviceId');
  });

  it('keeps the registration when the API cannot remove it, so it can be retried', async () => {
    secureStore.getItemAsync.mockResolvedValue('device-1');
    fetchClient.DELETE.mockRejectedValue(new TypeError('Network request failed'));

    await expect(disablePush()).rejects.toMatchObject({ problem: { code: 'network.unreachable' } });

    expect(secureStore.deleteItemAsync).not.toHaveBeenCalled();
  });

  it('does nothing on a phone without push', async () => {
    secureStore.getItemAsync.mockResolvedValue(null);

    await disablePush();

    expect(fetchClient.DELETE).not.toHaveBeenCalled();
  });
});
