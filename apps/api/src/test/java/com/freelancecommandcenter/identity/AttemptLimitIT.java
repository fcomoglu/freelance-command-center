package com.freelancecommandcenter.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.freelancecommandcenter.ApiClient;
import com.freelancecommandcenter.IntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Limits per e-mail address and per user, kept in Redis. */
@IntegrationTest
class AttemptLimitIT {

    private static final String PASSWORD = "correct horse battery";

    @Autowired
    private ApiClient api;

    @Test
    void tenFailedSignInsBlockTheAddressEvenForTheRightPassword() {
        String email = registeredUser();
        for (int i = 0; i < 10; i++) {
            assertThat(signIn(email, "wrong password here").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        ResponseEntity<Map<String, Object>> blocked = signIn(email, PASSWORD);

        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(blocked.getBody()).containsEntry("code", "identity.too_many_attempts");
    }

    @Test
    void successfulSignInClearsEarlierFailures() {
        String email = registeredUser();
        for (int i = 0; i < 9; i++) {
            signIn(email, "wrong password here");
        }
        assertThat(signIn(email, PASSWORD).getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(signIn(email, "wrong password here").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(signIn(email, PASSWORD).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void passwordResetIsLimitedPerAddressWhetherOrNotItIsRegistered() {
        String unknown = SignUpIT.uniqueEmail();
        for (int i = 0; i < 3; i++) {
            assertThat(requestReset(unknown).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        }

        assertThat(requestReset(unknown).getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void verificationResendsAreLimitedPerUser() {
        String email = registeredUser();
        HttpHeaders bearer = new HttpHeaders();
        bearer.setBearerAuth((String) signIn(email, PASSWORD).getBody().get("accessToken"));
        for (int i = 0; i < 3; i++) {
            assertThat(api.post("/api/v1/me/email-verification", Map.of(), bearer)
                            .getStatusCode())
                    .isEqualTo(HttpStatus.ACCEPTED);
        }

        assertThat(api.post("/api/v1/me/email-verification", Map.of(), bearer).getStatusCode())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    private String registeredUser() {
        String email = SignUpIT.uniqueEmail();
        api.post("/api/v1/auth/sign-up", Map.of("email", email, "password", PASSWORD));
        return email;
    }

    private ResponseEntity<Map<String, Object>> signIn(String email, String password) {
        return api.post("/api/v1/auth/sign-in", Map.of("email", email, "password", password));
    }

    private ResponseEntity<Map<String, Object>> requestReset(String email) {
        return api.post("/api/v1/auth/password-reset", Map.of("email", email));
    }
}
