package com.freelancecommandcenter.identity.application;

import com.freelancecommandcenter.identity.api.UserRegistered;
import com.freelancecommandcenter.identity.application.port.out.IdentityMailer;
import com.freelancecommandcenter.identity.application.port.out.UserRepository;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Sends e-mails after the triggering transaction has committed. Events are kept in the outbox until the handler
 * succeeds, so a mail server outage delays e-mails instead of losing them.
 */
@Component
class IdentityMailListener {

    private final EmailVerificationUseCase emailVerification;
    private final UserRepository users;
    private final IdentityMailer mailer;

    IdentityMailListener(EmailVerificationUseCase emailVerification, UserRepository users, IdentityMailer mailer) {
        this.emailVerification = emailVerification;
        this.users = users;
        this.mailer = mailer;
    }

    @ApplicationModuleListener
    void on(UserRegistered event) {
        emailVerification.sendVerification(event.userId());
    }

    @ApplicationModuleListener
    void on(ExistingAccountSignUpAttempted event) {
        users.findById(event.userId()).ifPresent(mailer::sendAlreadyRegistered);
    }
}
