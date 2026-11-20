package com.freelancecommandcenter.shared.http;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;

/** HTTP settings shared by the clients of external APIs. */
public final class HttpClients {

    private HttpClients() {}

    /** Bounded connect and response times, so one slow platform cannot stall the sync of the others. */
    public static ClientHttpRequestFactory withTimeouts(Duration connectTimeout, Duration readTimeout) {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(http);
        requestFactory.setReadTimeout(readTimeout);
        return requestFactory;
    }
}
