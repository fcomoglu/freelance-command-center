import { unwrap } from '@fcc/api-client';
import * as Linking from 'expo-linking';
import * as Notifications from 'expo-notifications';
import { useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';

import { api, queryClient } from '@/api';
import { enablePush, feedNotificationId, notificationUrl, pushEnabled, pushSupported } from '@/push';

/**
 * Keeps the push registration of a signed-in phone current (a rotated token, a changed language, a new user on the
 * phone) and opens the platform page of a tapped notification: the job listing, the conversation or the platform
 * notification, which then counts as read.
 */
export function usePushNotifications(signedIn: boolean) {
  const { t, i18n } = useTranslation();
  const language = i18n.resolvedLanguage ?? 'en';
  // A tap can start the app before the session is restored; the notification is marked read once it is.
  const toMarkRead = useRef<string | undefined>(undefined);
  const signedInNow = useRef(signedIn);

  useEffect(() => {
    if (!signedIn || !pushSupported()) return;
    void (async () => {
      if (await pushEnabled()) await enablePush(language, t);
    })().catch(() => undefined);
  }, [signedIn, language, t]);

  const tapped = Notifications.useLastNotificationResponse();
  useEffect(() => {
    if (!tapped) return;
    const url = notificationUrl(tapped);
    toMarkRead.current = feedNotificationId(tapped);
    if (signedInNow.current) markTappedRead(toMarkRead);
    // Cleared so the same tap does not open the page again on the next launch.
    void Notifications.clearLastNotificationResponseAsync();
    if (url) void Linking.openURL(url);
  }, [tapped]);

  useEffect(() => {
    signedInNow.current = signedIn;
    if (signedIn) markTappedRead(toMarkRead);
  }, [signedIn]);
}

function markTappedRead(pending: { current: string | undefined }) {
  const notificationId = pending.current;
  if (!notificationId) return;
  pending.current = undefined;
  void unwrap(
    api.fetchClient.POST('/api/v1/notifications/{notificationId}/read', {
      params: { path: { notificationId } },
    }),
  )
    .then(() => queryClient.invalidateQueries({ queryKey: ['notifications'] }))
    .catch(() => undefined);
}
