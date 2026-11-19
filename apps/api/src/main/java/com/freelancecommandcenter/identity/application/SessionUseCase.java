package com.freelancecommandcenter.identity.application;

import com.freelancecommandcenter.identity.application.port.out.AccessTokenIssuer;
import com.freelancecommandcenter.identity.application.port.out.AccessTokenIssuer.AccessToken;
import com.freelancecommandcenter.identity.application.port.out.AttemptCounter;
import com.freelancecommandcenter.identity.application.port.out.PasswordHasher;
import com.freelancecommandcenter.identity.application.port.out.RefreshTokenRepository;
import com.freelancecommandcenter.identity.application.port.out.UserRepository;
import com.freelancecommandcenter.identity.application.port.out.WorkspaceRepository;
import com.freelancecommandcenter.identity.domain.Email;
import com.freelancecommandcenter.identity.domain.InvalidTokenException;
import com.freelancecommandcenter.identity.domain.OpaqueToken;
import com.freelancecommandcenter.identity.domain.RefreshToken;
import com.freelancecommandcenter.identity.domain.User;
import com.freelancecommandcenter.identity.domain.Workspace;
import com.freelancecommandcenter.shared.error.DomainException;
import com.freelancecommandcenter.shared.error.ErrorKind;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Signs users in and out and rotates refresh tokens. */
@Service
public class SessionUseCase {

    /** Failed sign-ins per e-mail address before further attempts are refused until the window ends. */
    private static final int MAX_FAILED_SIGN_INS = 10;

    private static final Duration FAILED_SIGN_IN_WINDOW = Duration.ofMinutes(15);

    private final UserRepository users;
    private final WorkspaceRepository workspaces;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordHasher passwordHasher;
    private final AccessTokenIssuer accessTokens;
    private final AttemptCounter attempts;
    private final IdentityProperties properties;
    private final Clock clock;
    private final String timingDummyHash;

    SessionUseCase(
            UserRepository users,
            WorkspaceRepository workspaces,
            RefreshTokenRepository refreshTokens,
            PasswordHasher passwordHasher,
            AccessTokenIssuer accessTokens,
            AttemptCounter attempts,
            IdentityProperties properties,
            Clock clock) {
        this.users = users;
        this.workspaces = workspaces;
        this.refreshTokens = refreshTokens;
        this.passwordHasher = passwordHasher;
        this.accessTokens = accessTokens;
        this.attempts = attempts;
        this.properties = properties;
        this.clock = clock;
        this.timingDummyHash = passwordHasher.hash(OpaqueToken.generate().value());
    }

    @Transactional
    public Session signIn(String email, String password) {
        // Counted for unknown addresses too, so a refusal does not reveal whether an account exists.
        String attemptKey = "sign-in:" + email.strip().toLowerCase(Locale.ROOT);
        if (attempts.count(attemptKey) >= MAX_FAILED_SIGN_INS) {
            throw new TooManyAttemptsException();
        }
        Optional<User> user = findUser(email);
        // Unknown addresses still pay the hashing cost, so timing does not reveal registered accounts.
        String hash = user.map(User::passwordHash).orElse(timingDummyHash);
        if (!passwordHasher.matches(password, hash) || user.isEmpty()) {
            attempts.increment(attemptKey, FAILED_SIGN_IN_WINDOW);
            throw new DomainException(
                    ErrorKind.UNAUTHORIZED, "identity.invalid_credentials", "E-mail or password is wrong");
        }
        attempts.reset(attemptKey);
        Workspace workspace = workspaces.findByOwner(user.get().id()).orElseThrow();
        Instant now = clock.instant();
        OpaqueToken refreshToken = OpaqueToken.generate();
        RefreshToken stored = RefreshToken.startFamily(
                user.get().id(), workspace.id(), refreshToken.hash(), now, properties.refreshTokenLifetime());
        refreshTokens.save(stored);
        return session(stored, refreshToken, now);
    }

    /**
     * Exchanges a refresh token for a new pair. A replayed token revokes its whole family; that revocation must
     * survive the error, hence no rollback for {@link InvalidTokenException}.
     */
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public Session refresh(String rawRefreshToken) {
        Instant now = clock.instant();
        RefreshToken current = refreshTokens
                .findByHash(new OpaqueToken(rawRefreshToken).hash())
                .orElseThrow(InvalidTokenException::new);
        if (current.isReplay()) {
            refreshTokens.revokeFamily(current.familyId(), now);
            throw new InvalidTokenException();
        }
        OpaqueToken nextToken = OpaqueToken.generate();
        RefreshToken next = current.rotate(nextToken.hash(), now, properties.refreshTokenLifetime());
        refreshTokens.save(current);
        refreshTokens.save(next);
        return session(next, nextToken, now);
    }

    /** Ends the session of one device. Unknown tokens are ignored so signing out is always safe to repeat. */
    @Transactional
    public void signOut(String rawRefreshToken) {
        refreshTokens
                .findByHash(new OpaqueToken(rawRefreshToken).hash())
                .ifPresent(token -> refreshTokens.revokeFamily(token.familyId(), clock.instant()));
    }

    private Optional<User> findUser(String email) {
        try {
            return users.findByEmail(new Email(email));
        } catch (DomainException invalidEmail) {
            return Optional.empty();
        }
    }

    private Session session(RefreshToken stored, OpaqueToken refreshToken, Instant now) {
        AccessToken accessToken = accessTokens.issue(stored.userId(), stored.workspaceId(), now);
        return new Session(accessToken, refreshToken.value(), stored.expiresAt());
    }

    public record Session(AccessToken accessToken, String refreshToken, Instant refreshTokenExpiresAt) {}
}
