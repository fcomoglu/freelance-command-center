package com.freelancecommandcenter.identity.application;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of the identity module ({@code fcc.identity.*}).
 *
 * @param webBaseUrl base URL of the web client, used for links in e-mails
 * @param webOrigins origins allowed to use the refresh cookie
 * @param mailFrom sender address of identity e-mails
 * @param accessTokenLifetime lifetime of JWT access tokens
 * @param refreshTokenLifetime lifetime of each refresh token in a family
 * @param secureCookies whether the refresh cookie is marked {@code Secure}; off only for local HTTP development
 * @param jwtPrivateKey PEM-encoded RSA private key; a temporary key is generated when empty
 * @param jwtPublicKey PEM-encoded RSA public key matching {@code jwtPrivateKey}
 */
@ConfigurationProperties("fcc.identity")
public record IdentityProperties(
        String webBaseUrl,
        List<String> webOrigins,
        String mailFrom,
        @DefaultValue("15m") Duration accessTokenLifetime,
        @DefaultValue("30d") Duration refreshTokenLifetime,
        @DefaultValue("true") boolean secureCookies,
        String jwtPrivateKey,
        String jwtPublicKey) {}
