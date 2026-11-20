package com.freelancecommandcenter.notification.adapter.out.persistence;

import com.freelancecommandcenter.notification.application.port.out.DeviceRepository;
import com.freelancecommandcenter.notification.application.port.out.JobNotificationLog;
import com.freelancecommandcenter.notification.application.port.out.MessageNotificationLog;
import com.freelancecommandcenter.notification.domain.Device;
import com.freelancecommandcenter.shared.i18n.Language;
import com.freelancecommandcenter.shared.id.JobId;
import com.freelancecommandcenter.shared.id.UserId;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Devices and the notification logs in the {@code notification} schema; plain SQL, since all are simple rows. */
@Component
class NotificationPersistenceAdapter implements DeviceRepository, JobNotificationLog, MessageNotificationLog {

    private final JdbcClient jdbc;
    private final Clock clock;

    NotificationPersistenceAdapter(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Override
    public Device register(Device device) {
        return jdbc.sql("""
                        INSERT INTO notification.devices
                            (id, push_token, user_id, workspace_id, language, registered_at)
                        VALUES (:id, :pushToken, :userId, :workspaceId, :language, :registeredAt)
                        ON CONFLICT (push_token) DO UPDATE SET
                            user_id = EXCLUDED.user_id,
                            workspace_id = EXCLUDED.workspace_id,
                            language = EXCLUDED.language,
                            registered_at = EXCLUDED.registered_at
                        RETURNING id, push_token, user_id, workspace_id, language
                        """)
                .param("id", device.id())
                .param("pushToken", device.pushToken())
                .param("userId", device.userId().value())
                .param("workspaceId", device.workspaceId().value())
                .param("language", device.language().tag())
                .param("registeredAt", Timestamp.from(clock.instant()))
                .query(NotificationPersistenceAdapter::device)
                .single();
    }

    @Override
    public void unregister(UUID deviceId, UserId userId) {
        jdbc.sql("DELETE FROM notification.devices WHERE id = ? AND user_id = ?")
                .params(deviceId, userId.value())
                .update();
    }

    @Override
    public List<Device> findByWorkspace(WorkspaceId workspaceId) {
        return jdbc.sql("""
                        SELECT id, push_token, user_id, workspace_id, language
                        FROM notification.devices WHERE workspace_id = ?
                        """)
                .param(workspaceId.value())
                .query(NotificationPersistenceAdapter::device)
                .list();
    }

    @Override
    public void deleteByPushTokens(Collection<String> pushTokens) {
        jdbc.sql("DELETE FROM notification.devices WHERE push_token IN (:tokens)")
                .param("tokens", pushTokens)
                .update();
    }

    @Override
    public boolean recordFirst(WorkspaceId workspaceId, JobId jobId, Instant at) {
        return jdbc.sql("""
                                INSERT INTO notification.job_notifications (workspace_id, job_id, notified_at)
                                VALUES (?, ?, ?)
                                ON CONFLICT DO NOTHING
                                """)
                        .params(workspaceId.value(), jobId.value(), Timestamp.from(at))
                        .update()
                == 1;
    }

    @Override
    public boolean recordFirst(WorkspaceId workspaceId, UUID messageId, Instant at) {
        return jdbc.sql("""
                                INSERT INTO notification.message_notifications (workspace_id, message_id, notified_at)
                                VALUES (?, ?, ?)
                                ON CONFLICT DO NOTHING
                                """)
                        .params(workspaceId.value(), messageId, Timestamp.from(at))
                        .update()
                == 1;
    }

    private static Device device(ResultSet row, int rowNumber) throws SQLException {
        return new Device(
                row.getObject("id", UUID.class),
                new UserId(row.getObject("user_id", UUID.class)),
                new WorkspaceId(row.getObject("workspace_id", UUID.class)),
                row.getString("push_token"),
                Language.fromTag(row.getString("language")));
    }
}
