package com.freelancecommandcenter.identity.adapter.out.security;

import com.freelancecommandcenter.identity.application.IdentityProperties;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * RS256 key pair for access tokens. The decoder is the one the resource server uses for every module, so a token is
 * valid exactly when this service signed it and it has not expired.
 */
@Configuration(proxyBeanMethods = false)
class JwtConfiguration {

    static final String ISSUER = "freelance-command-center";

    private static final Logger log = LoggerFactory.getLogger(JwtConfiguration.class);

    @Bean
    JwtSigningKey jwtSigningKey(IdentityProperties properties) {
        if (isBlank(properties.jwtPrivateKey()) || isBlank(properties.jwtPublicKey())) {
            // Tokens then stop working on restart; acceptable locally, never in a deployed environment.
            log.warn("fcc.identity.jwt-private-key/jwt-public-key not set; using a temporary key pair");
            return JwtSigningKey.generate();
        }
        return new JwtSigningKey(
                RsaKeyConverters.x509().convert(stream(properties.jwtPublicKey())),
                RsaKeyConverters.pkcs8().convert(stream(properties.jwtPrivateKey())));
    }

    @Bean
    JwtEncoder jwtEncoder(JwtSigningKey key) {
        try {
            RSAKey jwk = new RSAKey.Builder(key.publicKey())
                    .privateKey(key.privateKey())
                    .keyIDFromThumbprint()
                    .build();
            return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
        } catch (JOSEException e) {
            throw new IllegalStateException("Invalid JWT signing key", e);
        }
    }

    @Bean
    JwtDecoder jwtDecoder(JwtSigningKey key) {
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withPublicKey(key.publicKey()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        return decoder;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static ByteArrayInputStream stream(String pem) {
        return new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8));
    }

    record JwtSigningKey(RSAPublicKey publicKey, RSAPrivateKey privateKey) {

        static JwtSigningKey generate() {
            try {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                KeyPair pair = generator.generateKeyPair();
                return new JwtSigningKey((RSAPublicKey) pair.getPublic(), (RSAPrivateKey) pair.getPrivate());
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
