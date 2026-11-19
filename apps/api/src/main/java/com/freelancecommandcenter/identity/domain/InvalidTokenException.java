package com.freelancecommandcenter.identity.domain;

import com.freelancecommandcenter.shared.error.DomainException;
import com.freelancecommandcenter.shared.error.ErrorKind;

/** A token is unknown, expired, already used or revoked. Deliberately does not say which. */
public final class InvalidTokenException extends DomainException {

    private static final long serialVersionUID = 1L;

    public InvalidTokenException() {
        super(ErrorKind.UNAUTHORIZED, "identity.invalid_token", "Token is invalid or expired");
    }
}
