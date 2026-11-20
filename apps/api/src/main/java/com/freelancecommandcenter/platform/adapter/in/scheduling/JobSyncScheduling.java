package com.freelancecommandcenter.platform.adapter.in.scheduling;

import com.freelancecommandcenter.platform.application.JobSyncUseCase;
import com.freelancecommandcenter.platform.application.PlatformProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.FixedDelayTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Runs the job sync with a fixed pause between runs, so a slow platform never causes overlapping syncs. Assumes a
 * single API instance.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnBooleanProperty(name = "fcc.platform.job-sync.enabled", matchIfMissing = true)
class JobSyncScheduling implements SchedulingConfigurer {

    private final JobSyncUseCase jobSync;
    private final PlatformProperties properties;

    JobSyncScheduling(JobSyncUseCase jobSync, PlatformProperties properties) {
        this.jobSync = jobSync;
        this.properties = properties;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        PlatformProperties.JobSync settings = properties.jobSync();
        registrar.addFixedDelayTask(new FixedDelayTask(jobSync::syncAll, settings.interval(), settings.initialDelay()));
    }
}
