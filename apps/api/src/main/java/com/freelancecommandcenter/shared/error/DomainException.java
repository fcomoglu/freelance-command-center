package com.freelancecommandcenter.shared.error;

import java.util.Objects;

/**
 * A failure with a stable, language-neutral {@code code} ({@code <module>.<reason>}) that clients translate. The
 * message is English text for developers and logs.
 */
public class DomainException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorKind kind;
    private final String code;

    public DomainException(ErrorKind kind, String code, String message) {
        super(message);
        this.kind = Objects.requireNonNull(kind, "kind");
        this.code = Objects.requireNonNull(code, "code");
    }

    public ErrorKind kind() {
        return kind;
    }

    public String code() {
        return code;
    }
}
