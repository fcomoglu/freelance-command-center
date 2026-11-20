package com.freelancecommandcenter;

import com.freelancecommandcenter.platform.api.JobListing;
import com.freelancecommandcenter.platform.application.port.out.PlatformMailParser;
import com.freelancecommandcenter.platform.domain.MailMessage;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * A parser for a made-up Arc.dev mail in integration tests: the subject {@value #PREFIX}{@code <title>} carries one job
 * whose description is the plain-text body. Real Arc.dev mails never start with this prefix.
 */
public class FakeMailParser implements PlatformMailParser {

    public static final String PREFIX = "Test job alert: ";

    @Override
    public PlatformCode platform() {
        return PlatformCode.ARC_DEV;
    }

    @Override
    public Optional<ParsedMail> parse(MailMessage mail, Instant receivedAt) {
        if (!mail.subject().startsWith(PREFIX)) {
            return Optional.empty();
        }
        String title = mail.subject().substring(PREFIX.length());
        Instant postedAt = mail.sentAt().orElse(receivedAt);
        return Optional.of(new ParsedMail(List.of(new JobListing(
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
}
