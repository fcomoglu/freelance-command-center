package com.freelancecommandcenter.notification.adapter.out.persistence;

import com.freelancecommandcenter.notification.application.port.out.NotificationRepository;
import com.freelancecommandcenter.notification.domain.Notification;
import com.freelancecommandcenter.platform.api.PlatformNotification;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.pagination.KeysetCursor;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** The notification feeds in {@code notification.notifications}. */
@Component
class NotificationFeedPersistenceAdapter implements NotificationRepository {

    private static final String COLUMNS =
            "id, workspace_id, platform, external_id, kind, title, body, url, occurred_at, read_at";

    private final JdbcClient jdbc;

    NotificationFeedPersistenceAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean add(Notification notification) {
        return jdbc.sql("INSERT INTO notification.notifications (" + COLUMNS + ") "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                                + "ON CONFLICT (workspace_id, platform, external_id) DO NOTHING")
                        .params(
                                notification.id(),
                                notification.workspaceId().value(),
                                notification.platform().value(),
                                notification.externalId(),
                                notification.kind().name(),
                                notification.title(),
                                notification.body(),
                                notification.url().toString(),
                                Timestamp.from(notification.occurredAt()),
                                notification.readAt().map(Timestamp::from).orElse(null))
                        .update()
                == 1;
    }

    @Override
    public List<Notification> findRecent(WorkspaceId workspaceId, Optional<KeysetCursor> after, int limit) {
        String sql = "SELECT " + COLUMNS + " FROM notification.notifications WHERE workspace_id = :workspaceId"
                + (after.isPresent() ? " AND (occurred_at, id) < (:at, :id)" : "")
                + " ORDER BY occurred_at DESC, id DESC LIMIT :limit";
        JdbcClient.StatementSpec statement =
                jdbc.sql(sql).param("workspaceId", workspaceId.value()).param("limit", limit);
        return after.map(cursor ->
                        statement.param("at", Timestamp.from(cursor.at())).param("id", cursor.id()))
                .orElse(statement)
                .query(NotificationFeedPersistenceAdapter::notification)
                .list();
    }

    /** Keeps the first read time, so marking twice changes nothing. */
    @Override
    public boolean markRead(WorkspaceId workspaceId, UUID notificationId, Instant at) {
        return jdbc.sql("UPDATE notification.notifications SET read_at = COALESCE(read_at, ?) "
                                + "WHERE id = ? AND workspace_id = ?")
                        .params(Timestamp.from(at), notificationId, workspaceId.value())
                        .update()
                == 1;
    }

    @Override
    public void markAllRead(WorkspaceId workspaceId, Instant at) {
        jdbc.sql("UPDATE notification.notifications SET read_at = ? WHERE workspace_id = ? AND read_at IS NULL")
                .params(Timestamp.from(at), workspaceId.value())
                .update();
    }

    private static Notification notification(ResultSet row, int rowNumber) throws SQLException {
        Timestamp readAt = row.getTimestamp("read_at");
        return new Notification(
                row.getObject("id", UUID.class),
                new WorkspaceId(row.getObject("workspace_id", UUID.class)),
                PlatformCode.fromValue(row.getString("platform")),
                row.getString("external_id"),
                PlatformNotification.Kind.valueOf(row.getString("kind")),
                row.getString("title"),
                row.getString("body"),
                URI.create(row.getString("url")),
                row.getTimestamp("occurred_at").toInstant(),
                Optional.ofNullable(readAt).map(Timestamp::toInstant));
    }
}
