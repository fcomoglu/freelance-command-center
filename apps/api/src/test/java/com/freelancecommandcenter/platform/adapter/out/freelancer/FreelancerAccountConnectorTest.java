package com.freelancecommandcenter.platform.adapter.out.freelancer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.freelancecommandcenter.platform.application.port.out.ConnectionRejectedException;
import com.freelancecommandcenter.platform.domain.ConnectedAccount;
import com.freelancecommandcenter.platform.domain.OAuthTokens;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.json.JsonMapper;

class FreelancerAccountConnectorTest {

    private static final String ACCOUNTS = "https://accounts.freelancer.com";
    private static final String SITE = "https://www.freelancer.com";
    private static final URI REDIRECT = URI.create("http://localhost:5173/connections/freelancer/callback");
    private static final Instant NOW = Instant.parse("2026-11-20T10:00:00Z");

    private final RestClient.Builder accountsBuilder = RestClient.builder().baseUrl(ACCOUNTS);
    private final RestClient.Builder apiBuilder = RestClient.builder().baseUrl(SITE);
    private final MockRestServiceServer accounts =
            MockRestServiceServer.bindTo(accountsBuilder).build();
    private final MockRestServiceServer api =
            MockRestServiceServer.bindTo(apiBuilder).build();
    private final FreelancerAccountConnector connector = new FreelancerAccountConnector(
            accountsBuilder.build(),
            apiBuilder.build(),
            new FreelancerProperties(
                    URI.create(SITE),
                    URI.create(ACCOUNTS),
                    "",
                    "client-1",
                    "client-secret",
                    "5",
                    100,
                    5,
                    100,
                    5,
                    Duration.ofSeconds(10),
                    Duration.ofSeconds(30)),
            JsonMapper.builder().build(),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void theConsentPageAsksForMessagingAndCarriesTheState() {
        URI url = connector.authorizationUrl("state+/=", REDIRECT);

        assertThat(url.toString()).startsWith(ACCOUNTS + FreelancerAccountConnector.AUTHORIZE_PATH + "?");
        MultiValueMap<String, String> query =
                UriComponentsBuilder.fromUri(url).build(true).getQueryParams();
        assertThat(decoded(query, "response_type")).isEqualTo("code");
        assertThat(decoded(query, "client_id")).isEqualTo("client-1");
        assertThat(decoded(query, "redirect_uri")).isEqualTo(REDIRECT.toString());
        assertThat(decoded(query, "scope")).isEqualTo("basic");
        assertThat(decoded(query, "advanced_scopes")).isEqualTo("5");
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
        accounts.expect(requestTo(ACCOUNTS + FreelancerAccountConnector.TOKEN_PATH))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formData(form))
                .andRespond(withSuccess("""
                        {"scope":"basic","access_token":"access-1","refresh_token":"refresh-1",\
                        "expires_in":2592000,"token_type":"Bearer"}""", MediaType.APPLICATION_JSON));
        api.expect(requestTo(SITE + FreelancerAccountConnector.SELF_PATH))
                .andExpect(header(FreelancerConfiguration.TOKEN_HEADER, "access-1"))
                .andRespond(withSuccess("""
                        {"status":"success","result":{"id":1001,"username":"ada_l","display_name":"Ada L."}}""", MediaType.APPLICATION_JSON));

        ConnectedAccount account = connector.connect("the-code", REDIRECT);

        accounts.verify();
        api.verify();
        assertThat(account.accountId()).isEqualTo("1001");
        assertThat(account.name()).isEqualTo("Ada L.");
        assertThat(account.tokens().accessToken()).isEqualTo("access-1");
        assertThat(account.tokens().refreshToken()).isEqualTo("refresh-1");
        assertThat(account.tokens().expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
    }

    @Test
    void aRefusedCodeIsRejected() {
        accounts.expect(requestTo(ACCOUNTS + FreelancerAccountConnector.TOKEN_PATH))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> connector.connect("used-code", REDIRECT))
                .isInstanceOf(ConnectionRejectedException.class);
    }

    @Test
    void refreshingKeepsTheRefreshTokenWhenNoNewOneIsIssued() {
        accounts.expect(requestTo(ACCOUNTS + FreelancerAccountConnector.TOKEN_PATH))
                .andExpect(content().string(containsString("grant_type=refresh_token")))
                .andRespond(withSuccess("""
                        {"access_token":"access-2","expires_in":3600}""", MediaType.APPLICATION_JSON));

        OAuthTokens renewed = connector.refresh(new OAuthTokens("access-1", "refresh-1", NOW));

        assertThat(renewed.accessToken()).isEqualTo("access-2");
        assertThat(renewed.refreshToken()).isEqualTo("refresh-1");
        assertThat(renewed.expiresAt()).isEqualTo(NOW.plusSeconds(3600));
    }

    @Test
    void withoutARefreshTokenTheAccountMustBeConnectedAgain() {
        assertThatThrownBy(() -> connector.refresh(new OAuthTokens("access-1", "", NOW)))
                .isInstanceOf(ConnectionRejectedException.class);
    }

    private static String decoded(MultiValueMap<String, String> query, String name) {
        return URLDecoder.decode(query.getFirst(name), StandardCharsets.UTF_8);
    }
}
