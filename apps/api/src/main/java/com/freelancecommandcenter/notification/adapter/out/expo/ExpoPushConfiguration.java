package com.freelancecommandcenter.notification.adapter.out.expo;

import com.freelancecommandcenter.notification.application.port.out.PushSender;
import com.freelancecommandcenter.shared.http.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
class ExpoPushConfiguration {

    @Bean
    PushSender expoPushSender(ExpoPushProperties properties, JsonMapper json) {
        RestClient.Builder client = RestClient.builder()
                .baseUrl(properties.sendUrl().toString())
                .requestFactory(HttpClients.withTimeouts(properties.connectTimeout(), properties.readTimeout()));
        if (!properties.accessToken().isBlank()) {
            client.defaultHeaders(headers -> headers.setBearerAuth(properties.accessToken()));
        }
        return new ExpoPushSender(client.build(), json);
    }
}
