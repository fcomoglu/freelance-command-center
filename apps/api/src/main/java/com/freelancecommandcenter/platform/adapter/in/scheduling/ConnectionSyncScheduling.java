package com.freelancecommandcenter.platform.adapter.in.scheduling;

import com.freelancecommandcenter.platform.application.ConnectionSyncUseCase;
import com.freelancecommandcenter.platform.application.PlatformProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.FixedDelayTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Reads the connected accounts with a fixed pause between runs, so a slow platform never causes overlapping reads.
 * Assumes a single API instance.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnBooleanProperty(name = "fcc.platform.connections.sync.enabled", matchIfMissing = true)
class ConnectionSyncScheduling implements SchedulingConfigurer {

    private final ConnectionSyncUseCase connectionSync;
    private final PlatformProperties properties;

    ConnectionSyncScheduling(ConnectionSyncUseCase connectionSync, PlatformProperties properties) {
        this.connectionSync = connectionSync;
        this.properties = properties;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        PlatformProperties.Sync settings = properties.connections().sync();
        registrar.addFixedDelayTask(
                new FixedDelayTask(connectionSync::syncAll, settings.interval(), settings.initialDelay()));
    }
}
