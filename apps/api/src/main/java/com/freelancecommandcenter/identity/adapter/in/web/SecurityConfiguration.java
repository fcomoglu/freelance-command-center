package com.freelancecommandcenter.identity.adapter.in.web;

import com.freelancecommandcenter.identity.application.IdentityProperties;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Stateless API security. Requests authenticate with a bearer access token; auth, documentation and health endpoints
 * are public. Browsers may call the API only from {@code fcc.identity.web-origins}.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {

    static final String[] PUBLIC_PATHS = {
        "/api/v1/auth/**", "/api/docs/**", "/api/openapi/**", "/swagger-ui/**", "/actuator/health/**", "/error"
    };

    private static final byte[] UNAUTHENTICATED_PROBLEM = """
            {"type":"about:blank","title":"Unauthorized","status":401,\
            "detail":"A valid access token is required","code":"identity.unauthenticated"}""".getBytes(StandardCharsets.UTF_8);

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        AuthenticationEntryPoint unauthenticated = (request, response, exception) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getOutputStream().write(UNAUTHENTICATED_PROBLEM);
        };
        return http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .authorizeHttpRequests(requests -> requests.requestMatchers(PUBLIC_PATHS)
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(
                        server -> server.jwt(Customizer.withDefaults()).authenticationEntryPoint(unauthenticated))
                .exceptionHandling(handling -> handling.authenticationEntryPoint(unauthenticated))
                .build();
    }

    /** Credentials are allowed so the web client can send the refresh cookie. */
    @Bean
    CorsConfigurationSource corsConfigurationSource(IdentityProperties properties) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(properties.webOrigins());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        cors.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT_LANGUAGE, "Idempotency-Key"));
        cors.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }
}
