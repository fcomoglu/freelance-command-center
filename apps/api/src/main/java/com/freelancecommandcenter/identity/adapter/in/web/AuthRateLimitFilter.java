package com.freelancecommandcenter.identity.adapter.in.web;

import com.freelancecommandcenter.identity.application.IdentityProperties;
import com.freelancecommandcenter.identity.application.port.out.AttemptCounter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limits auth requests per client IP address, as a first line against credential stuffing and sign-up spam. Limits
 * per e-mail address and per user are enforced by the use cases.
 */
@Component
class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final byte[] TOO_MANY_REQUESTS_PROBLEM = """
            {"type":"about:blank","title":"Too Many Requests","status":429,\
            "detail":"Too many attempts; try again later","code":"identity.too_many_attempts"}""".getBytes(StandardCharsets.UTF_8);

    private final AttemptCounter attempts;
    private final IdentityProperties properties;

    AuthRateLimitFilter(AttemptCounter attempts, IdentityProperties properties) {
        this.attempts = attempts;
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equals(request.getMethod()) && request.getRequestURI().startsWith("/api/v1/auth/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long count = attempts.increment("auth-ip:" + request.getRemoteAddr(), WINDOW);
        if (count > properties.authRequestsPerIpPerMinute()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(WINDOW.toSeconds()));
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getOutputStream().write(TOO_MANY_REQUESTS_PROBLEM);
            return;
        }
        chain.doFilter(request, response);
    }
}
