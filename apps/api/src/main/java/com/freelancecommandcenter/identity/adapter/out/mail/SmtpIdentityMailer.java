package com.freelancecommandcenter.identity.adapter.out.mail;

import com.freelancecommandcenter.identity.application.IdentityProperties;
import com.freelancecommandcenter.identity.application.port.out.IdentityMailer;
import com.freelancecommandcenter.identity.domain.OpaqueToken;
import com.freelancecommandcenter.identity.domain.User;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Plain-text e-mails rendered from {@code messages*.properties} in the user's language. */
@Component
class SmtpIdentityMailer implements IdentityMailer {

    private final JavaMailSender mailSender;
    private final MessageSource messages;
    private final IdentityProperties properties;

    SmtpIdentityMailer(JavaMailSender mailSender, MessageSource messages, IdentityProperties properties) {
        this.mailSender = mailSender;
        this.messages = messages;
        this.properties = properties;
    }

    @Override
    public void sendEmailVerification(User user, OpaqueToken token) {
        send(user, "verification", link("/verify-email?token=" + token.value()));
    }

    @Override
    public void sendAlreadyRegistered(User user) {
        send(user, "already-registered", link("/sign-in"));
    }

    @Override
    public void sendPasswordReset(User user, OpaqueToken token) {
        send(user, "password-reset", link("/reset-password?token=" + token.value()));
    }

    private void send(User user, String template, String link) {
        Locale locale = user.language().locale();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.mailFrom());
        message.setTo(user.email().value());
        message.setSubject(messages.getMessage("identity.mail." + template + ".subject", null, locale));
        message.setText(messages.getMessage("identity.mail." + template + ".body", new Object[] {link}, locale));
        mailSender.send(message);
    }

    private String link(String pathAndQuery) {
        return properties.webBaseUrl() + pathAndQuery;
    }
}
