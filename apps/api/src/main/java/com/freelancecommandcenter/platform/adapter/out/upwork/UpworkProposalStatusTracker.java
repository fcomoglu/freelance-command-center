package com.freelancecommandcenter.platform.adapter.out.upwork;

import com.freelancecommandcenter.platform.application.port.out.ProposalStatusTracker;
import com.freelancecommandcenter.platform.domain.ProposalSnapshot;
import com.freelancecommandcenter.platform.domain.ProposalStatus;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import tools.jackson.databind.JsonNode;

/**
 * Reads a connected freelancer's proposals that a client decided on. Upwork filters proposals by one status per
 * query, so a single request asks for the most recently changed offered, hired and declined proposals; proposals
 * still waiting for the client are not read, since only a decision is news.
 */
class UpworkProposalStatusTracker implements ProposalStatusTracker {

    private static final List<String> DECISIONS = List.of("offered", "hired", "declined");

    private final UpworkAccountApi api;
    private final URI site;
    private final String query;
    private final int pageSize;

    UpworkProposalStatusTracker(UpworkAccountApi api, URI site, String query, int pageSize) {
        this.api = api;
        this.site = site;
        this.query = query;
        this.pageSize = pageSize;
    }

    @Override
    public PlatformCode platform() {
        return PlatformCode.UPWORK;
    }

    @Override
    public List<ProposalSnapshot> fetchSubmittedSince(String accountId, String accessToken, Instant submittedSince) {
        JsonNode data = api.query(
                accessToken,
                query,
                Map.of(
                        "pagination", Map.of("first", pageSize),
                        "sort", Map.of("field", "MODIFIEDDATETIME", "sortOrder", "DESC")));
        List<ProposalSnapshot> proposals = new ArrayList<>();
        for (String decision : DECISIONS) {
            for (JsonNode edge : data.path(decision).path("edges").values()) {
                translate(edge.path("node"), submittedSince).ifPresent(proposals::add);
            }
        }
        return proposals;
    }

    private Optional<ProposalSnapshot> translate(JsonNode proposal, Instant submittedSince) {
        String id = proposal.path("id").asString("").strip();
        Optional<ProposalStatus> status =
                status(proposal.path("status").path("status").asString(""));
        JsonNode audit = proposal.path("auditDetails");
        Optional<Instant> submittedAt = UpworkJobTranslator.instant(audit.path("createdDateTime"));
        if (id.isEmpty()
                || status.isEmpty()
                || submittedAt.isEmpty()
                || submittedAt.get().isBefore(submittedSince)) {
            return Optional.empty();
        }
        JsonNode job = proposal.path("marketplaceJobPosting");
        String title = job.path("content").path("title").asString("").strip();
        return Optional.of(new ProposalSnapshot(
                id,
                title.isEmpty() ? "#" + job.path("id").asString(id) : title,
                site.resolve("/nx/proposals/" + id),
                status.get(),
                // A decision is the latest change of a proposal the freelancer no longer edits.
                UpworkJobTranslator.instant(audit.path("modifiedDateTime"))));
    }

    /** Upwork's status names; an offer counts as the award, and the hire that follows it is the same news. */
    static Optional<ProposalStatus> status(String name) {
        return switch (name) {
            case "Accepted", "Activated", "Pending" -> Optional.of(ProposalStatus.PENDING);
            case "Offered", "Hired" -> Optional.of(ProposalStatus.AWARDED);
            case "Declined" -> Optional.of(ProposalStatus.REJECTED);
            case "Withdrawn" -> Optional.of(ProposalStatus.WITHDRAWN);
            case "Archived" -> Optional.of(ProposalStatus.CLOSED);
            default -> Optional.empty();
        };
    }
}
