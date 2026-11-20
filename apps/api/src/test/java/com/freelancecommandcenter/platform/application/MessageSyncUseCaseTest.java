package com.freelancecommandcenter.platform.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.freelancecommandcenter.platform.api.MessageReceived;
import com.freelancecommandcenter.platform.api.PlatformMessage;
import com.freelancecommandcenter.platform.api.PlatformMessage.Detail;
import com.freelancecommandcenter.platform.api.PlatformMessage.Direction;
import com.freelancecommandcenter.platform.application.port.out.AccountConnector;
import com.freelancecommandcenter.platform.application.port.out.ConnectionRejectedException;
import com.freelancecommandcenter.platform.application.port.out.MessageSource;
import com.freelancecommandcenter.platform.application.port.out.PlatformUnavailableException;
import com.freelancecommandcenter.platform.domain.ConnectedAccount;
import com.freelancecommandcenter.platform.domain.OAuthTokens;
import com.freelancecommandcenter.platform.domain.PlatformConnection;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class MessageSyncUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-11-20T10:00:00Z");
    private static final WorkspaceId WORKSPACE = new WorkspaceId(UUID.randomUUID());

    private final InMemoryConnections connections = new InMemoryConnections();
    private final List<Object> events = new ArrayList<>();
    private final StubConnector connector = new StubConnector();
    private final StubSource source = new StubSource();
    private final MessageSyncUseCase messageSync = new MessageSyncUseCase(
            new PlatformRegistry(List.of(), List.of(), List.of(connector), List.of(source)),
            connections,
            events::add,
            new TransactionTemplate(new NoTransactionManager()),
            TestProperties.platform(),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void publishesTheMessagesOfEachConnectionAndAdvancesItsReadPosition() {
        PlatformConnection connection = connect(NOW.plus(Duration.ofDays(30)));
        source.result = List.of(message("1", NOW.minusSeconds(120)), message("2", NOW.minusSeconds(60)));

        messageSync.syncAll();

        assertThat(source.requestedSince).isEqualTo(NOW.minus(TestProperties.INITIAL_LOOKBACK));
        assertThat(source.requestedWith).isEqualTo("access-1");
        assertThat(events)
                .map(event -> (MessageReceived) event)
                .allSatisfy(event -> assertThat(event.workspaceId()).isEqualTo(WORKSPACE))
                .extracting(event -> event.message().externalId())
                .containsExactly("1", "2");
        assertThat(connection.messagesFrom(TestProperties.INITIAL_LOOKBACK)).isEqualTo(NOW.minusSeconds(60));

        source.result = List.of();
        messageSync.syncAll();

        assertThat(source.requestedSince).isEqualTo(NOW.minusSeconds(60));
        assertThat(connection.messagesFrom(TestProperties.INITIAL_LOOKBACK)).isEqualTo(NOW.minusSeconds(60));
    }

    @Test
    void renewsTokensThatExpireSoonBeforeReading() {
        PlatformConnection connection = connect(NOW.plus(Duration.ofHours(2)));

        messageSync.syncAll();

        assertThat(connection.tokens().accessToken()).isEqualTo("access-2");
        assertThat(source.requestedWith).isEqualTo("access-2");
        assertThat(connections.updates).isEqualTo(2);
    }

    @Test
    void aRejectedConnectionAwaitsReconnectingAndIsNoLongerRead() {
        PlatformConnection connection = connect(NOW.plus(Duration.ofDays(30)));
        source.failure = new ConnectionRejectedException(PlatformCode.FREELANCER, "revoked", null);

        messageSync.syncAll();

        assertThat(connection.status()).isEqualTo(PlatformConnection.Status.RECONNECT_REQUIRED);
        assertThat(connections.findActive(PlatformCode.FREELANCER)).isEmpty();
        assertThat(events).isEmpty();
    }

    @Test
    void anUnavailablePlatformLeavesTheReadPositionForTheNextRun() {
        PlatformConnection connection = connect(NOW.plus(Duration.ofDays(30)));
        source.failure = new PlatformUnavailableException(PlatformCode.FREELANCER, "down", null);

        messageSync.syncAll();

        assertThat(connection.status()).isEqualTo(PlatformConnection.Status.ACTIVE);
        assertThat(connection.messagesReadUntil()).isEmpty();
        assertThat(events).isEmpty();
    }

    private PlatformConnection connect(Instant expiresAt) {
        PlatformConnection connection = PlatformConnection.connect(
                WORKSPACE,
                PlatformCode.FREELANCER,
                new ConnectedAccount("1001", "Ada", new OAuthTokens("access-1", "refresh-1", expiresAt)),
                NOW);
        connections.replace(connection);
        return connection;
    }

    private static PlatformMessage message(String id, Instant sentAt) {
        return new PlatformMessage(
                PlatformCode.FREELANCER,
                "501",
                id,
                "Grace H.",
                Direction.INBOUND,
                Detail.FULL,
                "Message " + id,
                URI.create("https://www.freelancer.com/messages/thread/501"),
                sentAt);
    }

    private static final class StubConnector implements AccountConnector {

        @Override
        public PlatformCode platform() {
            return PlatformCode.FREELANCER;
        }

        @Override
        public URI authorizationUrl(String state, URI redirectUri) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ConnectedAccount connect(String code, URI redirectUri) {
            throw new UnsupportedOperationException();
        }

        @Override
        public OAuthTokens refresh(OAuthTokens tokens) {
            return new OAuthTokens("access-2", tokens.refreshToken(), NOW.plus(Duration.ofDays(30)));
        }
    }

    private static final class StubSource implements MessageSource {

        List<PlatformMessage> result = List.of();
        RuntimeException failure;
        Instant requestedSince;
        String requestedWith;

        @Override
        public PlatformCode platform() {
            return PlatformCode.FREELANCER;
        }

        @Override
        public List<PlatformMessage> fetchSince(String accountId, String accessToken, Instant since) {
            requestedSince = since;
            requestedWith = accessToken;
            if (failure != null) {
                throw failure;
            }
            return result;
        }
    }

    private static final class NoTransactionManager extends AbstractPlatformTransactionManager {

        private static final long serialVersionUID = 1L;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {}

        @Override
        protected void doCommit(DefaultTransactionStatus status) {}

        @Override
        protected void doRollback(DefaultTransactionStatus status) {}
    }
}
