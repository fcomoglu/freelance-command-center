package com.freelancecommandcenter.platform.application;

import com.freelancecommandcenter.platform.api.JobDiscovered;
import com.freelancecommandcenter.platform.api.MessageReceived;
import com.freelancecommandcenter.platform.application.port.out.InboundMailStore;
import com.freelancecommandcenter.platform.application.port.out.MailInboxRepository;
import com.freelancecommandcenter.platform.application.port.out.MailReader;
import com.freelancecommandcenter.platform.application.port.out.PlatformMailParser;
import com.freelancecommandcenter.platform.application.port.out.PlatformMailParser.ParsedMail;
import com.freelancecommandcenter.platform.domain.InboundMail;
import com.freelancecommandcenter.platform.domain.InboundMail.Status;
import com.freelancecommandcenter.platform.domain.MailInbox;
import com.freelancecommandcenter.platform.domain.MailMessage;
import com.freelancecommandcenter.platform.domain.MailSenders;
import com.freelancecommandcenter.shared.id.Uuids;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Accepts a forwarded e-mail for a workspace inbox (ADR 0012): identifies the platform by the sender, reads the mail
 * with that platform's parsers and stores the raw message. Jobs and messages in the mail are published as private to
 * the workspace.
 * Re-delivered mails (same Message-ID) are stored and read once.
 */
@Service
public class ReceiveMailUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReceiveMailUseCase.class);

    private final MailInboxRepository inboxes;
    private final InboundMailStore mails;
    private final MailReader reader;
    private final PlatformRegistry platforms;
    private final ApplicationEventPublisher events;
    private final String domain;
    private final Clock clock;

    ReceiveMailUseCase(
            MailInboxRepository inboxes,
            InboundMailStore mails,
            MailReader reader,
            PlatformRegistry platforms,
            ApplicationEventPublisher events,
            PlatformProperties properties,
            Clock clock) {
        this.inboxes = inboxes;
        this.mails = mails;
        this.reader = reader;
        this.platforms = platforms;
        this.events = events;
        this.domain = properties.mail().inboundDomain();
        this.clock = clock;
    }

    public enum Outcome {
        STORED,
        DUPLICATE,
        /** No workspace has this address, for example because it was renewed. */
        UNKNOWN_RECIPIENT
    }

    /**
     * @param recipient the envelope recipient reported by the mail routing service
     * @throws MailReader.UnreadableMailException when {@code raw} is not a MIME message
     */
    @Transactional
    public Outcome receive(String recipient, byte[] raw) {
        Optional<MailInbox> inbox = MailInbox.localPartOf(recipient, domain).flatMap(inboxes::findByLocalPart);
        if (inbox.isEmpty()) {
            return Outcome.UNKNOWN_RECIPIENT;
        }
        WorkspaceId workspace = inbox.get().workspaceId();
        MailMessage message = reader.read(raw);
        Instant now = clock.instant();
        Optional<PlatformCode> platform = MailSenders.platformOf(message.sender());
        Optional<ParsedMail> parsed = platform.flatMap(code -> parse(code, message, now));
        Status status = platform.isEmpty()
                ? Status.UNKNOWN_SENDER
                : parsed.isPresent() ? Status.PROCESSED : Status.UNRECOGNIZED;
        InboundMail mail = new InboundMail(
                Uuids.v7(),
                workspace,
                message.messageId().orElseGet(() -> digest(raw)),
                platform,
                message.sender(),
                message.subject(),
                message.sentAt(),
                now,
                status);
        if (!mails.save(mail, raw)) {
            return Outcome.DUPLICATE;
        }
        parsed.ifPresent(content -> {
            content.jobs().forEach(listing -> events.publishEvent(new JobDiscovered(listing, workspace)));
            content.messages().forEach(item -> events.publishEvent(new MessageReceived(item, workspace)));
        });
        return Outcome.STORED;
    }

    /**
     * A parser that fails on an unexpected mail must not reject it: the routing service would retry it forever. The
     * mail is stored as unrecognized instead, and its raw message stays available for a fixed parser.
     */
    private Optional<ParsedMail> parse(PlatformCode platform, MailMessage message, Instant receivedAt) {
        for (PlatformMailParser parser : platforms.mailParsers(platform)) {
            try {
                Optional<ParsedMail> parsed = parser.parse(message, receivedAt);
                if (parsed.isPresent()) {
                    return parsed;
                }
            } catch (RuntimeException exception) {
                log.warn("{} failed on a {} mail", parser.getClass().getSimpleName(), platform.value(), exception);
            }
        }
        return Optional.empty();
    }

    /** Stands in for a missing Message-ID, so identical re-deliveries are still recognized. */
    private static String digest(byte[] raw) {
        try {
            return "sha256:"
                    + HexFormat.of()
                            .formatHex(MessageDigest.getInstance("SHA-256").digest(raw));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
