package com.freelancecommandcenter;

import com.freelancecommandcenter.platform.application.port.out.ProposalStatusTracker;
import com.freelancecommandcenter.platform.domain.ProposalSnapshot;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stands in for the Freelancer.com bids API in integration tests: every read with an access token returns the
 * proposals last set for it, like the platform reporting their current status.
 */
public class FakeProposalStatusTracker implements ProposalStatusTracker {

    private final Map<String, List<ProposalSnapshot>> current = new HashMap<>();

    public synchronized void set(String accessToken, ProposalSnapshot... proposals) {
        current.put(accessToken, List.of(proposals));
    }

    @Override
    public PlatformCode platform() {
        return PlatformCode.FREELANCER;
    }

    @Override
    public synchronized List<ProposalSnapshot> fetchSubmittedSince(
            String accountId, String accessToken, Instant submittedSince) {
        return current.getOrDefault(accessToken, List.of());
    }
}
