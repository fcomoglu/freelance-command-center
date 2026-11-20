package com.freelancecommandcenter;

import com.freelancecommandcenter.platform.api.JobListing;
import com.freelancecommandcenter.platform.application.port.out.JobSource;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Stands in for the Freelancer.com adapter in integration tests: the next sync returns the listings handed to
 * {@link #offer}, then nothing until new ones are offered.
 */
public class FakeJobSource implements JobSource {

    private final List<JobListing> pending = new ArrayList<>();

    public synchronized void offer(JobListing... listings) {
        pending.addAll(List.of(listings));
    }

    @Override
    public PlatformCode platform() {
        return PlatformCode.FREELANCER;
    }

    @Override
    public synchronized List<FetchedListing> fetchUpdatedSince(Instant since) {
        List<FetchedListing> fetched = pending.stream()
                .map(listing -> new FetchedListing(listing, "{\"id\":\"" + listing.externalId() + "\"}"))
                .toList();
        pending.clear();
        return fetched;
    }
}
