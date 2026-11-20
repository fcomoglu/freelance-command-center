package com.freelancecommandcenter.platform.adapter.out.mail;

import com.freelancecommandcenter.platform.application.port.out.MailReader;
import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;
import org.springframework.stereotype.Component;

/** Parses MIME messages with Jakarta Mail; no connection to a mail server is involved. */
@Component
class JakartaMailReader implements MailReader {

    private static final Session SESSION = Session.getInstance(new Properties());

    @Override
    public MailHeaders headers(byte[] raw) {
        try {
            MimeMessage message = new MimeMessage(SESSION, new ByteArrayInputStream(raw));
            Address[] from = message.getFrom();
            String sender = from != null && from.length > 0 && from[0] instanceof InternetAddress address
                    ? address.getAddress()
                    : "";
            return new MailHeaders(
                    Optional.ofNullable(message.getMessageID())
                            .map(String::strip)
                            .filter(id -> !id.isEmpty()),
                    sender == null ? "" : sender,
                    Optional.ofNullable(message.getSubject()).orElse(""),
                    Optional.ofNullable(message.getSentDate()).map(Date::toInstant));
        } catch (MessagingException exception) {
            throw new UnreadableMailException("Not a MIME message: " + exception.getMessage(), exception);
        }
    }
}
