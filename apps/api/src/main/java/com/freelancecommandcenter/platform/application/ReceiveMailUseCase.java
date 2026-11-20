package com.freelancecommandcenter.platform.application;

import com.freelancecommandcenter.platform.application.port.out.InboundMailStore;
import com.freelancecommandcenter.platform.application.port.out.MailInboxRepository;
import com.freelancecommandcenter.platform.application.port.out.MailReader;
import com.freelancecommandcenter.platform.application.port.out.MailReader.MailHeaders;
import com.freelancecommandcenter.platform.domain.InboundMail;
import com.freelancecommandcenter.platform.domain.InboundMail.Status;
import com.freelancecommandcenter.platform.domain.MailInbox;
import com.freelancecommandcenter.platform.domain.MailSenders;
import com.freelancecommandcenter.shared.id.Uuids;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Accepts a forwarded e-mail for a workspace inbox (ADR 0012): identifies the platform by the sender and stores the
 * raw message. Re-delivered mails (same Message-ID) are stored once.
 */
@Service
public class ReceiveMailUseCase {

    private final MailInboxRepository inboxes;
    private final InboundMailStore mails;
    private final MailReader reader;
    private final String domain;
    private final Clock clock;

    ReceiveMailUseCase(
            MailInboxRepository inboxes,
            InboundMailStore mails,
            MailReader reader,
            PlatformProperties properties,
            Clock clock) {
        this.inboxes = inboxes;
        this.mails = mails;
        this.reader = reader;
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
        MailHeaders headers = reader.headers(raw);
        Optional<PlatformCode> platform = MailSenders.platformOf(headers.sender());
        InboundMail mail = new InboundMail(
                Uuids.v7(),
                inbox.get().workspaceId(),
                headers.messageId().orElseGet(() -> digest(raw)),
                platform,
                headers.sender(),
                headers.subject(),
                headers.sentAt(),
                clock.instant(),
                platform.isPresent() ? Status.UNRECOGNIZED : Status.UNKNOWN_SENDER);
        return mails.save(mail, raw) ? Outcome.STORED : Outcome.DUPLICATE;
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
