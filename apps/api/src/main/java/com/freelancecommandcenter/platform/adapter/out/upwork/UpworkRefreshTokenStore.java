package com.freelancecommandcenter.platform.adapter.out.upwork;

import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Keeps the newest Upwork refresh token across restarts. Upwork may issue a new refresh token with every refresh, and
 * an unused one expires after two weeks, so the configured token is only the starting point.
 */
class UpworkRefreshTokenStore {

    private static final String PLATFORM = PlatformCode.UPWORK.value();

    private final JdbcClient jdbc;
    private final Clock clock;

    UpworkRefreshTokenStore(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    Optional<String> find() {
        return jdbc.sql("SELECT refresh_token FROM platform.oauth_refresh_tokens WHERE platform = ?")
                .param(PLATFORM)
                .query(String.class)
                .optional();
    }

    void save(String refreshToken) {
        jdbc.sql("""
                        INSERT INTO platform.oauth_refresh_tokens (platform, refresh_token, updated_at)
                        VALUES (?, ?, ?)
                        ON CONFLICT (platform)
                        DO UPDATE SET refresh_token = EXCLUDED.refresh_token, updated_at = EXCLUDED.updated_at
                        """)
                .params(PLATFORM, refreshToken, Timestamp.from(clock.instant()))
                .update();
    }
}
