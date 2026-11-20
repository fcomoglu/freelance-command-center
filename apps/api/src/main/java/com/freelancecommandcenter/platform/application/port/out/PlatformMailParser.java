package com.freelancecommandcenter.platform.application.port.out;

import com.freelancecommandcenter.platform.api.JobListing;
import com.freelancecommandcenter.platform.domain.MailMessage;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Reads one kind of notification e-mail of a platform that has no API (ADR 0012), such as a job alert, into the
 * platform-neutral model. A platform can have several parsers; a mail is read by the first one that understands it.
 */
public interface PlatformMailParser {

    PlatformCode platform();

    /**
     * @param receivedAt when the inbox received the mail, for listings the mail gives no date for
     * @return what the mail contains; empty when this parser does not understand the mail
     */
    Optional<ParsedMail> parse(MailMessage mail, Instant receivedAt);

    /** The content of an understood mail; a mail can be understood and still carry no jobs. */
    record ParsedMail(List<JobListing> jobs) {

        public ParsedMail {
            jobs = List.copyOf(Objects.requireNonNull(jobs, "jobs"));
        }
    }
}
