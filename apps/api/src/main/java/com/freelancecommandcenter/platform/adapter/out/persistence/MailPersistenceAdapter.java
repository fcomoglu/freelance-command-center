package com.freelancecommandcenter.platform.adapter.out.persistence;

import com.freelancecommandcenter.platform.application.port.out.InboundMailStore;
import com.freelancecommandcenter.platform.application.port.out.MailInboxRepository;
import com.freelancecommandcenter.platform.domain.InboundMail;
import com.freelancecommandcenter.platform.domain.InboundMail.Status;
import com.freelancecommandcenter.platform.domain.MailInbox;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.pagination.KeysetCursor;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Inbox addresses and received mails in the {@code platform} schema. */
@Component
class MailPersistenceAdapter implements MailInboxRepository, InboundMailStore {

    private static final String MAIL_COLUMNS =
            "id, workspace_id, message_id, platform, sender, subject, sent_at, received_at, status";

    private final JdbcClient jdbc;
    private final Clock clock;

    MailPersistenceAdapter(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Override
    public Optional<MailInbox> findByWorkspace(WorkspaceId workspaceId) {
        return jdbc.sql("SELECT workspace_id, local_part FROM platform.mail_inboxes WHERE workspace_id = ?")
                .param(workspaceId.value())
                .query(MailPersistenceAdapter::inbox)
                .optional();
    }

    @Override
    public Optional<MailInbox> findByLocalPart(String localPart) {
        return jdbc.sql("SELECT workspace_id, local_part FROM platform.mail_inboxes WHERE local_part = ?")
                .param(localPart)
                .query(MailPersistenceAdapter::inbox)
                .optional();
    }

    @Override
    public void save(MailInbox inbox) {
        jdbc.sql("""
                        INSERT INTO platform.mail_inboxes (workspace_id, local_part, created_at) VALUES (?, ?, ?)
                        ON CONFLICT (workspace_id)
                        DO UPDATE SET local_part = EXCLUDED.local_part, created_at = EXCLUDED.created_at
                        """)
                .params(inbox.workspaceId().value(), inbox.localPart(), Timestamp.from(clock.instant()))
                .update();
    }

    @Override
    public boolean save(InboundMail mail, byte[] raw) {
        return jdbc.sql("INSERT INTO platform.inbound_mails (" + MAIL_COLUMNS + ", raw) "
                                + "VALUES (:id, :workspaceId, :messageId, :platform, :sender, :subject, :sentAt, "
                                + ":receivedAt, :status, :raw) ON CONFLICT (workspace_id, message_id) DO NOTHING")
                        .param("id", mail.id())
                        .param("workspaceId", mail.workspaceId().value())
                        .param("messageId", mail.messageId())
                        .param(
                                "platform",
                                mail.platform().map(PlatformCode::value).orElse(null))
                        .param("sender", mail.sender())
                        .param("subject", mail.subject())
                        .param("sentAt", mail.sentAt().map(Timestamp::from).orElse(null))
                        .param("receivedAt", Timestamp.from(mail.receivedAt()))
                        .param("status", mail.status().name())
                        .param("raw", raw)
                        .update()
                == 1;
    }

    @Override
    public List<InboundMail> findRecent(WorkspaceId workspaceId, Optional<KeysetCursor> after, int limit) {
        String sql = "SELECT " + MAIL_COLUMNS + " FROM platform.inbound_mails WHERE workspace_id = :workspaceId"
                + (after.isPresent() ? " AND (received_at, id) < (:at, :id)" : "")
                + " ORDER BY received_at DESC, id DESC LIMIT :limit";
        JdbcClient.StatementSpec statement =
                jdbc.sql(sql).param("workspaceId", workspaceId.value()).param("limit", limit);
        if (after.isPresent()) {
            statement = statement
                    .param("at", Timestamp.from(after.get().at()))
                    .param("id", after.get().id());
        }
        return statement.query(MailPersistenceAdapter::mail).list();
    }

    private static MailInbox inbox(ResultSet row, int rowNumber) throws SQLException {
        return new MailInbox(new WorkspaceId(row.getObject("workspace_id", UUID.class)), row.getString("local_part"));
    }

    private static InboundMail mail(ResultSet row, int rowNumber) throws SQLException {
        Timestamp sentAt = row.getTimestamp("sent_at");
        return new InboundMail(
                row.getObject("id", UUID.class),
                new WorkspaceId(row.getObject("workspace_id", UUID.class)),
                row.getString("message_id"),
                Optional.ofNullable(row.getString("platform")).map(PlatformCode::fromValue),
                row.getString("sender"),
                row.getString("subject"),
                Optional.ofNullable(sentAt).map(Timestamp::toInstant),
                row.getTimestamp("received_at").toInstant(),
                Status.valueOf(row.getString("status")));
    }
}
