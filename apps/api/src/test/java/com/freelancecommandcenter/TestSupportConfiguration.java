package com.freelancecommandcenter;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;

/** Test helpers bound to the running application and its containers. */
@TestConfiguration(proxyBeanMethods = false)
class TestSupportConfiguration {

    @Bean
    @Lazy
    ApiClient apiClient(Environment environment) {
        return new ApiClient("http://localhost:" + environment.getRequiredProperty("local.server.port"));
    }

    @Bean
    Mailbox mailbox(MailpitContainer mailpit) {
        return new Mailbox(mailpit.apiUrl());
    }
}
