package com.freelancecommandcenter.identity.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.freelancecommandcenter.identity.application.IdentityProperties;
import com.freelancecommandcenter.identity.application.port.out.AttemptCounter;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthRateLimitFilterTest {

    private final AuthRateLimitFilter filter = new AuthRateLimitFilter(new InMemoryAttemptCounter(), properties(2));

    @Test
    void refusesAuthRequestsAboveTheLimitPerIpAddress() throws Exception {
        assertThat(post("/api/v1/auth/sign-in", "10.0.0.1").getStatus()).isEqualTo(200);
        assertThat(post("/api/v1/auth/sign-up", "10.0.0.1").getStatus()).isEqualTo(200);

        MockHttpServletResponse refused = post("/api/v1/auth/sign-in", "10.0.0.1");

        assertThat(refused.getStatus()).isEqualTo(429);
        assertThat(refused.getHeader("Retry-After")).isEqualTo("60");
        assertThat(refused.getContentAsString()).contains("\"code\":\"identity.too_many_attempts\"");
        assertThat(post("/api/v1/auth/sign-in", "10.0.0.2").getStatus()).isEqualTo(200);
    }

    @Test
    void ignoresOtherEndpoints() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertThat(post("/api/v1/me/email-verification", "10.0.0.3").getStatus())
                    .isEqualTo(200);
        }
    }

    private MockHttpServletResponse post(String path, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private static IdentityProperties properties(int requestsPerMinute) {
        return new IdentityProperties(
                "http://localhost", List.of(), "test@example.com", null, null, false, null, null, requestsPerMinute);
    }

    private static final class InMemoryAttemptCounter implements AttemptCounter {

        private final Map<String, Long> counts = new HashMap<>();

        @Override
        public long increment(String key, Duration window) {
            return counts.merge(key, 1L, Long::sum);
        }

        @Override
        public long count(String key) {
            return counts.getOrDefault(key, 0L);
        }

        @Override
        public void reset(String key) {
            counts.remove(key);
        }
    }
}
