package com.freelancecommandcenter.platform.adapter.out.upwork;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.freelancecommandcenter.platform.application.port.out.PlatformUnavailableException;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class UpworkAccessTokensTest {

    private static final String TOKEN_URL = "https://www.upwork.com/api/v3/oauth2/token";
    private static final String API = "https://api.upwork.com/graphql";

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-20T12:00:00Z"));
    private final InMemoryStore store = new InMemoryStore();
    private final RestClient.Builder tokenBuilder = RestClient.builder().baseUrl(TOKEN_URL);
    private final MockRestServiceServer tokenServer =
            MockRestServiceServer.bindTo(tokenBuilder).build();

    @Test
    void redeemsTheConfiguredRefreshTokenOnceAndKeepsTheRotatedOne() {
        tokenServer
                .expect(once(), requestTo(TOKEN_URL))
                .andExpect(content()
                        .formDataContains(Map.of(
                                "grant_type",
                                "refresh_token",
                                "client_id",
                                "client-id",
                                "refresh_token",
                                "configured")))
                .andRespond(tokenResponse("access-1", "rotated-1"));

        UpworkAccessTokens tokens = tokens("configured");

        assertThat(tokens.current()).isEqualTo("access-1");
        assertThat(tokens.current()).isEqualTo("access-1");
        tokenServer.verify();
        assertThat(store.token).contains("rotated-1");
    }

    @Test
    void refreshesShortlyBeforeExpiryWithTheStoredToken() {
        store.token = Optional.of("stored");
        tokenServer.expect(requestTo(TOKEN_URL)).andRespond(tokenResponse("access-1", "rotated-1"));
        tokenServer
                .expect(requestTo(TOKEN_URL))
                .andExpect(content().formDataContains(Map.of("refresh_token", "rotated-1")))
                .andRespond(tokenResponse("access-2", "rotated-2"));
        UpworkAccessTokens tokens = tokens("configured");

        assertThat(tokens.current()).isEqualTo("access-1");
        clock.advance(Duration.ofHours(24).minusMinutes(4));

        assertThat(tokens.current()).isEqualTo("access-2");
        tokenServer.verify();
    }

    @Test
    void fallsBackToTheConfiguredTokenWhenTheStoredOneIsRejected() {
        store.token = Optional.of("revoked");
        tokenServer
                .expect(requestTo(TOKEN_URL))
                .andExpect(content().formDataContains(Map.of("refresh_token", "revoked")))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));
        tokenServer
                .expect(requestTo(TOKEN_URL))
                .andExpect(content().formDataContains(Map.of("refresh_token", "reauthorized")))
                .andRespond(tokenResponse("access-1", "rotated-1"));

        assertThat(tokens("reauthorized").current()).isEqualTo("access-1");
        assertThat(store.token).contains("rotated-1");
    }

    @Test
    void withoutAUsableRefreshTokenThePlatformIsUnavailable() {
        assertThatThrownBy(() -> tokens("").current())
                .isInstanceOf(PlatformUnavailableException.class)
                .hasMessageContaining("UPWORK_REFRESH_TOKEN");
    }

    @Test
    void anApiRequestRejectedForItsTokenIsRetriedOnceWithAFreshToken() {
        tokenServer.expect(requestTo(TOKEN_URL)).andRespond(tokenResponse("access-1", "rotated-1"));
        tokenServer.expect(requestTo(TOKEN_URL)).andRespond(tokenResponse("access-2", "rotated-2"));
        RestClient.Builder apiBuilder =
                RestClient.builder().baseUrl(API).requestInterceptor(new UpworkAuthorization(tokens("configured")));
        MockRestServiceServer apiServer =
                MockRestServiceServer.bindTo(apiBuilder).build();
        apiServer
                .expect(requestTo(API))
                .andExpect(header("Authorization", "Bearer access-1"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        apiServer
                .expect(requestTo(API))
                .andExpect(header("Authorization", "Bearer access-2"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        String body = apiBuilder.build().post().retrieve().body(String.class);

        assertThat(body).isEqualTo("{}");
        apiServer.verify();
        tokenServer.verify();
    }

    private UpworkAccessTokens tokens(String configuredRefreshToken) {
        UpworkProperties properties = new UpworkProperties(
                URI.create(API),
                URI.create(TOKEN_URL),
                URI.create("https://www.upwork.com/ab/account-security/oauth2/authorize"),
                URI.create("https://www.upwork.com"),
                "client-id",
                "client-secret",
                configuredRefreshToken,
                false,
                50,
                4,
                20,
                3,
                100,
                50,
                Duration.ofSeconds(10),
                Duration.ofSeconds(30));
        return new UpworkAccessTokens(
                tokenBuilder.build(), properties, store, JsonMapper.builder().build(), clock);
    }

    private static ResponseCreator tokenResponse(String accessToken, String refreshToken) {
        return withSuccess(
                "{\"access_token\":\"" + accessToken + "\",\"refresh_token\":\"" + refreshToken
                        + "\",\"token_type\":\"Bearer\",\"expires_in\":86400}",
                MediaType.APPLICATION_JSON);
    }

    private static final class InMemoryStore extends UpworkRefreshTokenStore {

        private Optional<String> token = Optional.empty();

        InMemoryStore() {
            super(null, Clock.systemUTC());
        }

        @Override
        Optional<String> find() {
            return token;
        }

        @Override
        void save(String refreshToken) {
            token = Optional.of(refreshToken);
        }
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
