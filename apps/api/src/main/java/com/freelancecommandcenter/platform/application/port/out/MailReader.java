package com.freelancecommandcenter.platform.application.port.out;

import com.freelancecommandcenter.platform.domain.MailMessage;

/** Reads a raw MIME message. */
public interface MailReader {

    /**
     * A body that cannot be decoded is left empty instead of failing, so the mail is still stored for a later reading.
     *
     * @throws UnreadableMailException when the bytes are not a MIME message
     */
    MailMessage read(byte[] raw);

    class UnreadableMailException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public UnreadableMailException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
