package com.freelancecommandcenter.platform.adapter.out.freelancer;

import com.freelancecommandcenter.platform.application.port.out.AccountConnector;
import com.freelancecommandcenter.platform.application.port.out.JobSource;
import com.freelancecommandcenter.platform.application.port.out.MessageSource;
import com.freelancecommandcenter.shared.http.HttpClients;
import java.net.URI;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Registers the Freelancer.com job source when an access token is configured, and account connections with message
 * reading when an OAuth app is configured.
 */
@Configuration(proxyBeanMethods = false)
class FreelancerConfiguration {

    static final String TOKEN_HEADER = "Freelancer-OAuth-V1";

    private static final String OAUTH_APP_CONFIGURED = "!'${fcc.platform.freelancer.client-id:}'.isBlank()";

    @Bean
    @ConditionalOnExpression("!'${fcc.platform.freelancer.access-token:}'.isBlank()")
    JobSource freelancerJobSource(FreelancerProperties properties, JsonMapper json) {
        return new FreelancerJobSource(
                client(properties, properties.baseUrl())
                        .defaultHeader(TOKEN_HEADER, properties.accessToken())
                        .build(),
                new FreelancerProjectTranslator(properties.baseUrl()),
                json,
                properties.pageSize(),
                properties.maxPagesPerSync());
    }

    @Bean
    @ConditionalOnExpression(OAUTH_APP_CONFIGURED)
    AccountConnector freelancerAccountConnector(FreelancerProperties properties, JsonMapper json, Clock clock) {
        return new FreelancerAccountConnector(
                client(properties, properties.accountsUrl()).build(),
                client(properties, properties.baseUrl()).build(),
                properties,
                json,
                clock);
    }

    @Bean
    @ConditionalOnExpression(OAUTH_APP_CONFIGURED)
    MessageSource freelancerMessageSource(FreelancerProperties properties, JsonMapper json) {
        return new FreelancerMessageSource(
                client(properties, properties.baseUrl()).build(),
                properties.baseUrl(),
                json,
                properties.messagePageSize(),
                properties.maxMessagePagesPerSync());
    }

    private static RestClient.Builder client(FreelancerProperties properties, URI baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl.toString())
                .requestFactory(HttpClients.withTimeouts(properties.connectTimeout(), properties.readTimeout()));
    }
}
