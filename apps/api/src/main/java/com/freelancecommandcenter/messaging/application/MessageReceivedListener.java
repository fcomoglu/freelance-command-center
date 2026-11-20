package com.freelancecommandcenter.messaging.application;

import com.freelancecommandcenter.platform.api.MessageReceived;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Records messages after the platform module has committed them; failed deliveries stay in the outbox. */
@Component
class MessageReceivedListener {

    private final RecordMessageUseCase messages;

    MessageReceivedListener(RecordMessageUseCase messages) {
        this.messages = messages;
    }

    @ApplicationModuleListener
    void on(MessageReceived event) {
        messages.record(event);
    }
}
