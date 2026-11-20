package com.freelancecommandcenter.platform.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The parts of a received e-mail that platform mail parsers read.
 *
 * @param messageId the Message-ID header, if present
 * @param sender the address of the first From header; empty when missing
 * @param text the first {@code text/plain} body part outside attachments
 * @param html the first {@code text/html} body part outside attachments
 */
public record MailMessage(
        Optional<String> messageId,
        String sender,
        String subject,
        Optional<Instant> sentAt,
        Optional<String> text,
        Optional<String> html) {

    private static final Pattern INVISIBLE_HTML =
            Pattern.compile("(?is)<!--.*?-->|<(head|style|script)\\b.*?</\\1\\s*>");
    /** Inline elements sit inside a word or sentence, so they vanish; every other tag separates text like a space. */
    private static final Pattern INLINE_TAG = Pattern.compile(
            "(?i)</?(a|abbr|b|bdi|bdo|cite|code|em|font|i|kbd|mark|q|s|small|span|strong|sub|sup|u)\\b[^>]*>");

    private static final Pattern TAG = Pattern.compile("(?s)<[^>]*>");
    private static final Pattern ENTITY = Pattern.compile("&(#x[0-9a-fA-F]+|#[0-9]+|[a-zA-Z]+);");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s\\u00A0\\u200B-\\u200D\\u2060\\uFEFF]+");

    public MailMessage {
        Objects.requireNonNull(messageId, "messageId");
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(sentAt, "sentAt");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(html, "html");
    }

    /**
     * The start of the mail's text on one line, at most {@code maxLength} characters and cut at a word: the plain-text
     * body, or the visible text of the HTML body when there is none; empty for a mail without text.
     */
    public String excerpt(int maxLength) {
        String content = text.filter(body -> !body.isBlank())
                .or(() -> html.map(MailMessage::visibleText))
                .orElse("");
        String line = WHITESPACE.matcher(content).replaceAll(" ").strip();
        if (line.length() <= maxLength) {
            return line;
        }
        int cut = line.lastIndexOf(' ', maxLength - 1);
        return line.substring(0, cut > maxLength / 2 ? cut : maxLength - 1).strip() + "…";
    }

    private static String visibleText(String html) {
        String visible = INVISIBLE_HTML.matcher(html).replaceAll(" ");
        String withoutMarkup =
                TAG.matcher(INLINE_TAG.matcher(visible).replaceAll("")).replaceAll(" ");
        return ENTITY.matcher(withoutMarkup).replaceAll(match -> Matcher.quoteReplacement(decode(match.group(1))));
    }

    /**
     * The entities notification e-mails use; any other entity (including {@code &nbsp;}) becomes a space rather than
     * showing as code.
     */
    private static String decode(String entity) {
        try {
            if (entity.startsWith("#x") || entity.startsWith("#X")) {
                return Character.toString(Integer.parseInt(entity.substring(2), 16));
            }
            if (entity.startsWith("#")) {
                return Character.toString(Integer.parseInt(entity.substring(1)));
            }
        } catch (IllegalArgumentException invalid) {
            return " ";
        }
        return switch (entity) {
            case "amp" -> "&";
            case "lt" -> "<";
            case "gt" -> ">";
            case "quot" -> "\"";
            case "apos" -> "'";
            default -> " ";
        };
    }
}
