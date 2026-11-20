package com.freelancecommandcenter;

import com.freelancecommandcenter.platform.api.JobListing;
import com.freelancecommandcenter.platform.api.PlatformMessage;
import com.freelancecommandcenter.platform.application.port.out.PlatformMailParser;
import com.freelancecommandcenter.platform.domain.MailMessage;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * A parser for made-up Arc.dev mails in integration tests. Real Arc.dev mails never start with these prefixes.
 *
 * <ul>
 *   <li>{@value #PREFIX}{@code <title>}: one job whose description is the plain-text body.
 *   <li>{@value #MESSAGE_PREFIX}{@code <name>}: a message notice from {@code <name>} whose text is the plain-text body;
 *       the conversation is named after the sender.
 * </ul>
 */
public class FakeMailParser implements PlatformMailParser {

    public static final String PREFIX = "Test job alert: ";
    public static final String MESSAGE_PREFIX = "Test message from ";

    @Override
    public PlatformCode platform() {
        return PlatformCode.ARC_DEV;
    }

    @Override
    public Optional<ParsedMail> parse(MailMessage mail, Instant receivedAt) {
        if (mail.subject().startsWith(MESSAGE_PREFIX)) {
            return Optional.of(ParsedMail.ofMessages(List.of(message(mail, receivedAt))));
        }
        if (!mail.subject().startsWith(PREFIX)) {
            return Optional.empty();
        }
        String title = mail.subject().substring(PREFIX.length());
        Instant postedAt = mail.sentAt().orElse(receivedAt);
        return Optional.of(ParsedMail.ofJobs(List.of(new JobListing(
                PlatformCode.ARC_DEV,
                title,
                title,
                mail.text().orElse("").strip(),
                URI.create("https://arc.dev/remote-jobs/" + URLEncoder.encode(title, StandardCharsets.UTF_8)),
                null,
                List.of(),
                null,
                null,
                postedAt,
                postedAt))));
    }

    private static PlatformMessage message(MailMessage mail, Instant receivedAt) {
        String counterpart = mail.subject().substring(MESSAGE_PREFIX.length());
        String conversation = counterpart.toLowerCase(Locale.ROOT).replace(' ', '-');
        return new PlatformMessage(
                PlatformCode.ARC_DEV,
                conversation,
                mail.messageId().orElseThrow(),
                counterpart,
                PlatformMessage.Direction.INBOUND,
                PlatformMessage.Detail.NOTICE,
                mail.text().orElse("").strip(),
                URI.create("https://arc.dev/messages/" + conversation),
                mail.sentAt().orElse(receivedAt));
    }
}
