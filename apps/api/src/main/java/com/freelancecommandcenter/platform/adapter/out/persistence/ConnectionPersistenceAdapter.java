package com.freelancecommandcenter.platform.adapter.out.persistence;

import com.freelancecommandcenter.platform.application.PlatformProperties;
import com.freelancecommandcenter.platform.application.port.out.AccountConnector;
import com.freelancecommandcenter.platform.application.port.out.PlatformConnectionRepository;
import com.freelancecommandcenter.platform.domain.ConnectionAttempt;
import com.freelancecommandcenter.platform.domain.OAuthTokens;
import com.freelancecommandcenter.platform.domain.PlatformConnection;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Connected accounts and started authorizations in the {@code platform} schema, with the tokens encrypted. */
@Component
class ConnectionPersistenceAdapter implements PlatformConnectionRepository {

    private static final String COLUMNS = "id, workspace_id, platform, account_id, account_name, access_token, "
            + "refresh_token, token_expires_at, status, connected_at, messages_read_until, proposals_read_at";

    private final JdbcClient jdbc;
    private final TokenCipher cipher;

    /** Fails at startup when a platform's OAuth app is configured but tokens could not be stored. */
    ConnectionPersistenceAdapter(JdbcClient jdbc, PlatformProperties properties, List<AccountConnector> connectors) {
        this.jdbc = jdbc;
        this.cipher = new TokenCipher(properties.connections().tokenKey());
        if (!connectors.isEmpty() && !cipher.configured()) {
            throw new IllegalStateException("Connecting platform accounts needs a token key: set PLATFORM_TOKEN_KEY"
                    + " (fcc.platform.connections.token-key) to 32 random bytes in base64");
        }
    }

    @Override
    public List<PlatformConnection> findByWorkspace(WorkspaceId workspaceId) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM platform.connections WHERE workspace_id = ? ORDER BY platform")
                .param(workspaceId.value())
                .query(this::connection)
                .list();
    }

    @Override
    public List<PlatformConnection> findActive(PlatformCode platform) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM platform.connections WHERE platform = ? AND status = ?")
                .params(platform.value(), PlatformConnection.Status.ACTIVE.name())
                .query(this::connection)
                .list();
    }

    /**
     * The proposal statuses of the replaced connection belong to its history and go in the same statement, so the new
     * connection starts with a first read.
     */
    @Override
    public void replace(PlatformConnection connection) {
        jdbc.sql("WITH replaced AS (DELETE FROM platform.proposal_statuses WHERE connection_id IN (SELECT id FROM "
                        + "platform.connections WHERE workspace_id = :workspaceId AND platform = :platform)) "
                        + "INSERT INTO platform.connections (" + COLUMNS + ") VALUES (:id, :workspaceId, :platform, "
                        + ":accountId, :accountName, :accessToken, :refreshToken, :expiresAt, :status, :connectedAt, "
                        + ":readUntil, :proposalsReadAt) ON CONFLICT (workspace_id, platform) DO UPDATE SET id = EXCLUDED.id, "
                        + "account_id = EXCLUDED.account_id, account_name = EXCLUDED.account_name, "
                        + "access_token = EXCLUDED.access_token, refresh_token = EXCLUDED.refresh_token, "
                        + "token_expires_at = EXCLUDED.token_expires_at, status = EXCLUDED.status, "
                        + "connected_at = EXCLUDED.connected_at, messages_read_until = EXCLUDED.messages_read_until, "
                        + "proposals_read_at = EXCLUDED.proposals_read_at")
                .param("id", connection.id())
                .param("workspaceId", connection.workspaceId().value())
                .param("platform", connection.platform().value())
                .param("accountId", connection.accountId())
                .param("accountName", connection.accountName())
                .param("accessToken", cipher.encrypt(connection.tokens().accessToken()))
                .param("refreshToken", cipher.encrypt(connection.tokens().refreshToken()))
                .param("expiresAt", Timestamp.from(connection.tokens().expiresAt()))
                .param("status", connection.status().name())
                .param("connectedAt", Timestamp.from(connection.connectedAt()))
                .param(
                        "readUntil",
                        connection.messagesReadUntil().map(Timestamp::from).orElse(null))
                .param(
                        "proposalsReadAt",
                        connection.proposalsReadAt().map(Timestamp::from).orElse(null))
                .update();
    }

    @Override
    public void update(PlatformConnection connection) {
        jdbc.sql("""
                        UPDATE platform.connections
                        SET access_token = :accessToken, refresh_token = :refreshToken, token_expires_at = :expiresAt,
                            status = :status, messages_read_until = :readUntil, proposals_read_at = :proposalsReadAt
                        WHERE id = :id
                        """)
                .param("id", connection.id())
                .param("accessToken", cipher.encrypt(connection.tokens().accessToken()))
                .param("refreshToken", cipher.encrypt(connection.tokens().refreshToken()))
                .param("expiresAt", Timestamp.from(connection.tokens().expiresAt()))
                .param("status", connection.status().name())
                .param(
                        "readUntil",
                        connection.messagesReadUntil().map(Timestamp::from).orElse(null))
                .param(
                        "proposalsReadAt",
                        connection.proposalsReadAt().map(Timestamp::from).orElse(null))
                .update();
    }

    @Override
    public boolean delete(WorkspaceId workspaceId, PlatformCode platform) {
        return jdbc.sql("DELETE FROM platform.connections WHERE workspace_id = ? AND platform = ?")
                        .params(workspaceId.value(), platform.value())
                        .update()
                > 0;
    }

    @Override
    public void saveAttempt(ConnectionAttempt attempt, Instant expiredBefore) {
        jdbc.sql("DELETE FROM platform.connection_attempts WHERE started_at < ?")
                .param(Timestamp.from(expiredBefore))
                .update();
        jdbc.sql("INSERT INTO platform.connection_attempts (state, workspace_id, platform, started_at) "
                        + "VALUES (?, ?, ?, ?)")
                .params(
                        attempt.state(),
                        attempt.workspaceId().value(),
                        attempt.platform().value(),
                        Timestamp.from(attempt.startedAt()))
                .update();
    }

    @Override
    public Optional<ConnectionAttempt> takeAttempt(String state) {
        return jdbc.sql("DELETE FROM platform.connection_attempts WHERE state = ? "
                        + "RETURNING state, workspace_id, platform, started_at")
                .param(state)
                .query((rs, row) -> new ConnectionAttempt(
                        rs.getString("state"),
                        new WorkspaceId(rs.getObject("workspace_id", UUID.class)),
                        PlatformCode.fromValue(rs.getString("platform")),
                        rs.getTimestamp("started_at").toInstant()))
                .optional();
    }

    private PlatformConnection connection(ResultSet rs, int row) throws SQLException {
        Timestamp readUntil = rs.getTimestamp("messages_read_until");
        Timestamp proposalsReadAt = rs.getTimestamp("proposals_read_at");
        return PlatformConnection.restore(
                rs.getObject("id", UUID.class),
                new WorkspaceId(rs.getObject("workspace_id", UUID.class)),
                PlatformCode.fromValue(rs.getString("platform")),
                rs.getString("account_id"),
                rs.getString("account_name"),
                rs.getTimestamp("connected_at").toInstant(),
                new OAuthTokens(
                        cipher.decrypt(rs.getBytes("access_token")),
                        cipher.decrypt(rs.getBytes("refresh_token")),
                        rs.getTimestamp("token_expires_at").toInstant()),
                PlatformConnection.Status.valueOf(rs.getString("status")),
                readUntil == null ? null : readUntil.toInstant(),
                proposalsReadAt == null ? null : proposalsReadAt.toInstant());
    }
}
