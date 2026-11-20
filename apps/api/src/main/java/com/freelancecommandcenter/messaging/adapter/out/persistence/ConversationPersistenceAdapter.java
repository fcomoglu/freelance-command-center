package com.freelancecommandcenter.messaging.adapter.out.persistence;

import com.freelancecommandcenter.messaging.application.port.out.ConversationRepository;
import com.freelancecommandcenter.messaging.domain.Conversation;
import com.freelancecommandcenter.messaging.domain.Message;
import com.freelancecommandcenter.shared.id.ConversationId;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.pagination.KeysetCursor;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Conversations and messages in the {@code messaging} schema. Plain SQL: row locks ({@code FOR UPDATE}) serialize the
 * messages of one conversation, which needs no version column.
 */
@Component
class ConversationPersistenceAdapter implements ConversationRepository {

    private static final String CONVERSATION_COLUMNS = "id, workspace_id, platform, external_id, counterpart, url, "
            + "detail, last_message_at, preview, unread_count";

    private final JdbcClient jdbc;

    ConversationPersistenceAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Conversation lockOrCreate(Conversation fresh) {
        jdbc.sql("INSERT INTO messaging.conversations (" + CONVERSATION_COLUMNS + ") VALUES (:id, :workspaceId, "
                        + ":platform, :externalId, :counterpart, :url, :detail, :lastMessageAt, :preview, "
                        + ":unreadCount) ON CONFLICT (workspace_id, platform, external_id) DO NOTHING")
                .paramSource(params(fresh))
                .update();
        return jdbc.sql("SELECT " + CONVERSATION_COLUMNS + " FROM messaging.conversations "
                        + "WHERE workspace_id = ? AND platform = ? AND external_id = ? FOR UPDATE")
                .params(fresh.workspaceId().value(), fresh.platform().value(), fresh.externalId())
                .query(ConversationPersistenceAdapter::conversation)
                .single();
    }

    @Override
    public Optional<Conversation> lock(WorkspaceId workspaceId, ConversationId conversationId) {
        return select(workspaceId, conversationId, " FOR UPDATE");
    }

    @Override
    public void save(Conversation conversation) {
        jdbc.sql("""
                        UPDATE messaging.conversations SET counterpart = :counterpart, url = :url, detail = :detail,
                            last_message_at = :lastMessageAt, preview = :preview, unread_count = :unreadCount
                        WHERE id = :id
                        """).paramSource(params(conversation)).update();
    }

    @Override
    public boolean addMessage(ConversationId conversationId, Message message) {
        return jdbc.sql("""
                                INSERT INTO messaging.messages
                                    (id, conversation_id, external_id, direction, detail, body, sent_at)
                                VALUES (?, ?, ?, ?, ?, ?, ?)
                                ON CONFLICT (conversation_id, external_id) DO NOTHING
                                """)
                        .params(
                                message.id(),
                                conversationId.value(),
                                message.externalId(),
                                message.direction().name(),
                                message.detail().name(),
                                message.body(),
                                Timestamp.from(message.sentAt()))
                        .update()
                == 1;
    }

    @Override
    public Optional<Conversation> find(WorkspaceId workspaceId, ConversationId conversationId) {
        return select(workspaceId, conversationId, "");
    }

    @Override
    public List<Conversation> findRecent(WorkspaceId workspaceId, Optional<KeysetCursor> after, int limit) {
        String sql =
                "SELECT " + CONVERSATION_COLUMNS + " FROM messaging.conversations WHERE workspace_id = :workspaceId"
                        + (after.isPresent() ? " AND (last_message_at, id) < (:at, :id)" : "")
                        + " ORDER BY last_message_at DESC, id DESC LIMIT :limit";
        JdbcClient.StatementSpec statement =
                jdbc.sql(sql).param("workspaceId", workspaceId.value()).param("limit", limit);
        return after(statement, after)
                .query(ConversationPersistenceAdapter::conversation)
                .list();
    }

    @Override
    public List<Message> findMessages(ConversationId conversationId, Optional<KeysetCursor> after, int limit) {
        String sql = "SELECT id, external_id, direction, detail, body, sent_at FROM messaging.messages"
                + " WHERE conversation_id = :conversationId"
                + (after.isPresent() ? " AND (sent_at, id) < (:at, :id)" : "")
                + " ORDER BY sent_at DESC, id DESC LIMIT :limit";
        JdbcClient.StatementSpec statement =
                jdbc.sql(sql).param("conversationId", conversationId.value()).param("limit", limit);
        return after(statement, after)
                .query(ConversationPersistenceAdapter::message)
                .list();
    }

    private Optional<Conversation> select(WorkspaceId workspaceId, ConversationId conversationId, String suffix) {
        return jdbc.sql("SELECT " + CONVERSATION_COLUMNS + " FROM messaging.conversations "
                        + "WHERE id = ? AND workspace_id = ?" + suffix)
                .params(conversationId.value(), workspaceId.value())
                .query(ConversationPersistenceAdapter::conversation)
                .optional();
    }

    private static JdbcClient.StatementSpec after(JdbcClient.StatementSpec statement, Optional<KeysetCursor> after) {
        return after.map(cursor ->
                        statement.param("at", Timestamp.from(cursor.at())).param("id", cursor.id()))
                .orElse(statement);
    }

    private static MapSqlParameterSource params(Conversation conversation) {
        return new MapSqlParameterSource()
                .addValue("id", conversation.id().value())
                .addValue("workspaceId", conversation.workspaceId().value())
                .addValue("platform", conversation.platform().value())
                .addValue("externalId", conversation.externalId())
                .addValue("counterpart", conversation.counterpart())
                .addValue("url", conversation.url().toString())
                .addValue("detail", conversation.detail().name())
                .addValue("lastMessageAt", Timestamp.from(conversation.lastMessageAt()))
                .addValue("preview", conversation.preview())
                .addValue("unreadCount", conversation.unreadCount());
    }

    private static Conversation conversation(ResultSet row, int rowNumber) throws SQLException {
        return Conversation.restore(
                new ConversationId(row.getObject("id", UUID.class)),
                new WorkspaceId(row.getObject("workspace_id", UUID.class)),
                PlatformCode.fromValue(row.getString("platform")),
                row.getString("external_id"),
                row.getString("counterpart"),
                URI.create(row.getString("url")),
                Message.Detail.valueOf(row.getString("detail")),
                row.getTimestamp("last_message_at").toInstant(),
                row.getString("preview"),
                row.getInt("unread_count"));
    }

    private static Message message(ResultSet row, int rowNumber) throws SQLException {
        return new Message(
                row.getObject("id", UUID.class),
                row.getString("external_id"),
                Message.Direction.valueOf(row.getString("direction")),
                Message.Detail.valueOf(row.getString("detail")),
                row.getString("body"),
                row.getTimestamp("sent_at").toInstant());
    }
}
