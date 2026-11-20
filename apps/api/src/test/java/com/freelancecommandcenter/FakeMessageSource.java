package com.freelancecommandcenter;

import com.freelancecommandcenter.platform.api.PlatformMessage;
import com.freelancecommandcenter.platform.application.port.out.MessageSource;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stands in for the Freelancer.com messaging API in integration tests: the next read with an access token returns the
 * messages offered for it, then nothing until new ones are offered.
 */
public class FakeMessageSource implements MessageSource {

    private final Map<String, List<PlatformMessage>> pending = new HashMap<>();

    public synchronized void offer(String accessToken, PlatformMessage... messages) {
        pending.computeIfAbsent(accessToken, token -> new ArrayList<>()).addAll(List.of(messages));
    }

    @Override
    public PlatformCode platform() {
        return PlatformCode.FREELANCER;
    }

    @Override
    public synchronized List<PlatformMessage> fetchSince(String accountId, String accessToken, Instant since) {
        List<PlatformMessage> messages = pending.remove(accessToken);
        return messages == null ? List.of() : messages;
    }
}
