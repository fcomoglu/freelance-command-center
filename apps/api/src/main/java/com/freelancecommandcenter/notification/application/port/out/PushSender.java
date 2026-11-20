package com.freelancecommandcenter.notification.application.port.out;

import java.util.List;
import java.util.Map;

/** Delivers push notifications to phones. */
public interface PushSender {

    /**
     * Hands the messages to the push service.
     *
     * @return the push tokens the service reported as no longer registered
     * @throws PushUnavailableException when the push service cannot be reached or refuses the request
     */
    List<String> send(List<PushMessage> messages);

    /**
     * @param channel Android notification channel the app creates on the phone
     * @param data passed to the app when the user taps the notification
     */
    record PushMessage(String pushToken, String title, String body, String channel, Map<String, String> data) {}

    class PushUnavailableException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public PushUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
