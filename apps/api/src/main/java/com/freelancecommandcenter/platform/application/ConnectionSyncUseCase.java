package com.freelancecommandcenter.platform.application;

import com.freelancecommandcenter.platform.api.MessageReceived;
import com.freelancecommandcenter.platform.api.NotificationReceived;
import com.freelancecommandcenter.platform.api.PlatformMessage;
import com.freelancecommandcenter.platform.api.PlatformNotification;
import com.freelancecommandcenter.platform.application.port.out.AccountConnector;
import com.freelancecommandcenter.platform.application.port.out.ConnectionRejectedException;
import com.freelancecommandcenter.platform.application.port.out.MessageSource;
import com.freelancecommandcenter.platform.application.port.out.PlatformConnectionRepository;
import com.freelancecommandcenter.platform.application.port.out.PlatformUnavailableException;
import com.freelancecommandcenter.platform.application.port.out.ProposalStatusStore;
import com.freelancecommandcenter.platform.application.port.out.ProposalStatusTracker;
import com.freelancecommandcenter.platform.domain.PlatformConnection;
import com.freelancecommandcenter.platform.domain.ProposalSnapshot;
import com.freelancecommandcenter.platform.domain.ProposalStatus;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Reads every connected account: new messages are published as {@link MessageReceived} events with full detail (ADR
 * 0013, ADR 0014), changes of its proposals as {@link NotificationReceived} events (ADR 0015). Both reads of a
 * connection run one after the other in one task, so they never renew the same tokens at the same time. A failed read
 * leaves the read position unchanged, so the next run reads the same period again.
 */
@Service
public class ConnectionSyncUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConnectionSyncUseCase.class);

    /**
     * Renew this long before expiry, so a token never runs out between two reads; short enough for access tokens that
     * live only a day (Upwork).
     */
    static final Duration TOKEN_RENEWAL_MARGIN = Duration.ofMinutes(10);

    private final PlatformRegistry platforms;
    private final PlatformConnectionRepository connections;
    private final ProposalStatusStore proposalStatuses;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate transaction;
    private final Duration messageLookback;
    private final Duration proposalWindow;
    private final Clock clock;

    ConnectionSyncUseCase(
            PlatformRegistry platforms,
            PlatformConnectionRepository connections,
            ProposalStatusStore proposalStatuses,
            ApplicationEventPublisher events,
            TransactionTemplate transaction,
            PlatformProperties properties,
            Clock clock) {
        this.platforms = platforms;
        this.connections = connections;
        this.proposalStatuses = proposalStatuses;
        this.events = events;
        this.transaction = transaction;
        this.messageLookback = properties.connections().sync().messageLookback();
        this.proposalWindow = properties.connections().sync().proposalWindow();
        this.clock = clock;
    }

    /** Reads every active connection; one failing account or platform does not stop the others. */
    public void syncAll() {
        for (PlatformCode platform : platforms.connectablePlatforms()) {
            AccountConnector connector = platforms.accountConnector(platform).orElseThrow();
            Optional<MessageSource> messages = platforms.messageSource(platform);
            Optional<ProposalStatusTracker> proposals = platforms.proposalStatusTracker(platform);
            if (messages.isEmpty() && proposals.isEmpty()) {
                continue;
            }
            for (PlatformConnection connection : connections.findActive(platform)) {
                try {
                    messages.ifPresent(source -> syncMessages(connection, source, connector));
                    proposals.ifPresent(tracker -> syncProposals(connection, tracker, connector));
                } catch (ConnectionRejectedException exception) {
                    log.info("Connection {} needs to be connected again: {}", connection.id(), exception.getMessage());
                    connection.requireReconnect();
                    connections.update(connection);
                } catch (PlatformUnavailableException exception) {
                    log.warn("Sync of connection {} skipped: {}", connection.id(), exception.getMessage());
                }
            }
        }
    }

    /**
     * Platform calls run outside the transaction so no database connection waits for them; the events (outbox) and
     * the new read position are then stored together.
     */
    void syncMessages(PlatformConnection connection, MessageSource source, AccountConnector connector) {
        List<PlatformMessage> messages = fetchWithCurrentTokens(
                connection,
                connector,
                accessToken -> source.fetchSince(
                        connection.accountId(), accessToken, connection.messagesFrom(messageLookback)));
        transaction.executeWithoutResult(status -> {
            messages.forEach(message -> events.publishEvent(new MessageReceived(message, connection.workspaceId())));
            connection.recordMessagesRead(
                    messages.stream().map(PlatformMessage::sentAt).max(Comparator.naturalOrder()));
            connections.update(connection);
        });
        log.debug("Connection {}: {} messages read", connection.id(), messages.size());
    }

    /**
     * A platform reports only the current status of a proposal, so a change is a status that differs from the one
     * stored at the previous read. Proposals submitted within the window are read every time, since their status can
     * change long after submission.
     */
    void syncProposals(PlatformConnection connection, ProposalStatusTracker tracker, AccountConnector connector) {
        Instant now = clock.instant();
        List<ProposalSnapshot> proposals = fetchWithCurrentTokens(
                connection,
                connector,
                accessToken ->
                        tracker.fetchSubmittedSince(connection.accountId(), accessToken, now.minus(proposalWindow)));
        transaction.executeWithoutResult(status -> {
            boolean firstRead = connection.proposalsReadAt().isEmpty();
            Map<String, ProposalStatus> known = proposalStatuses.find(
                    connection.id(),
                    proposals.stream().map(ProposalSnapshot::proposalId).toList());
            if (!firstRead) {
                proposals.stream()
                        .filter(proposal -> proposal.status() != known.get(proposal.proposalId()))
                        .flatMap(proposal -> notification(connection, proposal, now).stream())
                        .forEach(notification ->
                                events.publishEvent(new NotificationReceived(notification, connection.workspaceId())));
            }
            proposalStatuses.save(connection.id(), proposals, now);
            connection.recordProposalsRead(now);
            connections.update(connection);
        });
        log.debug("Connection {}: {} proposals read", connection.id(), proposals.size());
    }

    /**
     * Only the decisions of the client are notified; a proposal that is pending, withdrawn by the user or closed with
     * its job is not news. The status is part of the id, so a repeated change of the same proposal is one notification.
     */
    private Optional<PlatformNotification> notification(
            PlatformConnection connection, ProposalSnapshot proposal, Instant now) {
        Optional<PlatformNotification.Kind> kind = switch (proposal.status()) {
            case SHORTLISTED -> Optional.of(PlatformNotification.Kind.PROPOSAL_SHORTLISTED);
            case AWARDED -> Optional.of(PlatformNotification.Kind.PROPOSAL_AWARDED);
            case REJECTED -> Optional.of(PlatformNotification.Kind.PROPOSAL_REJECTED);
            case REVOKED -> Optional.of(PlatformNotification.Kind.PROPOSAL_REVOKED);
            case PENDING, WITHDRAWN, CLOSED -> Optional.empty();
        };
        return kind.map(value -> new PlatformNotification(
                connection.platform(),
                "proposal:" + proposal.proposalId() + ":"
                        + proposal.status().name().toLowerCase(Locale.ROOT),
                value,
                proposal.jobTitle(),
                "",
                proposal.url(),
                proposal.statusChangedAt().orElse(now)));
    }

    /**
     * Renews tokens that are about to expire before reading; a platform can also drop an access token early while
     * the refresh token still works, so a rejected read is retried once with renewed tokens.
     */
    private <T> T fetchWithCurrentTokens(
            PlatformConnection connection, AccountConnector connector, Function<String, T> read) {
        boolean renewed = false;
        if (connection.tokens().expiresWithin(TOKEN_RENEWAL_MARGIN, clock.instant())) {
            renew(connection, connector);
            renewed = true;
        }
        try {
            return read.apply(connection.tokens().accessToken());
        } catch (ConnectionRejectedException rejected) {
            if (renewed) {
                throw rejected;
            }
            renew(connection, connector);
            return read.apply(connection.tokens().accessToken());
        }
    }

    private void renew(PlatformConnection connection, AccountConnector connector) {
        connection.renewTokens(connector.refresh(connection.tokens()));
        connections.update(connection);
    }
}
