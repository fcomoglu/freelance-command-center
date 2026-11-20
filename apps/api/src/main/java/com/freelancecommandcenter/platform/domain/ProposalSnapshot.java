package com.freelancecommandcenter.platform.domain;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * One of the user's proposals as a platform API reports it now.
 *
 * @param proposalId the platform's id of the proposal
 * @param jobTitle the title of the job the proposal is for
 * @param url the job on its platform
 * @param statusChangedAt when the platform says the current status was reached, if it says
 */
public record ProposalSnapshot(
        String proposalId, String jobTitle, URI url, ProposalStatus status, Optional<Instant> statusChangedAt) {

    public ProposalSnapshot {
        Objects.requireNonNull(proposalId, "proposalId");
        Objects.requireNonNull(jobTitle, "jobTitle");
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(statusChangedAt, "statusChangedAt");
    }
}
