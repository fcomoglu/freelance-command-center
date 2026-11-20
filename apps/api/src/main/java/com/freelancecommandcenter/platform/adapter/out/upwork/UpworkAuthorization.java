package com.freelancecommandcenter.platform.adapter.out.upwork;

import java.io.IOException;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Adds the access token to every API request and, when the API rejects it, retries once with a fresh one. Keeps token
 * handling out of {@link UpworkJobSource}, which contains only the job search.
 */
class UpworkAuthorization implements ClientHttpRequestInterceptor {

    private final UpworkAccessTokens tokens;

    UpworkAuthorization(UpworkAccessTokens tokens) {
        this.tokens = tokens;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        request.getHeaders().setBearerAuth(tokens.current());
        ClientHttpResponse response = execution.execute(request, body);
        if (response.getStatusCode().value() != HttpStatus.UNAUTHORIZED.value()) {
            return response;
        }
        response.close();
        tokens.invalidate();
        request.getHeaders().setBearerAuth(tokens.current());
        return execution.execute(request, body);
    }
}
