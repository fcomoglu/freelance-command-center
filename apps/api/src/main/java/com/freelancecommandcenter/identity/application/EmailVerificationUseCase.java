package com.freelancecommandcenter.identity.application;

import com.freelancecommandcenter.identity.application.port.out.IdentityMailer;
import com.freelancecommandcenter.identity.application.port.out.OneTimeTokenRepository;
import com.freelancecommandcenter.identity.application.port.out.UserRepository;
import com.freelancecommandcenter.identity.domain.InvalidTokenException;
import com.freelancecommandcenter.identity.domain.OneTimeToken;
import com.freelancecommandcenter.identity.domain.OpaqueToken;
import com.freelancecommandcenter.identity.domain.TokenPurpose;
import com.freelancecommandcenter.identity.domain.User;
import com.freelancecommandcenter.shared.id.UserId;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sends verification links and confirms e-mail addresses. */
@Service
public class EmailVerificationUseCase {

    private final UserRepository users;
    private final OneTimeTokenRepository tokens;
    private final IdentityMailer mailer;
    private final Clock clock;

    EmailVerificationUseCase(UserRepository users, OneTimeTokenRepository tokens, IdentityMailer mailer, Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.mailer = mailer;
        this.clock = clock;
    }

    /** Sends a new verification link; earlier links stop working. Does nothing for verified users. */
    @Transactional
    public void sendVerification(UserId userId) {
        User user = users.findById(userId).orElseThrow();
        if (user.isEmailVerified()) {
            return;
        }
        Instant now = clock.instant();
        OpaqueToken token = OpaqueToken.generate();
        tokens.invalidateOpen(userId, TokenPurpose.EMAIL_VERIFICATION, now);
        tokens.save(OneTimeToken.issue(userId, TokenPurpose.EMAIL_VERIFICATION, token.hash(), now));
        mailer.sendEmailVerification(user, token);
    }

    @Transactional
    public void verify(String rawToken) {
        Instant now = clock.instant();
        OneTimeToken token = tokens.findByHash(new OpaqueToken(rawToken).hash())
                .filter(found -> found.purpose() == TokenPurpose.EMAIL_VERIFICATION)
                .orElseThrow(InvalidTokenException::new);
        token.consume(now);
        tokens.save(token);

        User user = users.findById(token.userId()).orElseThrow(InvalidTokenException::new);
        user.verifyEmail(now);
        users.save(user);
    }
}
