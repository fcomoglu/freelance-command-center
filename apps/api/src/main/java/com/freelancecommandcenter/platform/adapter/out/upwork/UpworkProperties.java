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
 * @param authorizeUrl OAuth 2 consent page
 * @param siteUrl base of the links to job pages and message rooms
 * @param clientId API key; the adapter is disabled while it is empty
 * @param clientSecret API key secret
 * @param refreshToken refresh token from authorizing the API key once; the adapter stores every newer one itself
 * @param accountConnections whether users can connect their accounts; the API key then needs the permissions
 *     "Messaging - Read-Only Access", "Common Entities - Read-Only Access" and "Client Proposals - Read And Write
 *     Access" and the connection callback as its redirect URI
 * @param pageSize jobs per request
 * @param maxPagesPerSync requests per sync, which bounds the API usage of one run
 * @param roomPageSize message rooms per request
 * @param maxRoomPagesPerSync requests per account and message read
 * @param storiesPerRoom messages read per room and request
 * @param proposalPageSize most recently changed proposals read per decision (offered, hired, declined) and read
 * @param connectTimeout connection timeout of API calls
 * @param readTimeout response timeout of API calls
 */
@ConfigurationProperties("fcc.platform.upwork")
record UpworkProperties(
        @DefaultValue("https://api.upwork.com/graphql") URI apiUrl,

        @DefaultValue("https://www.upwork.com/api/v3/oauth2/token")
        URI tokenUrl,

        @DefaultValue("https://www.upwork.com/ab/account-security/oauth2/authorize")
        URI authorizeUrl,

        @DefaultValue("https://www.upwork.com") URI siteUrl,
        @DefaultValue("") String clientId,
        @DefaultValue("") String clientSecret,
        @DefaultValue("") String refreshToken,
        @DefaultValue("false") boolean accountConnections,
        @DefaultValue("50") int pageSize,
        @DefaultValue("4") int maxPagesPerSync,
        @DefaultValue("20") int roomPageSize,
        @DefaultValue("3") int maxRoomPagesPerSync,
        @DefaultValue("100") int storiesPerRoom,
        @DefaultValue("50") int proposalPageSize,
        @DefaultValue("10s") Duration connectTimeout,
        @DefaultValue("30s") Duration readTimeout) {}
