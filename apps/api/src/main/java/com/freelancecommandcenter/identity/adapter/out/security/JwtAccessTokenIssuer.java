package com.freelancecommandcenter.identity.adapter.out.security;

import com.freelancecommandcenter.identity.application.IdentityProperties;
import com.freelancecommandcenter.identity.application.port.out.AccessTokenIssuer;
import com.freelancecommandcenter.shared.id.UserId;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.security.AuthenticatedUser;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
class JwtAccessTokenIssuer implements AccessTokenIssuer {

    private final JwtEncoder encoder;
    private final IdentityProperties properties;

    JwtAccessTokenIssuer(JwtEncoder encoder, IdentityProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    @Override
    public AccessToken issue(UserId userId, WorkspaceId workspaceId, Instant now) {
        Instant expiresAt = now.plus(properties.accessTokenLifetime());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtConfiguration.ISSUER)
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(AuthenticatedUser.WORKSPACE_CLAIM, workspaceId.toString())
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(value, expiresAt);
    }
}
