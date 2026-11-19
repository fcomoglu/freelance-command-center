package com.freelancecommandcenter.identity.application;

import com.freelancecommandcenter.shared.error.DomainException;
import com.freelancecommandcenter.shared.error.ErrorKind;

public final class TooManyAttemptsException extends DomainException {

    private static final long serialVersionUID = 1L;

    public TooManyAttemptsException() {
        super(ErrorKind.TOO_MANY_REQUESTS, "identity.too_many_attempts", "Too many attempts; try again later");
    }
}
