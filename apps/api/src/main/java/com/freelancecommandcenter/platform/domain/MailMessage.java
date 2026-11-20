package com.freelancecommandcenter.platform.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * The parts of a received e-mail that platform mail parsers read.
 *
 * @param messageId the Message-ID header, if present
 * @param sender the address of the first From header; empty when missing
 * @param text the first {@code text/plain} body part outside attachments
 * @param html the first {@code text/html} body part outside attachments
 */
public record MailMessage(
        Optional<String> messageId,
        String sender,
        String subject,
        Optional<Instant> sentAt,
        Optional<String> text,
        Optional<String> html) {

    public MailMessage {
        Objects.requireNonNull(messageId, "messageId");
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(sentAt, "sentAt");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(html, "html");
    }
}
