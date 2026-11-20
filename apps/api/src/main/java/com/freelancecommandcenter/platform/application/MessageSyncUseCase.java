package com.freelancecommandcenter.platform.application;

import com.freelancecommandcenter.platform.api.MessageReceived;
import com.freelancecommandcenter.platform.api.PlatformMessage;
import com.freelancecommandcenter.platform.application.port.out.AccountConnector;
import com.freelancecommandcenter.platform.application.port.out.ConnectionRejectedException;
import com.freelancecommandcenter.platform.application.port.out.MessageSource;
import com.freelancecommandcenter.platform.application.port.out.PlatformConnectionRepository;
import com.freelancecommandcenter.platform.application.port.out.PlatformUnavailableException;
import com.freelancecommandcenter.platform.domain.PlatformConnection;
import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Reads new messages of every connected account and publishes them as {@link MessageReceived} events with full detail
 * (ADR 0013, ADR 0014). A failed read leaves the read position unchanged, so the next run reads the same period again.
 */
@Service
public class MessageSyncUseCase {

    private static final Logger log = LoggerFactory.getLogger(MessageSyncUseCase.class);

    /** Renew this long before expiry, so a token never runs out between two reads. */
    static final Duration TOKEN_RENEWAL_MARGIN = Duration.ofDays(1);

    private final PlatformRegistry platforms;
    private final PlatformConnectionRepository connections;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate transaction;
    private final Duration initialLookback;
    private final Clock clock;

    MessageSyncUseCase(
            PlatformRegistry platforms,
            PlatformConnectionRepository connections,
            ApplicationEventPublisher events,
            TransactionTemplate transaction,
            PlatformProperties properties,
            Clock clock) {
        this.platforms = platforms;
        this.connections = connections;
        this.events = events;
        this.transaction = transaction;
        this.initialLookback = properties.connections().messageSync().initialLookback();
        this.clock = clock;
    }

    /** Reads every active connection; one failing account or platform does not stop the others. */
    public void syncAll() {
        for (MessageSource source : platforms.messageSources()) {
            Optional<AccountConnector> connector = platforms.accountConnector(source.platform());
            if (connector.isEmpty()) {
                continue;
            }
            for (PlatformConnection connection : connections.findActive(source.platform())) {
                try {
                    int count = sync(connection, source, connector.get());
                    log.debug("Message sync of connection {} read {} messages", connection.id(), count);
                } catch (ConnectionRejectedException exception) {
                    log.info("Connection {} needs to be connected again: {}", connection.id(), exception.getMessage());
                    connection.requireReconnect();
                    connections.update(connection);
                } catch (PlatformUnavailableException exception) {
                    log.warn("Message sync of connection {} skipped: {}", connection.id(), exception.getMessage());
                }
            }
        }
    }

    /**
     * Platform calls run outside the transaction so no database connection waits for them; the events (outbox) and
     * the new read position are then stored together.
     *
     * @return the number of messages read
     */
    int sync(PlatformConnection connection, MessageSource source, AccountConnector connector) {
        if (connection.tokens().expiresWithin(TOKEN_RENEWAL_MARGIN, clock.instant())) {
            connection.renewTokens(connector.refresh(connection.tokens()));
            connections.update(connection);
        }
        List<PlatformMessage> messages = source.fetchSince(
                connection.accountId(), connection.tokens().accessToken(), connection.messagesFrom(initialLookback));
        transaction.executeWithoutResult(status -> {
            messages.forEach(message -> events.publishEvent(new MessageReceived(message, connection.workspaceId())));
            connection.recordMessagesRead(
                    messages.stream().map(PlatformMessage::sentAt).max(Comparator.naturalOrder()));
            connections.update(connection);
        });
        return messages.size();
    }
}
