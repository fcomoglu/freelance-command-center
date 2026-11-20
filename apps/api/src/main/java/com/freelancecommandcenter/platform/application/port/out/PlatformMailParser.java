package com.freelancecommandcenter.platform.application.port.out;

import com.freelancecommandcenter.platform.api.JobListing;
import com.freelancecommandcenter.platform.api.PlatformMessage;
import com.freelancecommandcenter.platform.api.PlatformNotification;
import com.freelancecommandcenter.platform.domain.MailMessage;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Reads one kind of notification e-mail of a platform that has no API (ADR 0012), such as a job alert, a message notice
 * or an order notice, into the platform-neutral model. A platform can have several parsers; a mail is read by the first one that
 * understands it.
 */
public interface PlatformMailParser {

    PlatformCode platform();

    /**
     * @param receivedAt when the inbox received the mail, for listings the mail gives no date for
     * @return what the mail contains; empty when this parser does not understand the mail
     */
    Optional<ParsedMail> parse(MailMessage mail, Instant receivedAt);

    /** The content of an understood mail; a mail can be understood and still carry nothing to show. */
    record ParsedMail(List<JobListing> jobs, List<PlatformMessage> messages, List<PlatformNotification> notifications) {

        public ParsedMail {
            jobs = List.copyOf(Objects.requireNonNull(jobs, "jobs"));
            messages = List.copyOf(Objects.requireNonNull(messages, "messages"));
            notifications = List.copyOf(Objects.requireNonNull(notifications, "notifications"));
        }

        public static ParsedMail ofJobs(List<JobListing> jobs) {
            return new ParsedMail(jobs, List.of(), List.of());
        }

        public static ParsedMail ofMessages(List<PlatformMessage> messages) {
            return new ParsedMail(List.of(), messages, List.of());
        }

        public static ParsedMail ofNotifications(List<PlatformNotification> notifications) {
            return new ParsedMail(List.of(), List.of(), notifications);
        }
    }
}
