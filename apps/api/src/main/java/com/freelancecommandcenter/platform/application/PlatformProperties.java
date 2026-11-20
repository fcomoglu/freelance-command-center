package com.freelancecommandcenter.platform.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/**
 * Settings of the platform module ({@code fcc.platform.*}); per-platform settings live with their adapters.
 *
 * @param jobSync the periodic job sync of platforms that offer a job API
 * @param mail the inbound mail of platforms read through forwarded notification e-mails
 */
@ConfigurationProperties("fcc.platform")
public record PlatformProperties(
        @DefaultValue JobSync jobSync, @DefaultValue Mail mail) {

    /**
     * @param enabled whether the sync runs on a schedule
     * @param interval pause between the end of one sync and the start of the next
     * @param initialDelay pause after application start before the first sync
     * @param initialLookback how far back the first sync of a platform reads
     */
    public record JobSync(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("5m") Duration interval,
            @DefaultValue("30s") Duration initialDelay,
            @DefaultValue("1h") Duration initialLookback) {}

    /**
     * @param inboundDomain the domain of the workspace inbox addresses
     * @param webhookSecret bearer token of the mail routing service that posts inbound mail; the webhook rejects
     *     every request while it is empty
     * @param maxSize larger mails are rejected
     */
    public record Mail(
            @DefaultValue("inbound.localhost") String inboundDomain,
            @DefaultValue("") String webhookSecret,
            @DefaultValue("5MB") DataSize maxSize) {}
}
