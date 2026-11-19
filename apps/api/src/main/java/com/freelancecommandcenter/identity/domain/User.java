package com.freelancecommandcenter.identity.domain;

import com.freelancecommandcenter.shared.i18n.Language;
import com.freelancecommandcenter.shared.id.UserId;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** A person who signs in to the product. Owns exactly one workspace. */
public final class User {

    private final UserId id;
    private final Email email;
    private String passwordHash;
    private final Language language;
    private Instant emailVerifiedAt;
    private final Instant createdAt;

    private User(
            UserId id,
            Email email,
            String passwordHash,
            Language language,
            Instant emailVerifiedAt,
            Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.email = Objects.requireNonNull(email, "email");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.language = Objects.requireNonNull(language, "language");
        this.emailVerifiedAt = emailVerifiedAt;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public static User register(Email email, String passwordHash, Language language, Instant now) {
        return new User(UserId.newId(), email, passwordHash, language, null, now);
    }

    /** Rebuilds a user from storage; does not apply registration rules. */
    public static User restore(
            UserId id,
            Email email,
            String passwordHash,
            Language language,
            Instant emailVerifiedAt,
            Instant createdAt) {
        return new User(id, email, passwordHash, language, emailVerifiedAt, createdAt);
    }

    /** Idempotent: verifying an already verified address keeps the first verification time. */
    public void verifyEmail(Instant now) {
        if (emailVerifiedAt == null) {
            emailVerifiedAt = now;
        }
    }

    public void changePassword(String newPasswordHash) {
        passwordHash = Objects.requireNonNull(newPasswordHash, "newPasswordHash");
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    public UserId id() {
        return id;
    }

    public Email email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public Language language() {
        return language;
    }

    public Optional<Instant> emailVerifiedAt() {
        return Optional.ofNullable(emailVerifiedAt);
    }

    public Instant createdAt() {
        return createdAt;
    }
}
