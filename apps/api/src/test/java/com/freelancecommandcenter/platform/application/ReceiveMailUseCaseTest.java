package com.freelancecommandcenter.platform.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.freelancecommandcenter.platform.api.JobDiscovered;
import com.freelancecommandcenter.platform.api.JobListing;
import com.freelancecommandcenter.platform.application.ReceiveMailUseCase.Outcome;
import com.freelancecommandcenter.platform.application.port.out.InboundMailStore;
import com.freelancecommandcenter.platform.application.port.out.MailInboxRepository;
import com.freelancecommandcenter.platform.application.port.out.PlatformMailParser;
import com.freelancecommandcenter.platform.domain.InboundMail;
import com.freelancecommandcenter.platform.domain.InboundMail.Status;
import com.freelancecommandcenter.platform.domain.MailInbox;
import com.freelancecommandcenter.platform.domain.MailMessage;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.pagination.KeysetCursor;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

class ReceiveMailUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");
    private static final MailInbox INBOX = MailInbox.newFor(WorkspaceId.newId());
    private static final String ADDRESS = INBOX.address("inbound.localhost");

    private final List<InboundMail> stored = new ArrayList<>();
    private final List<Object> events = new ArrayList<>();
    private MailMessage message = message("jobs@arc.dev", "<1@arc.dev>");

    @Test
    void jobsOfAnUnderstoodMailArePublishedAsPrivateToTheWorkspace() {
        ReceiveMailUseCase receiver =
                receiver(parser((mail, at) -> Optional.empty()), parser((mail, at) -> Optional.of(jobs("42", "43"))));

        assertThat(receiver.receive(ADDRESS, new byte[0])).isEqualTo(Outcome.STORED);

        assertThat(stored).singleElement().extracting(InboundMail::status).isEqualTo(Status.PROCESSED);
        assertThat(events)
                .map(JobDiscovered.class::cast)
                .allSatisfy(event -> assertThat(event.owner()).isEqualTo(INBOX.workspaceId()))
                .extracting(event -> event.listing().externalId())
                .containsExactly("42", "43");
    }

    @Test
    void aRedeliveredMailPublishesNothing() {
        ReceiveMailUseCase receiver = receiver(parser((mail, at) -> Optional.of(jobs("42"))));
        receiver.receive(ADDRESS, new byte[0]);
        events.clear();

        assertThat(receiver.receive(ADDRESS, new byte[0])).isEqualTo(Outcome.DUPLICATE);

        assertThat(events).isEmpty();
    }

    @Test
    void aMailNoParserUnderstandsIsStoredAsUnrecognizedEvenWhenAParserFails() {
        ReceiveMailUseCase receiver = receiver(parser((mail, at) -> {
            throw new IllegalStateException("unexpected layout");
        }));

        assertThat(receiver.receive(ADDRESS, new byte[0])).isEqualTo(Outcome.STORED);

        assertThat(stored).singleElement().extracting(InboundMail::status).isEqualTo(Status.UNRECOGNIZED);
        assertThat(events).isEmpty();
    }

    @Test
    void mailFromOutsideThePlatformsIsNotParsed() {
        message = message("friend@example.com", "<2@example.com>");
        ReceiveMailUseCase receiver = receiver(parser((mail, at) -> Optional.of(jobs("42"))));

        receiver.receive(ADDRESS, new byte[0]);

        assertThat(stored).singleElement().extracting(InboundMail::status).isEqualTo(Status.UNKNOWN_SENDER);
        assertThat(events).isEmpty();
    }

    private ReceiveMailUseCase receiver(PlatformMailParser... parsers) {
        return new ReceiveMailUseCase(
                new SingleInbox(),
                new InMemoryMails(),
                raw -> message,
                new PlatformRegistry(List.of(), List.of(parsers), List.of(), List.of()),
                events::add,
                new PlatformProperties(
                        new PlatformProperties.JobSync(
                                false, Duration.ofMinutes(5), Duration.ZERO, Duration.ofHours(1)),
                        new PlatformProperties.Mail("inbound.localhost", "", DataSize.ofMegabytes(5)),
                        TestProperties.connections()),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static PlatformMailParser parser(
            BiFunction<MailMessage, Instant, Optional<PlatformMailParser.ParsedMail>> parse) {
        return new PlatformMailParser() {
            @Override
            public PlatformCode platform() {
                return PlatformCode.ARC_DEV;
            }

            @Override
            public Optional<ParsedMail> parse(MailMessage mail, Instant receivedAt) {
                return parse.apply(mail, receivedAt);
            }
        };
    }

    private static PlatformMailParser.ParsedMail jobs(String... externalIds) {
        return PlatformMailParser.ParsedMail.ofJobs(List.of(externalIds).stream()
                .map(id -> new JobListing(
                        PlatformCode.ARC_DEV,
                        id,
                        "Job " + id,
                        "",
                        URI.create("https://arc.dev/remote-jobs/" + id),
                        null,
                        List.of(),
                        null,
                        null,
                        NOW,
                        NOW))
                .toList());
    }

    private static MailMessage message(String sender, String messageId) {
        return new MailMessage(
                Optional.of(messageId), sender, "Job alert", Optional.empty(), Optional.empty(), Optional.empty());
    }

    private static final class SingleInbox implements MailInboxRepository {

        @Override
        public Optional<MailInbox> findByWorkspace(WorkspaceId workspaceId) {
            return Optional.of(INBOX).filter(inbox -> inbox.workspaceId().equals(workspaceId));
        }

        @Override
        public Optional<MailInbox> findByLocalPart(String localPart) {
            return Optional.of(INBOX).filter(inbox -> inbox.localPart().equals(localPart));
        }

        @Override
        public void save(MailInbox inbox) {
            throw new UnsupportedOperationException();
        }
    }

    private final class InMemoryMails implements InboundMailStore {

        @Override
        public boolean save(InboundMail mail, byte[] raw) {
            if (stored.stream().anyMatch(existing -> existing.messageId().equals(mail.messageId()))) {
                return false;
            }
            stored.add(mail);
            return true;
        }

        @Override
        public List<InboundMail> findRecent(WorkspaceId workspaceId, Optional<KeysetCursor> after, int limit) {
            throw new UnsupportedOperationException();
        }
    }
}
