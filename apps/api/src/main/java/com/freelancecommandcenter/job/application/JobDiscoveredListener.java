package com.freelancecommandcenter.job.application;

import com.freelancecommandcenter.platform.api.JobDiscovered;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Catalogs listings after the platform sync has committed; failed deliveries stay in the outbox. */
@Component
class JobDiscoveredListener {

    private final CatalogJobUseCase catalog;

    JobDiscoveredListener(CatalogJobUseCase catalog) {
        this.catalog = catalog;
    }

    @ApplicationModuleListener
    void on(JobDiscovered event) {
        catalog.record(event.listing());
    }
}
