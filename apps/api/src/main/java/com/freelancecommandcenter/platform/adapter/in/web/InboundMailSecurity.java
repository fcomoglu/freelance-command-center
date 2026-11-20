package com.freelancecommandcenter.platform.adapter.in.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The inbound mail webhook carries the routing service's shared secret, not a user's access token, so it gets its own
 * filter chain ahead of the API chain; {@link InboundMailWebhook} checks the secret.
 */
@Configuration(proxyBeanMethods = false)
class InboundMailSecurity {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    SecurityFilterChain inboundMailFilterChain(HttpSecurity http) throws Exception {
        return http.securityMatcher(InboundMailWebhook.PATH)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .build();
    }
}
