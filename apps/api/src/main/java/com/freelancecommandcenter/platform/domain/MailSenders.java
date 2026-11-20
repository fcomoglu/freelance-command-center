package com.freelancecommandcenter.platform.domain;

import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Which platform a notification e-mail comes from, by the domain of its sender address. */
public final class MailSenders {

    /** Platforms read through forwarded e-mails (ADR 0012) and the domains they send from, subdomains included. */
    private static final Map<String, PlatformCode> DOMAINS = Map.of(
            "bionluk.com", PlatformCode.BIONLUK,
            "fiverr.com", PlatformCode.FIVERR,
            "arc.dev", PlatformCode.ARC_DEV);

    private MailSenders() {}

    public static Optional<PlatformCode> platformOf(String senderAddress) {
        String address = senderAddress.strip().toLowerCase(Locale.ROOT);
        int at = address.lastIndexOf('@');
        if (at < 0) {
            return Optional.empty();
        }
        String host = address.substring(at + 1);
        return DOMAINS.entrySet().stream()
                .filter(entry -> host.equals(entry.getKey()) || host.endsWith("." + entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
    }
}
