import * as Notifications from 'expo-notifications';
import * as Linking from 'expo-linking';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';

import { enablePush, jobUrl, pushEnabled, pushSupported } from '@/push';

/**
 * Keeps the push registration of a signed-in phone current (a rotated token, a changed language, a new user on the
 * phone) and opens the listing of a tapped notification.
 */
export function usePushNotifications(signedIn: boolean) {
  const { t, i18n } = useTranslation();
  const language = i18n.resolvedLanguage ?? 'en';

  useEffect(() => {
    if (!signedIn || !pushSupported()) return;
    void (async () => {
      if (await pushEnabled()) await enablePush(language, t);
    })().catch(() => undefined);
  }, [signedIn, language, t]);

  const tapped = Notifications.useLastNotificationResponse();
  useEffect(() => {
    if (!tapped) return;
    const url = jobUrl(tapped);
    // Cleared so the same tap does not open the listing again on the next launch.
    void Notifications.clearLastNotificationResponseAsync();
    if (url) void Linking.openURL(url);
  }, [tapped]);
}
