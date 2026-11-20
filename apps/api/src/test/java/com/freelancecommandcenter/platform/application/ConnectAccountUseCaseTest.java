package com.freelancecommandcenter.platform.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.freelancecommandcenter.platform.application.port.out.AccountConnector;
import com.freelancecommandcenter.platform.application.port.out.ConnectionRejectedException;
import com.freelancecommandcenter.platform.application.port.out.PlatformUnavailableException;
import com.freelancecommandcenter.platform.domain.ConnectedAccount;
import com.freelancecommandcenter.platform.domain.OAuthTokens;
import com.freelancecommandcenter.platform.domain.PlatformConnection;
import com.freelancecommandcenter.shared.error.DomainException;
import com.freelancecommandcenter.shared.error.ErrorKind;
import com.freelancecommandcenter.shared.id.UserId;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import com.freelancecommandcenter.shared.security.AuthenticatedUser;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConnectAccountUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-11-20T10:00:00Z");
    private static final WorkspaceId WORKSPACE = new WorkspaceId(UUID.randomUUID());
    private static final AuthenticatedUser CALLER = new AuthenticatedUser(new UserId(UUID.randomUUID()), WORKSPACE);

    private final InMemoryConnections connections = new InMemoryConnections();
    private final StubConnector connector = new StubConnector();
    private final Set<UserId> verifiedUsers = new HashSet<>(Set.of(CALLER.userId()));
    private final ConnectAccountUseCase connecting = new ConnectAccountUseCase(
            new PlatformRegistry(List.of(), List.of(), List.of(connector), List.of(), List.of()),
            connections,
            userId -> verifiedUsers.contains(userId),
            TestProperties.platform(),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void connectsTheAccountWhoseCodeComesBackWithTheState() {
        String state = stateOf(connecting.start(CALLER, PlatformCode.FREELANCER));

        PlatformConnection connection = connecting.complete(WORKSPACE, PlatformCode.FREELANCER, "code", state);

        assertThat(connector.redirectUri)
                .isEqualTo(URI.create("http://localhost:5173/connections/freelancer/callback"));
        assertThat(connection.accountName()).isEqualTo("Ada");
        assertThat(connections.findByWorkspace(WORKSPACE)).containsExactly(connection);
    }

    @Test
    void connectingAgainReplacesThePreviousConnection() {
        connecting.complete(
                WORKSPACE, PlatformCode.FREELANCER, "code", stateOf(connecting.start(CALLER, PlatformCode.FREELANCER)));
        PlatformConnection second = connecting.complete(
                WORKSPACE, PlatformCode.FREELANCER, "code", stateOf(connecting.start(CALLER, PlatformCode.FREELANCER)));

        assertThat(connections.findByWorkspace(WORKSPACE)).containsExactly(second);
    }

    @Test
    void aStateCompletesOneConnectionOfItsOwnWorkspaceOnly() {
        String state = stateOf(connecting.start(CALLER, PlatformCode.FREELANCER));
        WorkspaceId other = new WorkspaceId(UUID.randomUUID());

        assertCode(
                () -> connecting.complete(other, PlatformCode.FREELANCER, "code", state),
                "platform.connection_expired");
        assertCode(
                () -> connecting.complete(WORKSPACE, PlatformCode.FREELANCER, "code", state),
                "platform.connection_expired");
        assertThat(connections.connections).isEmpty();
    }

    @Test
    void platformFailuresAreReportedWithoutConnecting() {
        connector.failure = new ConnectionRejectedException(PlatformCode.FREELANCER, "refused", null);
        assertCode(
                () -> connecting.complete(
                        WORKSPACE,
                        PlatformCode.FREELANCER,
                        "code",
                        stateOf(connecting.start(CALLER, PlatformCode.FREELANCER))),
                "platform.connection_rejected");

        connector.failure = new PlatformUnavailableException(PlatformCode.FREELANCER, "down", null);
        assertThatThrownBy(() -> connecting.complete(
                        WORKSPACE,
                        PlatformCode.FREELANCER,
                        "code",
                        stateOf(connecting.start(CALLER, PlatformCode.FREELANCER))))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("platform.unavailable");
                    assertThat(exception.kind()).isEqualTo(ErrorKind.UNAVAILABLE);
                });
        assertThat(connections.connections).isEmpty();
    }

    @Test
    void onlyUsersWithAConfirmedEmailAddressStartConnecting() {
        verifiedUsers.clear();

        assertThatThrownBy(() -> connecting.start(CALLER, PlatformCode.FREELANCER))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("platform.email_not_verified");
                    assertThat(exception.kind()).isEqualTo(ErrorKind.FORBIDDEN);
                });
        assertThat(connections.attempts).isEmpty();
    }

    @Test
    void platformsWithoutAnOAuthAppCannotBeConnected() {
        assertCode(() -> connecting.start(CALLER, PlatformCode.UPWORK), "platform.connection_unavailable");
    }

    private static String stateOf(URI authorizationUrl) {
        return authorizationUrl.getQuery().substring("state=".length());
    }

    private static void assertCode(Runnable action, String code) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        DomainException.class,
                        exception -> assertThat(exception.code()).isEqualTo(code));
    }

    private static final class StubConnector implements AccountConnector {

        URI redirectUri;
        RuntimeException failure;

        @Override
        public PlatformCode platform() {
            return PlatformCode.FREELANCER;
        }

        @Override
        public URI authorizationUrl(String state, URI redirectUri) {
            return URI.create("https://accounts.example/authorize?state=" + state);
        }

        @Override
        public ConnectedAccount connect(String code, URI redirectUri) {
            if (failure != null) {
                throw failure;
            }
            this.redirectUri = redirectUri;
            return new ConnectedAccount(
                    "1001", "Ada", new OAuthTokens("access-" + code, "refresh", NOW.plus(Duration.ofDays(30))));
        }

        @Override
        public OAuthTokens refresh(OAuthTokens tokens) {
            return tokens;
        }
    }
}
