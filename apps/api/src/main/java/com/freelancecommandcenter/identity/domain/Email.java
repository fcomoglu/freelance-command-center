package com.freelancecommandcenter.identity.domain;

import com.freelancecommandcenter.shared.error.DomainException;
import com.freelancecommandcenter.shared.error.ErrorKind;
import java.util.Locale;
import java.util.regex.Pattern;

/** A syntactically valid e-mail address, normalized to lower case so it can be compared and stored uniquely. */
public record Email(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MAX_LENGTH = 254;

    public Email {
        if (value == null) {
            throw invalid();
        }
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.length() > MAX_LENGTH || !FORMAT.matcher(value).matches()) {
            throw invalid();
        }
    }

    private static DomainException invalid() {
        return new DomainException(ErrorKind.INVALID, "identity.invalid_email", "E-mail address is not valid");
    }

    @Override
    public String toString() {
        return value;
    }
}
