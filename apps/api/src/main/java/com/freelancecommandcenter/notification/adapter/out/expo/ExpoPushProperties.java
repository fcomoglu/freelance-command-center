package com.freelancecommandcenter.notification.adapter.out.expo;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of the Expo Push Service client ({@code fcc.notification.expo.*}).
 *
 * @param sendUrl the push send endpoint
 * @param accessToken required only when "enhanced push security" is enabled for the Expo project
 * @param connectTimeout connection timeout
 * @param readTimeout response timeout
 */
@ConfigurationProperties("fcc.notification.expo")
record ExpoPushProperties(
        @DefaultValue("https://exp.host/--/api/v2/push/send")
        URI sendUrl,

        @DefaultValue("") String accessToken,
        @DefaultValue("5s") Duration connectTimeout,
        @DefaultValue("15s") Duration readTimeout) {}
