package com.freelancecommandcenter;

import com.freelancecommandcenter.notification.application.port.out.PushSender;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stands in for the Expo Push Service in integration tests: records every message and reports the tokens handed to
 * {@link #forget} as no longer registered.
 */
public class FakePushSender implements PushSender {

    private final List<PushMessage> sent = new ArrayList<>();
    private final Set<String> unregistered = ConcurrentHashMap.newKeySet();

    @Override
    public synchronized List<String> send(List<PushMessage> messages) {
        sent.addAll(messages);
        return messages.stream()
                .map(PushMessage::pushToken)
                .filter(unregistered::contains)
                .toList();
    }

    /** Messages sent to {@code pushToken} so far. */
    public synchronized List<PushMessage> sentTo(String pushToken) {
        return sent.stream()
                .filter(message -> message.pushToken().equals(pushToken))
                .toList();
    }

    public void forget(String pushToken) {
        unregistered.add(pushToken);
    }
}
