package com.freelancecommandcenter.platform.application.port.out;

import com.freelancecommandcenter.platform.api.PlatformMessage;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.time.Instant;
import java.util.List;

/** Reads the messages of a connected account from a platform API. */
public interface MessageSource {

    PlatformCode platform();

    /**
     * The account's messages, sent and received, from {@code since} (inclusive), in any order. A bounded number of
     * requests per call keeps one read within the platform's rate limits; the rest follows on the next call.
     *
     * @param accountId the platform's id of the account, which tells sent from received messages
     * @throws ConnectionRejectedException when the platform rejects the access token
     * @throws PlatformUnavailableException when the platform cannot be reached or answers unexpectedly
     */
    List<PlatformMessage> fetchSince(String accountId, String accessToken, Instant since);
}
