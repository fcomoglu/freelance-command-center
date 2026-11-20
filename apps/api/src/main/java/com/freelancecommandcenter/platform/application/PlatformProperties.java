package com.freelancecommandcenter.platform.application;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/**
 * Settings of the platform module ({@code fcc.platform.*}); per-platform settings live with their adapters.
 *
 * @param jobSync the periodic job sync of platforms that offer a job API
 * @param mail the inbound mail of platforms read through forwarded notification e-mails
 * @param connections the platform accounts workspaces connect through OAuth
 */
@ConfigurationProperties("fcc.platform")
public record PlatformProperties(
        @DefaultValue JobSync jobSync,
        @DefaultValue Mail mail,
        @DefaultValue Connections connections) {

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

    /**
     * @param redirectBaseUrl the web client's address; the platforms send users back to
     *     {@code <redirectBaseUrl>/connections/<platform>/callback}, which must be registered with each platform's OAuth
     *     app
     * @param tokenKey base64 AES-256 key the stored tokens are encrypted with; required once a platform's OAuth app is
     *     configured; tokens stored under another key cannot be read
     * @param sync the periodic read of connected accounts
     */
    public record Connections(
            @DefaultValue("http://localhost:5173") URI redirectBaseUrl,
            @DefaultValue("") String tokenKey,
            @DefaultValue Sync sync) {}

    /**
     * @param enabled whether the read runs on a schedule
     * @param interval pause between the end of one read and the start of the next
     * @param initialDelay pause after application start before the first read
     * @param messageLookback how far back the first message read of a new connection goes
     * @param proposalWindow proposals submitted within this period are checked for status changes
     */
    public record Sync(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("2m") Duration interval,
            @DefaultValue("45s") Duration initialDelay,
            @DefaultValue("7d") Duration messageLookback,
            @DefaultValue("30d") Duration proposalWindow) {}
}
