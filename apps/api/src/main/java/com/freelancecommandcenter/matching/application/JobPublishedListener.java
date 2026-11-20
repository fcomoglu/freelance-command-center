package com.freelancecommandcenter.matching.application;

import com.freelancecommandcenter.job.api.JobPublished;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Matches each new catalog job once it is committed. A redelivery may publish {@code JobMatched} again; the
 * notification module ignores repeats.
 */
@Component
class JobPublishedListener {

    private final MatchJobUseCase matching;

    JobPublishedListener(MatchJobUseCase matching) {
        this.matching = matching;
    }

    @ApplicationModuleListener
    void on(JobPublished event) {
        matching.match(event);
    }
}
