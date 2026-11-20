package com.freelancecommandcenter.notification.application;

import com.freelancecommandcenter.platform.api.NotificationReceived;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Records each platform notification after the platform module has committed it. */
@Component
class NotificationReceivedListener {

    private final RecordNotificationUseCase notifications;

    NotificationReceivedListener(RecordNotificationUseCase notifications) {
        this.notifications = notifications;
    }

    @ApplicationModuleListener
    void on(NotificationReceived event) {
        notifications.record(event);
    }
}
