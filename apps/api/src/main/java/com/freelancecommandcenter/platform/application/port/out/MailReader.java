package com.freelancecommandcenter.platform.application.port.out;

import java.time.Instant;
import java.util.Optional;

/** Reads the headers of a raw MIME message. */
public interface MailReader {

    /** @throws UnreadableMailException when the bytes are not a MIME message */
    MailHeaders headers(byte[] raw);

    /**
     * @param messageId the Message-ID header, if present
     * @param sender the address of the first From header; empty when missing
     */
    record MailHeaders(Optional<String> messageId, String sender, String subject, Optional<Instant> sentAt) {}

    class UnreadableMailException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public UnreadableMailException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
