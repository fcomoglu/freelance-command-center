package com.freelancecommandcenter.platform.adapter.out.freelancer;

import com.freelancecommandcenter.platform.application.port.out.JobSource;
import com.freelancecommandcenter.shared.http.HttpClients;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/** Registers the Freelancer.com job source when an access token is configured. */
@Configuration(proxyBeanMethods = false)
class FreelancerConfiguration {

    static final String TOKEN_HEADER = "Freelancer-OAuth-V1";

    @Bean
    @ConditionalOnExpression("!'${fcc.platform.freelancer.access-token:}'.isBlank()")
    JobSource freelancerJobSource(FreelancerProperties properties, JsonMapper json) {
        return new FreelancerJobSource(
                apiClient(properties),
                new FreelancerProjectTranslator(properties.baseUrl()),
                json,
                properties.pageSize(),
                properties.maxPagesPerSync());
    }

    /** An API client authenticated with the configured token. */
    static RestClient apiClient(FreelancerProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl().toString())
                .defaultHeader(TOKEN_HEADER, properties.accessToken())
                .requestFactory(HttpClients.withTimeouts(properties.connectTimeout(), properties.readTimeout()))
                .build();
    }
}
