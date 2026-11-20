package com.freelancecommandcenter.platform.application.port.out;

import com.freelancecommandcenter.platform.api.JobListing;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.time.Instant;
import java.util.List;

/**
 * Capability port: a platform whose open jobs can be read by the server. One strategy per platform, selected through
 * {@link com.freelancecommandcenter.platform.application.PlatformRegistry}.
 */
public interface JobSource {

    PlatformCode platform();

    /**
     * Open listings updated at or after {@code since}, oldest update first. May return only the oldest part when there
     * are many; the next call continues from the newest update returned.
     *
     * @throws PlatformUnavailableException when the platform cannot be read right now
     */
    List<FetchedListing> fetchUpdatedSince(Instant since);

    /** A translated listing together with the platform's original JSON, kept so the translation can be re-applied. */
    record FetchedListing(JobListing listing, String rawJson) {}
}
