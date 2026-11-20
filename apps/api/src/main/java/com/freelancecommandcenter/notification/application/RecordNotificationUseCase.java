package com.freelancecommandcenter.notification.application;

import com.freelancecommandcenter.notification.application.port.out.DeviceRepository;
import com.freelancecommandcenter.notification.application.port.out.NotificationRepository;
import com.freelancecommandcenter.notification.application.port.out.PushSender;
import com.freelancecommandcenter.notification.application.port.out.PushSender.PushMessage;
import com.freelancecommandcenter.notification.application.port.out.PushSender.PushUnavailableException;
import com.freelancecommandcenter.notification.domain.Device;
import com.freelancecommandcenter.notification.domain.Notification;
import com.freelancecommandcenter.platform.api.NotificationReceived;
import com.freelancecommandcenter.shared.id.Uuids;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adds a platform notification to the workspace's feed once and pushes it to the workspace's phones. Push is best
 * effort like job alerts: a failed push is logged, not retried; the notification stays in the feed.
 */
@Service
public class RecordNotificationUseCase {

    /** Android notification channel of platform notifications; the app creates it with the same id. */
    public static final String NOTIFICATIONS_CHANNEL = "notifications";

    private static final Logger log = LoggerFactory.getLogger(RecordNotificationUseCase.class);

    private final NotificationRepository notifications;
    private final DeviceRepository devices;
    private final PushSender push;
    private final MessageSource messages;

    RecordNotificationUseCase(
            NotificationRepository notifications, DeviceRepository devices, PushSender push, MessageSource messages) {
        this.notifications = notifications;
        this.devices = devices;
        this.push = push;
        this.messages = messages;
    }

    @Transactional
    public void record(NotificationReceived received) {
        Notification notification = Notification.unread(Uuids.v7(), received.workspaceId(), received.notification());
        if (!notifications.add(notification)) {
            return;
        }
        List<Device> targets = devices.findByWorkspace(notification.workspaceId());
        if (targets.isEmpty()) {
            return;
        }
        try {
            List<String> gone = push.send(targets.stream()
                    .map(device -> message(device, notification))
                    .toList());
            if (!gone.isEmpty()) {
                devices.deleteByPushTokens(gone);
            }
        } catch (PushUnavailableException exception) {
            log.warn(
                    "Push for notification {} to workspace {} failed: {}",
                    notification.id(),
                    notification.workspaceId(),
                    exception.getMessage());
        }
    }

    /** A notice without a subject shows the start of its text instead. */
    private PushMessage message(Device device, Notification notification) {
        Locale locale = device.language().locale();
        String platform =
                messages.getMessage("platform." + notification.platform().value(), null, locale);
        String title = messages.getMessage(
                "notification.push.notification." + notification.kind().name().toLowerCase(Locale.ROOT) + ".title",
                new Object[] {platform},
                locale);
        String body = notification.title().isBlank() ? notification.body() : notification.title();
        return new PushMessage(
                device.pushToken(),
                title,
                body,
                NOTIFICATIONS_CHANNEL,
                Map.of(
                        "type", "notification",
                        "notificationId", notification.id().toString(),
                        "url", notification.url().toString()));
    }
}
