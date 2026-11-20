package com.freelancecommandcenter.platform.adapter.in.scheduling;

import com.freelancecommandcenter.platform.application.MessageSyncUseCase;
import com.freelancecommandcenter.platform.application.PlatformProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.FixedDelayTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Reads the messages of connected accounts with a fixed pause between runs, so a slow platform never causes
 * overlapping reads. Assumes a single API instance.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnBooleanProperty(name = "fcc.platform.connections.message-sync.enabled", matchIfMissing = true)
class MessageSyncScheduling implements SchedulingConfigurer {

    private final MessageSyncUseCase messageSync;
    private final PlatformProperties properties;

    MessageSyncScheduling(MessageSyncUseCase messageSync, PlatformProperties properties) {
        this.messageSync = messageSync;
        this.properties = properties;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        PlatformProperties.MessageSync settings = properties.connections().messageSync();
        registrar.addFixedDelayTask(
                new FixedDelayTask(messageSync::syncAll, settings.interval(), settings.initialDelay()));
    }
}
