package com.freelancecommandcenter.platform.adapter.out.upwork;

import static org.assertj.core.api.Assertions.assertThat;

import com.freelancecommandcenter.IntegrationTest;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

@IntegrationTest
class UpworkRefreshTokenStoreIT {

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private Clock clock;

    @Test
    void keepsOnlyTheNewestRefreshToken() {
        UpworkRefreshTokenStore store = new UpworkRefreshTokenStore(jdbc, clock);

        store.save("first");
        store.save("second");

        assertThat(store.find()).contains("second");
        assertThat(jdbc.sql("SELECT count(*) FROM platform.oauth_refresh_tokens")
                        .query(Long.class)
                        .single())
                .isEqualTo(1);
    }
}
