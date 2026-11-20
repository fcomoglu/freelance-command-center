package com.freelancecommandcenter.platform.application;

import com.freelancecommandcenter.platform.application.port.out.AccountConnector;
import com.freelancecommandcenter.platform.application.port.out.ConnectionRejectedException;
import com.freelancecommandcenter.platform.application.port.out.PlatformConnectionRepository;
import com.freelancecommandcenter.platform.application.port.out.PlatformUnavailableException;
import com.freelancecommandcenter.platform.domain.ConnectedAccount;
import com.freelancecommandcenter.platform.domain.ConnectionAttempt;
import com.freelancecommandcenter.platform.domain.PlatformConnection;
import com.freelancecommandcenter.shared.error.DomainException;
import com.freelancecommandcenter.shared.error.ErrorKind;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Connects a platform account to a workspace with the OAuth 2 authorization code flow (ADR 0014): {@link #start}
 * returns the platform's consent page, the platform sends the user back to the web client, and {@link #complete}
 * redeems the code. Connecting again replaces the workspace's previous connection to the platform.
 */
@Service
public class ConnectAccountUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConnectAccountUseCase.class);

    private final PlatformRegistry platforms;
    private final PlatformConnectionRepository connections;
    private final URI redirectBaseUrl;
    private final Clock clock;

    ConnectAccountUseCase(
            PlatformRegistry platforms,
            PlatformConnectionRepository connections,
            PlatformProperties properties,
            Clock clock) {
        this.platforms = platforms;
        this.connections = connections;
        this.redirectBaseUrl = properties.connections().redirectBaseUrl();
        this.clock = clock;
    }

    /** The redirect URI registered with the platform's OAuth app. */
    public URI redirectUri(PlatformCode platform) {
        String base = redirectBaseUrl.toString().replaceAll("/+$", "");
        return URI.create(base + "/connections/" + platform.value() + "/callback");
    }

    /** @return the platform's consent page to send the user to */
    public URI start(WorkspaceId workspaceId, PlatformCode platform) {
        AccountConnector connector = connector(platform);
        Instant now = clock.instant();
        ConnectionAttempt attempt = ConnectionAttempt.start(workspaceId, platform, now);
        connections.saveAttempt(attempt, now.minus(ConnectionAttempt.LIFETIME));
        return connector.authorizationUrl(attempt.state(), redirectUri(platform));
    }

    /**
     * Redeems the code the platform returned with {@code state}. The platform call runs outside a transaction, so no
     * database connection waits for it; the state is consumed first, so a code is never redeemed twice.
     */
    public PlatformConnection complete(WorkspaceId workspaceId, PlatformCode platform, String code, String state) {
        AccountConnector connector = connector(platform);
        Instant now = clock.instant();
        connections
                .takeAttempt(state)
                .filter(attempt -> attempt.completes(workspaceId, platform, now))
                .orElseThrow(() -> new DomainException(
                        ErrorKind.INVALID,
                        "platform.connection_expired",
                        "The authorization was not started here or has expired; connect again"));
        ConnectedAccount account;
        try {
            account = connector.connect(code, redirectUri(platform));
        } catch (ConnectionRejectedException exception) {
            log.info("Connection refused: {}", exception.getMessage());
            throw new DomainException(
                    ErrorKind.INVALID, "platform.connection_rejected", "The platform refused the authorization");
        } catch (PlatformUnavailableException exception) {
            log.warn("Connection failed: {}", exception.getMessage(), exception);
            throw new DomainException(
                    ErrorKind.UNAVAILABLE, "platform.unavailable", "The platform cannot be reached; try again later");
        }
        PlatformConnection connection = PlatformConnection.connect(workspaceId, platform, account, now);
        connections.replace(connection);
        return connection;
    }

    private AccountConnector connector(PlatformCode platform) {
        return platforms
                .accountConnector(platform)
                .orElseThrow(() -> new DomainException(
                        ErrorKind.NOT_FOUND,
                        "platform.connection_unavailable",
                        "Accounts of " + platform.value() + " cannot be connected"));
    }
}
