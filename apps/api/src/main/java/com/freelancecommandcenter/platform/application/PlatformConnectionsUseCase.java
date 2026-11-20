package com.freelancecommandcenter.platform.application;

import com.freelancecommandcenter.platform.application.port.out.PlatformConnectionRepository;
import com.freelancecommandcenter.platform.domain.PlatformConnection;
import com.freelancecommandcenter.shared.error.DomainException;
import com.freelancecommandcenter.shared.error.ErrorKind;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** The workspace's connected platform accounts, and disconnecting one. */
@Service
public class PlatformConnectionsUseCase {

    private final PlatformRegistry platforms;
    private final PlatformConnectionRepository connections;

    PlatformConnectionsUseCase(PlatformRegistry platforms, PlatformConnectionRepository connections) {
        this.platforms = platforms;
        this.connections = connections;
    }

    /** A platform whose accounts can be connected, with the workspace's connection if there is one. */
    public record ConnectablePlatform(PlatformCode platform, Optional<PlatformConnection> connection) {}

    public List<ConnectablePlatform> list(WorkspaceId workspaceId) {
        Map<PlatformCode, PlatformConnection> connected = connections.findByWorkspace(workspaceId).stream()
                .collect(Collectors.toMap(PlatformConnection::platform, Function.identity()));
        return platforms.connectablePlatforms().stream()
                .map(platform -> new ConnectablePlatform(platform, Optional.ofNullable(connected.get(platform))))
                .toList();
    }

    /**
     * Forgets the account's tokens; the conversations read so far stay in the inbox. Access granted on the platform
     * is withdrawn there.
     */
    public void disconnect(WorkspaceId workspaceId, PlatformCode platform) {
        if (!connections.delete(workspaceId, platform)) {
            throw new DomainException(
                    ErrorKind.NOT_FOUND,
                    "platform.connection_not_found",
                    "No " + platform.value() + " account is connected");
        }
    }
}
