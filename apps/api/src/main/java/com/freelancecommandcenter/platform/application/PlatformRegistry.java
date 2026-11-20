package com.freelancecommandcenter.platform.application;

import com.freelancecommandcenter.platform.application.port.out.JobSource;
import com.freelancecommandcenter.platform.application.port.out.PlatformMailParser;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Selects the adapter (strategy) of a platform for each capability. A platform without a capability, or one that is
 * not configured, simply has no entry, so callers check for presence instead of handling unsupported operations.
 */
@Component
public class PlatformRegistry {

    private final Map<PlatformCode, JobSource> jobSources = new EnumMap<>(PlatformCode.class);
    private final Map<PlatformCode, List<PlatformMailParser>> mailParsers = new EnumMap<>(PlatformCode.class);

    public PlatformRegistry(List<JobSource> jobSources, List<PlatformMailParser> mailParsers) {
        for (JobSource source : jobSources) {
            if (this.jobSources.putIfAbsent(source.platform(), source) != null) {
                throw new IllegalStateException("More than one job source for " + source.platform());
            }
        }
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
}
