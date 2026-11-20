package com.freelancecommandcenter.platform.application.port.out;

import com.freelancecommandcenter.platform.domain.ConnectionAttempt;
import com.freelancecommandcenter.platform.domain.PlatformConnection;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Connected platform accounts and started authorizations; tokens are stored encrypted. */
public interface PlatformConnectionRepository {

    List<PlatformConnection> findByWorkspace(WorkspaceId workspaceId);

    List<PlatformConnection> findActive(PlatformCode platform);

    /** Stores a new connection in place of the workspace's previous connection to the same platform. */
    void replace(PlatformConnection connection);

    /** Stores changes of an existing connection; does nothing when it was removed or replaced meanwhile. */
    void update(PlatformConnection connection);

    /** @return whether there was a connection to remove */
    boolean delete(WorkspaceId workspaceId, PlatformCode platform);

    /** Stores a started authorization and removes those started before {@code expiredBefore}. */
    void saveAttempt(ConnectionAttempt attempt, Instant expiredBefore);

    /** Removes and returns the authorization with this state, so each state completes at most one connection. */
    Optional<ConnectionAttempt> takeAttempt(String state);
}
