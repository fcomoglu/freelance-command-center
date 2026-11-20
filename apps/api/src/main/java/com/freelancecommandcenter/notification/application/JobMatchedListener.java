package com.freelancecommandcenter.notification.application;

import com.freelancecommandcenter.matching.api.JobMatched;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Notifies each match after matching has committed. */
@Component
class JobMatchedListener {

    private final NotifyJobMatchUseCase notifications;

    JobMatchedListener(NotifyJobMatchUseCase notifications) {
        this.notifications = notifications;
    }

    @ApplicationModuleListener
    void on(JobMatched event) {
        notifications.notify(event);
    }
}
