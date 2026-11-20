package com.freelancecommandcenter.platform.adapter.out.upwork;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.freelancecommandcenter.platform.adapter.out.oauth.OAuthTokenClient;
import com.freelancecommandcenter.platform.domain.ConnectedAccount;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.json.JsonMapper;

class UpworkAccountConnectorTest {

    private static final String AUTHORIZE = "https://www.upwork.com/ab/account-security/oauth2/authorize";
    private static final String TOKEN = "https://www.upwork.com/api/v3/oauth2/token";
    private static final String API = "https://api.upwork.com/graphql";
    private static final URI REDIRECT = URI.create("http://localhost:5173/connections/upwork/callback");
    private static final Instant NOW = Instant.parse("2026-11-20T10:00:00Z");

    private final RestClient.Builder tokenBuilder = RestClient.builder().baseUrl(TOKEN);
    private final RestClient.Builder apiBuilder = RestClient.builder().baseUrl(API);
    private final MockRestServiceServer tokenServer =
            MockRestServiceServer.bindTo(tokenBuilder).build();
    private final MockRestServiceServer apiServer =
            MockRestServiceServer.bindTo(apiBuilder).build();
    private final UpworkAccountConnector connector = new UpworkAccountConnector(
            new OAuthTokenClient(
                    PlatformCode.UPWORK,
                    tokenBuilder.build(),
                    "client-1",
                    "client-secret",
                    UpworkAccountConnector.DEFAULT_EXPIRES_IN_SECONDS,
                    JsonMapper.builder().build(),
                    Clock.fixed(NOW, ZoneOffset.UTC)),
            new UpworkAccountApi(apiBuilder.build(), JsonMapper.builder().build()),
            new UpworkProperties(
                    URI.create(API),
                    URI.create(TOKEN),
                    URI.create(AUTHORIZE),
                    URI.create("https://www.upwork.com"),
                    "client-1",
                    "client-secret",
                    "",
                    true,
                    50,
                    4,
                    20,
                    3,
                    100,
                    50,
                    Duration.ofSeconds(10),
                    Duration.ofSeconds(30)),
            UpworkConfiguration.query(UpworkConfiguration.CURRENT_USER_QUERY));

    @Test
    void theConsentPageCarriesTheKeyRedirectAndState() {
        URI url = connector.authorizationUrl("state+/=", REDIRECT);

        assertThat(url.toString()).startsWith(AUTHORIZE + "?");
        MultiValueMap<String, String> query =
                UriComponentsBuilder.fromUri(url).build(true).getQueryParams();
        assertThat(decoded(query, "response_type")).isEqualTo("code");
        assertThat(decoded(query, "client_id")).isEqualTo("client-1");
        assertThat(decoded(query, "redirect_uri")).isEqualTo(REDIRECT.toString());
        assertThat(decoded(query, "state")).isEqualTo("state+/=");
    }

    @Test
    void redeemsTheCodeAndIdentifiesTheAccount() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", "the-code");
        form.add("redirect_uri", REDIRECT.toString());
        form.add("client_id", "client-1");
        form.add("client_secret", "client-secret");
        tokenServer
                .expect(requestTo(TOKEN))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formData(form))
                .andRespond(withSuccess("""
                        {"access_token":"oauth2v2_access","refresh_token":"oauth2v2_refresh",\
                        "token_type":"Bearer","expires_in":86400}""", MediaType.APPLICATION_JSON));
        apiServer
                .expect(requestTo(API))
                .andExpect(header("Authorization", "Bearer oauth2v2_access"))
                .andRespond(withSuccess("""
                        {"data":{"user":{"id":"1181921839933591552","name":"Ada L."}}}""", MediaType.APPLICATION_JSON));

        ConnectedAccount account = connector.connect("the-code", REDIRECT);

        tokenServer.verify();
        apiServer.verify();
        assertThat(account.accountId()).isEqualTo("1181921839933591552");
        assertThat(account.name()).isEqualTo("Ada L.");
        assertThat(account.tokens().refreshToken()).isEqualTo("oauth2v2_refresh");
        assertThat(account.tokens().expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(1)));
    }

    private static String decoded(MultiValueMap<String, String> query, String name) {
        return URLDecoder.decode(query.getFirst(name), StandardCharsets.UTF_8);
    }
}
