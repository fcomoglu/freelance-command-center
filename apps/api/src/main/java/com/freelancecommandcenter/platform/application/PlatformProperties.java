package com.freelancecommandcenter.platform.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of the platform module ({@code fcc.platform.*}); per-platform settings live with their adapters.
 *
 * @param jobSync the periodic job sync of platforms that offer a job API
 */
@ConfigurationProperties("fcc.platform")
public record PlatformProperties(@DefaultValue JobSync jobSync) {

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
}
