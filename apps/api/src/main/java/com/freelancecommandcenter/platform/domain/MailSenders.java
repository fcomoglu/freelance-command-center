package com.freelancecommandcenter.platform.domain;

import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Which platform a notification e-mail comes from, by the domain of its sender address. */
public final class MailSenders {

    /**
     * Platforms read through forwarded e-mails (ADR 0012), the domain they send from, subdomains included, and their
     * site.
     */
    private static final List<Sender> SENDERS = List.of(
            new Sender(PlatformCode.BIONLUK, "bionluk.com", URI.create("https://bionluk.com")),
            new Sender(PlatformCode.FIVERR, "fiverr.com", URI.create("https://www.fiverr.com")),
            new Sender(PlatformCode.ARC_DEV, "arc.dev", URI.create("https://arc.dev")));

    private MailSenders() {}

    public static Optional<PlatformCode> platformOf(String senderAddress) {
        String address = senderAddress.strip().toLowerCase(Locale.ROOT);
        int at = address.lastIndexOf('@');
        if (at < 0) {
            return Optional.empty();
        }
        String host = address.substring(at + 1);
        return SENDERS.stream()
                .filter(sender -> host.equals(sender.domain()) || host.endsWith("." + sender.domain()))
                .map(Sender::platform)
                .findFirst();
    }

    /**
     * The site of a platform read through e-mail, where a notice without a better link sends the user.
     *
     * @throws IllegalArgumentException for a platform that is not read through e-mail
     */
    public static URI siteOf(PlatformCode platform) {
        return SENDERS.stream()
                .filter(sender -> sender.platform() == platform)
                .map(Sender::site)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(platform + " is not read through e-mail"));
    }

    private record Sender(PlatformCode platform, String domain, URI site) {}
}
