package com.freelancecommandcenter.matching.domain;

import com.freelancecommandcenter.shared.error.DomainException;
import com.freelancecommandcenter.shared.error.ErrorKind;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A search term of a job filter. It matches whole words only, ignoring case, so "java" does not match "JavaScript";
 * terms with symbols such as "c++" or "node.js" work as typed, and the words of a phrase may be separated by any
 * whitespace.
 */
public final class Keyword {

    public static final int MAX_LENGTH = 50;

    private final String value;
    private final Pattern pattern;

    private Keyword(String value) {
        this.value = value;
        this.pattern = Pattern.compile(
                "(?<![\\p{L}\\p{N}])"
                        + String.join(
                                "\\s+",
                                Arrays.stream(value.split(" "))
                                        .map(Pattern::quote)
                                        .toList())
                        + "(?![\\p{L}\\p{N}])",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    /** Trims, collapses inner whitespace and lower-cases the term. */
    public static Keyword of(String term) {
        String value = term == null ? "" : term.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        if (value.isEmpty() || value.length() > MAX_LENGTH) {
            throw new DomainException(
                    ErrorKind.INVALID, "matching.invalid_keyword", "A keyword has 1 to " + MAX_LENGTH + " characters");
        }
        return new Keyword(value);
    }

    public String value() {
        return value;
    }

    boolean occursIn(String text) {
        return pattern.matcher(text).find();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Keyword keyword && keyword.value.equals(value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
