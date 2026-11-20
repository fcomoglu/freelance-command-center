package com.freelancecommandcenter.platform.adapter.out.upwork;

import com.freelancecommandcenter.platform.adapter.out.HttpClients;
import com.freelancecommandcenter.platform.application.port.out.JobSource;
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

/** Registers the Upwork job source when an API key is configured. */
@Configuration(proxyBeanMethods = false)
class UpworkConfiguration {

    static final String JOB_SEARCH_QUERY = "platform/upwork/job-search.graphql";

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
                jobSearchQuery(),
                properties.pageSize(),
                properties.maxPagesPerSync());
    }

    static String jobSearchQuery() {
        try {
            return new ClassPathResource(JOB_SEARCH_QUERY).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
