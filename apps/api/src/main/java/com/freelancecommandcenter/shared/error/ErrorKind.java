package com.freelancecommandcenter.shared.error;

/** Category of a {@link DomainException}; the web layer maps it to an HTTP status. */
public enum ErrorKind {
    INVALID,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    TOO_MANY_REQUESTS,
    /** A platform or other service the request depends on cannot be reached. */
    UNAVAILABLE
}
