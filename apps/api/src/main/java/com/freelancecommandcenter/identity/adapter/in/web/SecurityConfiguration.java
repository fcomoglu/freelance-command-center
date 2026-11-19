package com.freelancecommandcenter.identity.adapter.in.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/** Stateless API security: public auth and documentation endpoints, everything else requires authentication. */
@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {

    static final String[] PUBLIC_PATHS = {
        "/api/v1/auth/**", "/api/docs/**", "/api/openapi/**", "/swagger-ui/**", "/actuator/health/**", "/error"
    };

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .authorizeHttpRequests(requests -> requests.requestMatchers(PUBLIC_PATHS)
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .build();
    }
}
