package com.freelancecommandcenter.platform.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.freelancecommandcenter.platform.api.MessageReceived;
import com.freelancecommandcenter.platform.api.NotificationReceived;
import com.freelancecommandcenter.platform.api.PlatformMessage;
import com.freelancecommandcenter.platform.api.PlatformMessage.Detail;
import com.freelancecommandcenter.platform.api.PlatformMessage.Direction;
import com.freelancecommandcenter.platform.api.PlatformNotification;
import com.freelancecommandcenter.platform.application.port.out.AccountConnector;
import com.freelancecommandcenter.platform.application.port.out.ConnectionRejectedException;
import com.freelancecommandcenter.platform.application.port.out.MessageSource;
import com.freelancecommandcenter.platform.application.port.out.PlatformUnavailableException;
import com.freelancecommandcenter.platform.application.port.out.ProposalStatusStore;
import com.freelancecommandcenter.platform.application.port.out.ProposalStatusTracker;
import com.freelancecommandcenter.platform.domain.ConnectedAccount;
import com.freelancecommandcenter.platform.domain.OAuthTokens;
import com.freelancecommandcenter.platform.domain.PlatformConnection;
import com.freelancecommandcenter.platform.domain.ProposalSnapshot;
import com.freelancecommandcenter.platform.domain.ProposalStatus;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class ConnectionSyncUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-11-20T10:00:00Z");
    private static final WorkspaceId WORKSPACE = new WorkspaceId(UUID.randomUUID());

    private final InMemoryConnections connections = new InMemoryConnections();
    private final List<Object> events = new ArrayList<>();
    private final StubConnector connector = new StubConnector();
    private final StubSource source = new StubSource();
    private final StubTracker tracker = new StubTracker();
    private final InMemoryProposalStatuses proposalStatuses = new InMemoryProposalStatuses();
    private final ConnectionSyncUseCase connectionSync = new ConnectionSyncUseCase(
            new PlatformRegistry(List.of(), List.of(), List.of(connector), List.of(source), List.of(tracker)),
            connections,
            proposalStatuses,
            events::add,
            new TransactionTemplate(new NoTransactionManager()),
            TestProperties.platform(),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void publishesTheMessagesOfEachConnectionAndAdvancesItsReadPosition() {
        PlatformConnection connection = connect(NOW.plus(Duration.ofDays(30)));
        source.result = List.of(message("1", NOW.minusSeconds(120)), message("2", NOW.minusSeconds(60)));

        connectionSync.syncAll();

        assertThat(source.requestedSince).isEqualTo(NOW.minus(TestProperties.MESSAGE_LOOKBACK));
        assertThat(source.requestedWith).isEqualTo("access-1");
        assertThat(events)
                .map(MessageReceived.class::cast)
                .allSatisfy(event -> assertThat(event.workspaceId()).isEqualTo(WORKSPACE))
                .extracting(event -> event.message().externalId())
                .containsExactly("1", "2");
        assertThat(connection.messagesFrom(TestProperties.MESSAGE_LOOKBACK)).isEqualTo(NOW.minusSeconds(60));

        source.result = List.of();
        connectionSync.syncAll();

        assertThat(source.requestedSince).isEqualTo(NOW.minusSeconds(60));
        assertThat(connection.messagesFrom(TestProperties.MESSAGE_LOOKBACK)).isEqualTo(NOW.minusSeconds(60));
    }

    @Test
    void renewsTokensThatExpireSoonBeforeReading() {
        PlatformConnection connection = connect(NOW.plus(Duration.ofMinutes(5)));

        connectionSync.syncAll();

        assertThat(connection.tokens().accessToken()).isEqualTo("access-2");
        assertThat(source.requestedWith).isEqualTo("access-2");
        assertThat(tracker.requestedWith).isEqualTo("access-2");
        assertThat(connector.refreshes).isEqualTo(1);
    }

    @Test
    void aRejectedAccessTokenIsRenewedOnceBeforeGivingUp() {
        PlatformConnection connection = connect(NOW.plus(Duration.ofDays(30)));
        source.rejectedTokens.add("access-1");
        source.result = List.of(message("1", NOW.minusSeconds(60)));

        connectionSync.syncAll();

        assertThat(connection.status()).isEqualTo(PlatformConnection.Status.ACTIVE);
        assertThat(source.requestedWith).isEqualTo("access-2");
        assertThat(events).hasSize(1);
    }

    @Test
    void aRejectedConnectionAwaitsReconnectingAndIsNoLongerRead() {
        PlatformConnection connection = connect(NOW.plus(Duration.ofDays(30)));
        source.failure = new ConnectionRejectedException(PlatformCode.FREELANCER, "revoked", null);

        connectionSync.syncAll();

        assertThat(connection.status()).isEqualTo(PlatformConnection.Status.RECONNECT_REQUIRED);
        assertThat(connections.findActive(PlatformCode.FREELANCER)).isEmpty();
        assertThat(events).isEmpty();
    }

    @Test
    void anUnavailablePlatformLeavesTheReadPositionForTheNextRun() {
        PlatformConnection connection = connect(NOW.plus(Duration.ofDays(30)));
        source.failure = new PlatformUnavailableException(PlatformCode.FREELANCER, "down", null);

        connectionSync.syncAll();

        assertThat(connection.status()).isEqualTo(PlatformConnection.Status.ACTIVE);
        assertThat(connection.messagesReadUntil()).isEmpty();
        assertThat(events).isEmpty();
    }

    @Test
    void theFirstProposalReadOnlyLearnsTheStatusesAndLaterDecisionsAreNotifiedOnce() {
        PlatformConnection connection = connect(NOW.plus(Duration.ofDays(30)));
        tracker.result = List.of(
                proposal("71", ProposalStatus.PENDING, Optional.empty()),
                proposal("72", ProposalStatus.AWARDED, Optional.of(NOW.minusSeconds(3600))));

        connectionSync.syncAll();

        assertThat(tracker.requestedSince).isEqualTo(NOW.minus(TestProperties.PROPOSAL_WINDOW));
        assertThat(events).isEmpty();
        assertThat(connection.proposalsReadAt()).contains(NOW);
        assertThat(proposalStatuses.statuses)
                .containsEntry("71", ProposalStatus.PENDING)
                .containsEntry("72", ProposalStatus.AWARDED);

        tracker.result = List.of(
                proposal("71", ProposalStatus.SHORTLISTED, Optional.empty()),
                proposal("72", ProposalStatus.AWARDED, Optional.of(NOW.minusSeconds(3600))),
                proposal("73", ProposalStatus.REJECTED, Optional.empty()),
                proposal("74", ProposalStatus.PENDING, Optional.empty()),
                proposal("75", ProposalStatus.WITHDRAWN, Optional.empty()));
        connectionSync.syncAll();
        connectionSync.syncAll();

        assertThat(events)
                .map(NotificationReceived.class::cast)
                .allSatisfy(event -> assertThat(event.workspaceId()).isEqualTo(WORKSPACE))
                .extracting(NotificationReceived::notification)
                .containsExactly(
                        new PlatformNotification(
                                PlatformCode.FREELANCER,
                                "proposal:71:shortlisted",
                                PlatformNotification.Kind.PROPOSAL_SHORTLISTED,
                                "Job 71",
                                "",
                                URI.create("https://www.freelancer.com/projects/job-71"),
                                NOW),
                        new PlatformNotification(
                                PlatformCode.FREELANCER,
                                "proposal:73:rejected",
                                PlatformNotification.Kind.PROPOSAL_REJECTED,
                                "Job 73",
                                "",
                                URI.create("https://www.freelancer.com/projects/job-73"),
                                NOW));
    }

    @Test
    void anAwardIsNotifiedWithTheTimeThePlatformReports() {
        connect(NOW.plus(Duration.ofDays(30)));
        tracker.result = List.of(proposal("81", ProposalStatus.SHORTLISTED, Optional.empty()));
        connectionSync.syncAll();

        tracker.result = List.of(proposal("81", ProposalStatus.AWARDED, Optional.of(NOW.minusSeconds(90))));
        connectionSync.syncAll();

        assertThat(events).map(NotificationReceived.class::cast).singleElement().satisfies(event -> {
            assertThat(event.notification().kind()).isEqualTo(PlatformNotification.Kind.PROPOSAL_AWARDED);
            assertThat(event.notification().occurredAt()).isEqualTo(NOW.minusSeconds(90));
        });
    }

    @Test
    void aRejectedProposalReadAlsoRequiresReconnecting() {
        PlatformConnection connection = connect(NOW.plus(Duration.ofDays(30)));
        tracker.failure = new ConnectionRejectedException(PlatformCode.FREELANCER, "scope missing", null);

        connectionSync.syncAll();

        assertThat(connection.status()).isEqualTo(PlatformConnection.Status.RECONNECT_REQUIRED);
        assertThat(connection.proposalsReadAt()).isEmpty();
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

    private static ProposalSnapshot proposal(String id, ProposalStatus status, Optional<Instant> changedAt) {
        return new ProposalSnapshot(
                id, "Job " + id, URI.create("https://www.freelancer.com/projects/job-" + id), status, changedAt);
    }

    private static final class StubConnector implements AccountConnector {

        int refreshes;

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
            refreshes++;
            return new OAuthTokens("access-2", tokens.refreshToken(), NOW.plus(Duration.ofDays(30)));
        }
    }

    private static final class StubSource implements MessageSource {

        List<PlatformMessage> result = List.of();
        RuntimeException failure;
        final List<String> rejectedTokens = new ArrayList<>();
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
            if (rejectedTokens.contains(accessToken)) {
                throw new ConnectionRejectedException(PlatformCode.FREELANCER, "revoked", null);
            }
            return result;
        }
    }

    private static final class StubTracker implements ProposalStatusTracker {

        List<ProposalSnapshot> result = List.of();
        RuntimeException failure;
        Instant requestedSince;
        String requestedWith;

        @Override
        public PlatformCode platform() {
            return PlatformCode.FREELANCER;
        }

        @Override
        public List<ProposalSnapshot> fetchSubmittedSince(String accountId, String accessToken, Instant since) {
            requestedSince = since;
            requestedWith = accessToken;
            if (failure != null) {
                throw failure;
            }
            return result;
        }
    }

    /** Keeps the statuses of the one connection of a test. */
    private static final class InMemoryProposalStatuses implements ProposalStatusStore {

        final Map<String, ProposalStatus> statuses = new HashMap<>();

        @Override
        public Map<String, ProposalStatus> find(UUID connectionId, Collection<String> proposalIds) {
            return statuses.entrySet().stream()
                    .filter(entry -> proposalIds.contains(entry.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        @Override
        public void save(UUID connectionId, Collection<ProposalSnapshot> proposals, Instant at) {
            proposals.forEach(proposal -> statuses.put(proposal.proposalId(), proposal.status()));
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
