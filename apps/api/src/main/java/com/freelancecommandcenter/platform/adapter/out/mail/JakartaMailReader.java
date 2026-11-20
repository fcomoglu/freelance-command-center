package com.freelancecommandcenter.platform.adapter.out.mail;

import com.freelancecommandcenter.platform.application.port.out.MailReader;
import com.freelancecommandcenter.platform.domain.MailMessage;
import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Parses MIME messages with Jakarta Mail; no connection to a mail server is involved. */
@Component
class JakartaMailReader implements MailReader {

    private static final Logger log = LoggerFactory.getLogger(JakartaMailReader.class);
    /** Headers may be raw UTF-8 instead of encoded words (RFC 6532), as in mail relayed by some forwarding services. */
    private static final Session SESSION = Session.getInstance(utf8Headers());

    private static Properties utf8Headers() {
        Properties properties = new Properties();
        properties.setProperty("mail.mime.allowutf8", "true");
        return properties;
    }

    @Override
    public MailMessage read(byte[] raw) {
        try {
            MimeMessage message = new MimeMessage(SESSION, new ByteArrayInputStream(raw));
            Address[] from = message.getFrom();
            String sender = from != null && from.length > 0 && from[0] instanceof InternetAddress address
                    ? address.getAddress()
                    : null;
            Bodies bodies = new Bodies();
            collect(message, bodies);
            return new MailMessage(
                    Optional.ofNullable(message.getMessageID())
                            .map(String::strip)
                            .filter(id -> !id.isEmpty()),
                    sender == null ? "" : sender,
                    Optional.ofNullable(message.getSubject()).orElse(""),
                    Optional.ofNullable(message.getSentDate()).map(Date::toInstant),
                    Optional.ofNullable(bodies.text),
                    Optional.ofNullable(bodies.html));
        } catch (MessagingException exception) {
            throw new UnreadableMailException("Not a MIME message: " + exception.getMessage(), exception);
        }
    }

    /** Walks the MIME tree depth-first and keeps the first plain-text and HTML body outside attachments. */
    private static void collect(Part part, Bodies bodies) throws MessagingException {
        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
            return;
        }
        try {
            if (part.isMimeType("multipart/*") && part.getContent() instanceof Multipart multipart) {
                for (int index = 0; index < multipart.getCount(); index++) {
                    collect(multipart.getBodyPart(index), bodies);
                }
            } else if (bodies.text == null && part.isMimeType("text/plain")) {
                bodies.text = (String) part.getContent();
            } else if (bodies.html == null && part.isMimeType("text/html")) {
                bodies.html = (String) part.getContent();
            }
        } catch (IOException | ClassCastException exception) {
            // An unknown charset or a broken encoding loses only this part; the raw mail is kept for a later reading.
            log.warn("Skipped an undecodable body part: {}", exception.getMessage());
        }
    }

    private static final class Bodies {
        private String text;
        private String html;
    }
}
