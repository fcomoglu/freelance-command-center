package com.freelancecommandcenter.platform.adapter.out.upwork;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of the Upwork adapter ({@code fcc.platform.upwork.*}).
 *
 * @param apiUrl GraphQL endpoint
 * @param tokenUrl OAuth 2 token endpoint
 * @param siteUrl base of the links to job pages
 * @param clientId API key; the adapter is disabled while it is empty
 * @param clientSecret API key secret
 * @param refreshToken refresh token from authorizing the API key once; the adapter stores every newer one itself
 * @param pageSize jobs per request
 * @param maxPagesPerSync requests per sync, which bounds the API usage of one run
 * @param connectTimeout connection timeout of API calls
 * @param readTimeout response timeout of API calls
 */
@ConfigurationProperties("fcc.platform.upwork")
record UpworkProperties(
        @DefaultValue("https://api.upwork.com/graphql") URI apiUrl,

        @DefaultValue("https://www.upwork.com/api/v3/oauth2/token")
        URI tokenUrl,

        @DefaultValue("https://www.upwork.com") URI siteUrl,
        @DefaultValue("") String clientId,
        @DefaultValue("") String clientSecret,
        @DefaultValue("") String refreshToken,
        @DefaultValue("50") int pageSize,
        @DefaultValue("4") int maxPagesPerSync,
        @DefaultValue("10s") Duration connectTimeout,
        @DefaultValue("30s") Duration readTimeout) {}
