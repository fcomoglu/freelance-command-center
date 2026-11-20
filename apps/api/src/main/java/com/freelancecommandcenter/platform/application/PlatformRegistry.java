package com.freelancecommandcenter.platform.application;

import com.freelancecommandcenter.platform.application.port.out.AccountConnector;
import com.freelancecommandcenter.platform.application.port.out.JobSource;
import com.freelancecommandcenter.platform.application.port.out.MessageSource;
import com.freelancecommandcenter.platform.application.port.out.PlatformMailParser;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/**
 * Selects the adapter (strategy) of a platform for each capability. A platform without a capability, or one that is
 * not configured, simply has no entry, so callers check for presence instead of handling unsupported operations.
 */
@Component
public class PlatformRegistry {

    private final Map<PlatformCode, JobSource> jobSources;
    private final Map<PlatformCode, List<PlatformMailParser>> mailParsers = new EnumMap<>(PlatformCode.class);
    private final Map<PlatformCode, AccountConnector> accountConnectors;
    private final Map<PlatformCode, MessageSource> messageSources;

    public PlatformRegistry(
            List<JobSource> jobSources,
            List<PlatformMailParser> mailParsers,
            List<AccountConnector> accountConnectors,
            List<MessageSource> messageSources) {
        this.jobSources = byPlatform(jobSources, JobSource::platform, "job source");
        this.accountConnectors = byPlatform(accountConnectors, AccountConnector::platform, "account connector");
        this.messageSources = byPlatform(messageSources, MessageSource::platform, "message source");
        for (PlatformMailParser parser : mailParsers) {
            this.mailParsers
                    .computeIfAbsent(parser.platform(), platform -> new ArrayList<>())
                    .add(parser);
        }
    }

    public Optional<JobSource> jobSource(PlatformCode platform) {
        return Optional.ofNullable(jobSources.get(platform));
    }

    public Collection<JobSource> jobSources() {
        return List.copyOf(jobSources.values());
    }

    /** The parsers for the notification e-mails of a platform; each understands its own kinds of mail. */
    public List<PlatformMailParser> mailParsers(PlatformCode platform) {
        return List.copyOf(mailParsers.getOrDefault(platform, List.of()));
    }

    public Optional<AccountConnector> accountConnector(PlatformCode platform) {
        return Optional.ofNullable(accountConnectors.get(platform));
    }

    /** The platforms a user can connect an account of, in {@link PlatformCode} order. */
    public List<PlatformCode> connectablePlatforms() {
        return List.copyOf(accountConnectors.keySet());
    }

    public Collection<MessageSource> messageSources() {
        return List.copyOf(messageSources.values());
    }

    private static <T> Map<PlatformCode, T> byPlatform(
            List<T> adapters, Function<T, PlatformCode> platformOf, String capability) {
        Map<PlatformCode, T> map = new EnumMap<>(PlatformCode.class);
        for (T adapter : adapters) {
            if (map.putIfAbsent(platformOf.apply(adapter), adapter) != null) {
                throw new IllegalStateException("More than one " + capability + " for " + platformOf.apply(adapter));
            }
        }
        return map;
    }
}
