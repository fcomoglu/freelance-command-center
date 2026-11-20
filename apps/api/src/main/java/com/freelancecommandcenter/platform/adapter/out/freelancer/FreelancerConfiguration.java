package com.freelancecommandcenter.platform.adapter.out.freelancer;

import com.freelancecommandcenter.platform.adapter.out.oauth.OAuthTokenClient;
import com.freelancecommandcenter.platform.application.port.out.AccountConnector;
import com.freelancecommandcenter.platform.application.port.out.JobSource;
import com.freelancecommandcenter.platform.application.port.out.MessageSource;
import com.freelancecommandcenter.platform.application.port.out.ProposalStatusTracker;
import com.freelancecommandcenter.shared.http.HttpClients;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Registers the Freelancer.com job source when an access token is configured, and account connections with message and
 * bid reading when an OAuth app is configured.
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
        OAuthTokenClient tokens = new OAuthTokenClient(
                PlatformCode.FREELANCER,
                client(properties, properties.accountsUrl().resolve(FreelancerAccountConnector.TOKEN_PATH))
                        .build(),
                properties.clientId(),
                properties.clientSecret(),
                FreelancerAccountConnector.DEFAULT_EXPIRES_IN_SECONDS,
                json,
                clock);
        return new FreelancerAccountConnector(
                tokens, client(properties, properties.baseUrl()).build(), properties, json);
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

    @Bean
    @ConditionalOnExpression(OAUTH_APP_CONFIGURED)
    ProposalStatusTracker freelancerProposalStatusTracker(FreelancerProperties properties, JsonMapper json) {
        return new FreelancerProposalStatusTracker(
                client(properties, properties.baseUrl()).build(),
                properties.baseUrl(),
                json,
                properties.bidPageSize(),
                properties.maxBidPagesPerSync());
    }

    private static RestClient.Builder client(FreelancerProperties properties, URI baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl.toString())
                .requestFactory(HttpClients.withTimeouts(properties.connectTimeout(), properties.readTimeout()));
    }
}
