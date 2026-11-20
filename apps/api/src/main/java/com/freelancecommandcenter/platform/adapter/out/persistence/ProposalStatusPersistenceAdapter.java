package com.freelancecommandcenter.platform.adapter.out.persistence;

import com.freelancecommandcenter.platform.application.port.out.ProposalStatusStore;
import com.freelancecommandcenter.platform.domain.ProposalSnapshot;
import com.freelancecommandcenter.platform.domain.ProposalStatus;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** The last known proposal statuses in {@code platform.proposal_statuses}. */
@Component
class ProposalStatusPersistenceAdapter implements ProposalStatusStore {

    private final JdbcClient jdbc;

    ProposalStatusPersistenceAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<String, ProposalStatus> find(UUID connectionId, Collection<String> proposalIds) {
        if (proposalIds.isEmpty()) {
            return Map.of();
        }
        return jdbc
                .sql("SELECT proposal_id, status FROM platform.proposal_statuses "
                        + "WHERE connection_id = :connectionId AND proposal_id IN (:proposalIds)")
                .param("connectionId", connectionId)
                .param("proposalIds", proposalIds)
                .query((row, number) ->
                        Map.entry(row.getString("proposal_id"), ProposalStatus.valueOf(row.getString("status"))))
                .list()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public void save(UUID connectionId, Collection<ProposalSnapshot> proposals, Instant at) {
        for (ProposalSnapshot proposal : proposals) {
            jdbc.sql("""
                            INSERT INTO platform.proposal_statuses (connection_id, proposal_id, status, checked_at)
                            VALUES (?, ?, ?, ?)
                            ON CONFLICT (connection_id, proposal_id) DO UPDATE SET
                                status = EXCLUDED.status, checked_at = EXCLUDED.checked_at
                            """)
                    .params(
                            connectionId,
                            proposal.proposalId(),
                            proposal.status().name(),
                            Timestamp.from(at))
                    .update();
        }
    }
}
