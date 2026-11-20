package com.freelancecommandcenter.platform.adapter.out.freelancer;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of the Freelancer.com adapter ({@code fcc.platform.freelancer.*}).
 *
 * @param baseUrl site and API host: {@code https://www.freelancer.com} or {@code https://www.freelancer-sandbox.com}
 * @param accessToken OAuth access token for the job search; the adapter is disabled while it is empty
 * @param pageSize projects per request; the API returns at most 100
 * @param maxPagesPerSync requests per sync, which bounds the API usage of one run
 * @param connectTimeout connection timeout of API calls
 * @param readTimeout response timeout of API calls
 */
@ConfigurationProperties("fcc.platform.freelancer")
record FreelancerProperties(
        @DefaultValue("https://www.freelancer.com") URI baseUrl,
        @DefaultValue("") String accessToken,
        @DefaultValue("100") int pageSize,
        @DefaultValue("5") int maxPagesPerSync,
        @DefaultValue("10s") Duration connectTimeout,
        @DefaultValue("30s") Duration readTimeout) {}
