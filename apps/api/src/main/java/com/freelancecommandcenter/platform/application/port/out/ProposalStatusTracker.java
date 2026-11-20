package com.freelancecommandcenter.platform.application.port.out;

import com.freelancecommandcenter.platform.domain.ProposalSnapshot;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.time.Instant;
import java.util.List;

/** Reads the current status of a connected account's proposals from a platform API. */
public interface ProposalStatusTracker {

    PlatformCode platform();

    /**
     * The account's proposals submitted from {@code submittedSince} (inclusive), with their current status, in any order.
     * A bounded number of requests per call keeps one read within the platform's rate limits.
     *
     * @param accountId the platform's id of the account
     * @throws ConnectionRejectedException when the platform rejects the access token
     * @throws PlatformUnavailableException when the platform cannot be reached or answers unexpectedly
     */
    List<ProposalSnapshot> fetchSubmittedSince(String accountId, String accessToken, Instant submittedSince);
}
