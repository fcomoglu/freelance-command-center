package com.freelancecommandcenter.platform.application.port.out;

import com.freelancecommandcenter.platform.domain.ProposalSnapshot;
import com.freelancecommandcenter.platform.domain.ProposalStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** The last known status of each proposal of a connection, which tells a change from a repeated read. */
public interface ProposalStatusStore {

    /** @return the known status by proposal id; proposals never seen are missing */
    Map<String, ProposalStatus> find(UUID connectionId, Collection<String> proposalIds);

    void save(UUID connectionId, Collection<ProposalSnapshot> proposals, Instant at);
}
