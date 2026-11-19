package com.freelancecommandcenter.identity.domain;

import com.freelancecommandcenter.shared.error.DomainException;
import com.freelancecommandcenter.shared.error.ErrorKind;

/** Rules a new password must satisfy. The upper bound keeps hashing cost bounded. */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 10;
    public static final int MAX_LENGTH = 128;

    private PasswordPolicy() {}

    public static void check(String rawPassword) {
        int length = rawPassword == null ? 0 : rawPassword.codePointCount(0, rawPassword.length());
        if (length < MIN_LENGTH || length > MAX_LENGTH || rawPassword.isBlank()) {
            throw new DomainException(
                    ErrorKind.INVALID,
                    "identity.weak_password",
                    "Password must be between %d and %d characters".formatted(MIN_LENGTH, MAX_LENGTH));
        }
    }
}
