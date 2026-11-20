package com.freelancecommandcenter.notification.application;

import com.freelancecommandcenter.messaging.api.MessageArrived;
import com.freelancecommandcenter.notification.application.port.out.DeviceRepository;
import com.freelancecommandcenter.notification.application.port.out.MessageNotificationLog;
import com.freelancecommandcenter.notification.application.port.out.PushSender;
import com.freelancecommandcenter.notification.application.port.out.PushSender.PushMessage;
import com.freelancecommandcenter.notification.application.port.out.PushSender.PushUnavailableException;
import com.freelancecommandcenter.notification.domain.Device;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pushes a new message to the phones of the workspace, once per message. Best effort like job alerts: a failed push is
 * logged, not retried; the message stays in the inbox.
 */
@Service
public class NotifyMessageUseCase {

    /** Android notification channel of new messages; the app creates it with the same id. */
    public static final String MESSAGES_CHANNEL = "messages";

    private static final Logger log = LoggerFactory.getLogger(NotifyMessageUseCase.class);

    private final MessageNotificationLog notified;
    private final DeviceRepository devices;
    private final PushSender push;
    private final MessageSource messages;
    private final Clock clock;

    NotifyMessageUseCase(
            MessageNotificationLog notified,
            DeviceRepository devices,
            PushSender push,
            MessageSource messages,
            Clock clock) {
        this.notified = notified;
        this.devices = devices;
        this.push = push;
        this.messages = messages;
        this.clock = clock;
    }

    @Transactional
    public void notify(MessageArrived arrived) {
        if (!notified.recordFirst(arrived.workspaceId(), arrived.messageId(), clock.instant())) {
            return;
        }
        List<Device> targets = devices.findByWorkspace(arrived.workspaceId());
        if (targets.isEmpty()) {
            return;
        }
        try {
            List<String> gone = push.send(
                    targets.stream().map(device -> message(device, arrived)).toList());
            if (!gone.isEmpty()) {
                devices.deleteByPushTokens(gone);
            }
        } catch (PushUnavailableException exception) {
            log.warn(
                    "Push for message {} to workspace {} failed: {}",
                    arrived.messageId(),
                    arrived.workspaceId(),
                    exception.getMessage());
        }
    }

    private PushMessage message(Device device, MessageArrived arrived) {
        String platform = messages.getMessage(
                "platform." + arrived.platform().value(),
                null,
                device.language().locale());
        String title = messages.getMessage(
                "notification.push.message-arrived.title",
                new Object[] {platform},
                device.language().locale());
        String body =
                arrived.preview().isEmpty() ? arrived.counterpart() : arrived.counterpart() + ": " + arrived.preview();
        return new PushMessage(
                device.pushToken(),
                title,
                body,
                MESSAGES_CHANNEL,
                Map.of(
                        "type", "message",
                        "conversationId", arrived.conversationId().toString(),
                        "url", arrived.url().toString()));
    }
}
