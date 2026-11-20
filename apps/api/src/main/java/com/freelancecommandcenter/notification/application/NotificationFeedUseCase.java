package com.freelancecommandcenter.notification.application;

import com.freelancecommandcenter.notification.application.port.out.NotificationRepository;
import com.freelancecommandcenter.notification.domain.Notification;
import com.freelancecommandcenter.shared.error.DomainException;
import com.freelancecommandcenter.shared.error.ErrorKind;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.pagination.KeysetCursor;
import com.freelancecommandcenter.shared.pagination.Page;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The workspace's notification feed, newest first, and its read state. */
@Service
public class NotificationFeedUseCase {

    private final NotificationRepository notifications;
    private final Clock clock;

    NotificationFeedUseCase(NotificationRepository notifications, Clock clock) {
        this.notifications = notifications;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<Notification> list(WorkspaceId workspaceId, Optional<KeysetCursor> after, int limit) {
        return Page.of(
                notifications.findRecent(workspaceId, after, limit + 1),
                limit,
                notification -> new KeysetCursor(notification.occurredAt(), notification.id()));
    }

    /** @throws DomainException {@code notification.not_found} when the workspace has no such notification */
    @Transactional
    public void markRead(WorkspaceId workspaceId, UUID notificationId) {
        if (!notifications.markRead(workspaceId, notificationId, clock.instant())) {
            throw new DomainException(
                    ErrorKind.NOT_FOUND, "notification.not_found", "The workspace has no such notification");
        }
    }

    @Transactional
    public void markAllRead(WorkspaceId workspaceId) {
        notifications.markAllRead(workspaceId, clock.instant());
    }
}
