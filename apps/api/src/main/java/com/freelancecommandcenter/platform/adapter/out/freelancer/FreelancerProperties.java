package com.freelancecommandcenter.platform.adapter.out.freelancer;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of the Freelancer.com adapter ({@code fcc.platform.freelancer.*}).
 *
 * @param baseUrl site and API host: {@code https://www.freelancer.com} or {@code https://www.freelancer-sandbox.com}
 * @param accountsUrl OAuth host: {@code https://accounts.freelancer.com} or
 *     {@code https://accounts.freelancer-sandbox.com}
 * @param accessToken OAuth access token for the job search; the job search is disabled while it is empty
 * @param clientId OAuth app that users connect their accounts to; account connections are disabled while it is empty
 * @param clientSecret secret of the OAuth app
 * @param advancedScopes space-separated ids of the advanced scopes users are asked for ({@code 2}: fln:project_manage,
 *     for reading bids; {@code 5}: fln:messaging)
 * @param pageSize projects per request; the API returns at most 100
 * @param maxPagesPerSync requests per job sync, which bounds the API usage of one run
 * @param messagePageSize messages per request
 * @param maxMessagePagesPerSync requests per account and message read
 * @param bidPageSize bids per request
 * @param maxBidPagesPerSync requests per account and bid read
 * @param connectTimeout connection timeout of API calls
 * @param readTimeout response timeout of API calls
 */
@ConfigurationProperties("fcc.platform.freelancer")
record FreelancerProperties(
        @DefaultValue("https://www.freelancer.com") URI baseUrl,
        @DefaultValue("https://accounts.freelancer.com") URI accountsUrl,
        @DefaultValue("") String accessToken,
        @DefaultValue("") String clientId,
        @DefaultValue("") String clientSecret,
        @DefaultValue("2 5") String advancedScopes,
        @DefaultValue("100") int pageSize,
        @DefaultValue("5") int maxPagesPerSync,
        @DefaultValue("100") int messagePageSize,
        @DefaultValue("5") int maxMessagePagesPerSync,
        @DefaultValue("100") int bidPageSize,
        @DefaultValue("3") int maxBidPagesPerSync,
        @DefaultValue("10s") Duration connectTimeout,
        @DefaultValue("30s") Duration readTimeout) {}
