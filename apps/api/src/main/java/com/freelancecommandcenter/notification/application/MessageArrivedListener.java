package com.freelancecommandcenter.notification.application;

import com.freelancecommandcenter.messaging.api.MessageArrived;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Notifies each new message after messaging has committed it. */
@Component
class MessageArrivedListener {

    private final NotifyMessageUseCase notifications;

    MessageArrivedListener(NotifyMessageUseCase notifications) {
        this.notifications = notifications;
    }

    @ApplicationModuleListener
    void on(MessageArrived event) {
        notifications.notify(event);
    }
}
