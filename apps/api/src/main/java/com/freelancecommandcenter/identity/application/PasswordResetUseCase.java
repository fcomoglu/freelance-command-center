package com.freelancecommandcenter.identity.application;

import com.freelancecommandcenter.identity.application.port.out.AttemptCounter;
import com.freelancecommandcenter.identity.application.port.out.IdentityMailer;
import com.freelancecommandcenter.identity.application.port.out.OneTimeTokenRepository;
import com.freelancecommandcenter.identity.application.port.out.PasswordHasher;
import com.freelancecommandcenter.identity.application.port.out.RefreshTokenRepository;
import com.freelancecommandcenter.identity.application.port.out.UserRepository;
import com.freelancecommandcenter.identity.domain.Email;
import com.freelancecommandcenter.identity.domain.InvalidTokenException;
import com.freelancecommandcenter.identity.domain.OneTimeToken;
import com.freelancecommandcenter.identity.domain.OpaqueToken;
import com.freelancecommandcenter.identity.domain.PasswordPolicy;
import com.freelancecommandcenter.identity.domain.TokenPurpose;
import com.freelancecommandcenter.identity.domain.User;
import com.freelancecommandcenter.shared.error.DomainException;
import com.freelancecommandcenter.shared.id.UserId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lets users choose a new password through an e-mailed link. Requests for unknown addresses are accepted silently,
 * and the e-mail is sent after the request returns, so neither the answer nor its timing reveals registered accounts.
 */
@Service
public class PasswordResetUseCase {

    private static final int MAX_REQUESTS = 3;
    private static final Duration REQUEST_WINDOW = Duration.ofHours(1);

    private final UserRepository users;
    private final OneTimeTokenRepository tokens;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordHasher passwordHasher;
    private final IdentityMailer mailer;
    private final AttemptCounter attempts;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    PasswordResetUseCase(
            UserRepository users,
            OneTimeTokenRepository tokens,
            RefreshTokenRepository refreshTokens,
            PasswordHasher passwordHasher,
            IdentityMailer mailer,
            AttemptCounter attempts,
            ApplicationEventPublisher events,
            Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.refreshTokens = refreshTokens;
        this.passwordHasher = passwordHasher;
        this.mailer = mailer;
        this.attempts = attempts;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public void request(String email) {
        // Counted for unknown addresses too, so a refusal does not reveal whether an account exists.
        if (attempts.increment("password-reset:" + email.strip().toLowerCase(Locale.ROOT), REQUEST_WINDOW)
                > MAX_REQUESTS) {
            throw new TooManyAttemptsException();
        }
        Email address;
        try {
            address = new Email(email);
        } catch (DomainException invalidEmail) {
            return;
        }
        users.findByEmail(address).ifPresent(user -> events.publishEvent(new PasswordResetRequested(user.id())));
    }

    /** Sends a new reset link; earlier links stop working. */
    @Transactional
    public void sendResetLink(UserId userId) {
        User user = users.findById(userId).orElseThrow();
        Instant now = clock.instant();
        OpaqueToken token = OpaqueToken.generate();
        tokens.invalidateOpen(userId, TokenPurpose.PASSWORD_RESET, now);
        tokens.save(OneTimeToken.issue(userId, TokenPurpose.PASSWORD_RESET, token.hash(), now));
        mailer.sendPasswordReset(user, token);
    }

    /** Sets the new password and signs the user out everywhere, since the old password may be known to someone. */
    @Transactional
    public void confirm(String rawToken, String newPassword) {
        PasswordPolicy.check(newPassword);
        Instant now = clock.instant();
        OneTimeToken token = tokens.findByHash(new OpaqueToken(rawToken).hash())
                .filter(found -> found.purpose() == TokenPurpose.PASSWORD_RESET)
                .orElseThrow(InvalidTokenException::new);
        token.consume(now);
        tokens.save(token);

        User user = users.findById(token.userId()).orElseThrow(InvalidTokenException::new);
        user.changePassword(passwordHasher.hash(newPassword));
        users.save(user);
        refreshTokens.revokeAllOf(user.id(), now);
    }
}
