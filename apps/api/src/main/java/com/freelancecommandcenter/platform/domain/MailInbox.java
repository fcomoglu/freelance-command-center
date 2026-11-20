package com.freelancecommandcenter.platform.domain;

import com.freelancecommandcenter.shared.id.WorkspaceId;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The address a workspace forwards platform notification e-mails to. The local part is a random secret: whoever knows
 * it can send mail into the workspace, so it can be replaced at any time.
 */
public record MailInbox(WorkspaceId workspaceId, String localPart) {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] ALPHABET = "abcdefghijklmnopqrstuvwxyz234567".toCharArray();
    /** 20 base32 characters carry 100 random bits. */
    private static final int LENGTH = 20;

    private static final Pattern LOCAL_PART = Pattern.compile("^[a-z2-7]{" + LENGTH + "}$");

    public MailInbox {
        Objects.requireNonNull(workspaceId, "workspaceId");
        if (!LOCAL_PART.matcher(localPart).matches()) {
            throw new IllegalArgumentException("Not an inbox local part");
        }
    }

    /** A new inbox with a fresh random address. */
    public static MailInbox newFor(WorkspaceId workspaceId) {
        char[] chars = new char[LENGTH];
        for (int i = 0; i < LENGTH; i++) {
            chars[i] = ALPHABET[RANDOM.nextInt(ALPHABET.length)];
        }
        return new MailInbox(workspaceId, new String(chars));
    }

    /**
     * The local part of {@code recipient} when it belongs to {@code domain}, ignoring case and a "+tag" suffix that
     * some mail programs add.
     */
    public static Optional<String> localPartOf(String recipient, String domain) {
        String address = recipient.strip().toLowerCase(Locale.ROOT);
        int at = address.lastIndexOf('@');
        if (at <= 0 || !address.substring(at + 1).equals(domain.toLowerCase(Locale.ROOT))) {
            return Optional.empty();
        }
        String local = address.substring(0, at);
        int plus = local.indexOf('+');
        return Optional.of(plus < 0 ? local : local.substring(0, plus));
    }

    public String address(String domain) {
        return localPart + "@" + domain;
    }
}
