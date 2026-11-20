package com.freelancecommandcenter.platform.application;

import com.freelancecommandcenter.platform.application.port.out.PlatformConnectionRepository;
import com.freelancecommandcenter.platform.domain.ConnectionAttempt;
import com.freelancecommandcenter.platform.domain.PlatformConnection;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Keeps connections and attempts in memory; counts updates so tests can see what was stored. */
final class InMemoryConnections implements PlatformConnectionRepository {

    final List<PlatformConnection> connections = new ArrayList<>();
    final Map<String, ConnectionAttempt> attempts = new HashMap<>();
    int updates;

    @Override
    public List<PlatformConnection> findByWorkspace(WorkspaceId workspaceId) {
        return connections.stream()
                .filter(connection -> connection.workspaceId().equals(workspaceId))
                .toList();
    }

    @Override
    public List<PlatformConnection> findActive(PlatformCode platform) {
        return connections.stream()
                .filter(connection ->
                        connection.platform() == platform && connection.status() == PlatformConnection.Status.ACTIVE)
                .toList();
    }

    @Override
    public void replace(PlatformConnection connection) {
        delete(connection.workspaceId(), connection.platform());
        connections.add(connection);
    }

    @Override
    public void update(PlatformConnection connection) {
        updates++;
    }

    @Override
    public boolean delete(WorkspaceId workspaceId, PlatformCode platform) {
        return connections.removeIf(
                connection -> connection.workspaceId().equals(workspaceId) && connection.platform() == platform);
    }

    @Override
    public void saveAttempt(ConnectionAttempt attempt, Instant expiredBefore) {
        attempts.values().removeIf(stored -> stored.startedAt().isBefore(expiredBefore));
        attempts.put(attempt.state(), attempt);
    }

    @Override
    public Optional<ConnectionAttempt> takeAttempt(String state) {
        return Optional.ofNullable(attempts.remove(state));
    }
}
