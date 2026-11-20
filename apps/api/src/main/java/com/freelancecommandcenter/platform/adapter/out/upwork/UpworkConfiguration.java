package com.freelancecommandcenter.platform.adapter.out.upwork;

import com.freelancecommandcenter.platform.adapter.out.oauth.OAuthTokenClient;
import com.freelancecommandcenter.platform.application.port.out.AccountConnector;
import com.freelancecommandcenter.platform.application.port.out.JobSource;
import com.freelancecommandcenter.platform.application.port.out.MessageSource;
import com.freelancecommandcenter.shared.http.HttpClients;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Registers the Upwork job source when an API key is configured, and account connections with message reading when
 * they are enabled for the key.
 */
@Configuration(proxyBeanMethods = false)
class UpworkConfiguration {

    static final String JOB_SEARCH_QUERY = "platform/upwork/job-search.graphql";
    static final String CURRENT_USER_QUERY = "platform/upwork/current-user.graphql";
    static final String ROOM_MESSAGES_QUERY = "platform/upwork/room-messages.graphql";

    private static final String ACCOUNT_CONNECTIONS_ENABLED =
            "!'${fcc.platform.upwork.client-id:}'.isBlank()" + " and ${fcc.platform.upwork.account-connections:false}";

    @Bean
    @ConditionalOnExpression("!'${fcc.platform.upwork.client-id:}'.isBlank()")
    JobSource upworkJobSource(UpworkProperties properties, JsonMapper json, JdbcClient jdbc, Clock clock) {
        ClientHttpRequestFactory http = HttpClients.withTimeouts(properties.connectTimeout(), properties.readTimeout());
        UpworkAccessTokens tokens = new UpworkAccessTokens(
                RestClient.builder()
                        .baseUrl(properties.tokenUrl().toString())
                        .requestFactory(http)
                        .build(),
                properties,
                new UpworkRefreshTokenStore(jdbc, clock),
                json,
                clock);
        RestClient api = RestClient.builder()
                .baseUrl(properties.apiUrl().toString())
                .requestFactory(http)
                .requestInterceptor(new UpworkAuthorization(tokens))
                .build();
        return new UpworkJobSource(
                api,
                new UpworkJobTranslator(properties.siteUrl()),
                json,
                query(JOB_SEARCH_QUERY),
                properties.pageSize(),
                properties.maxPagesPerSync());
    }

    @Bean
    @ConditionalOnExpression(ACCOUNT_CONNECTIONS_ENABLED)
    AccountConnector upworkAccountConnector(UpworkProperties properties, JsonMapper json, Clock clock) {
        OAuthTokenClient tokens = new OAuthTokenClient(
                PlatformCode.UPWORK,
                client(properties, properties.tokenUrl().toString()),
                properties.clientId(),
                properties.clientSecret(),
                UpworkAccountConnector.DEFAULT_EXPIRES_IN_SECONDS,
                json,
                clock);
        return new UpworkAccountConnector(tokens, accountApi(properties, json), properties, query(CURRENT_USER_QUERY));
    }

    @Bean
    @ConditionalOnExpression(ACCOUNT_CONNECTIONS_ENABLED)
    MessageSource upworkMessageSource(UpworkProperties properties, JsonMapper json) {
        return new UpworkMessageSource(
                accountApi(properties, json),
                properties.siteUrl(),
                query(ROOM_MESSAGES_QUERY),
                properties.roomPageSize(),
                properties.maxRoomPagesPerSync(),
                properties.storiesPerRoom());
    }

    private static UpworkAccountApi accountApi(UpworkProperties properties, JsonMapper json) {
        return new UpworkAccountApi(client(properties, properties.apiUrl().toString()), json);
    }

    private static RestClient client(UpworkProperties properties, String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(HttpClients.withTimeouts(properties.connectTimeout(), properties.readTimeout()))
                .build();
    }

    static String query(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
