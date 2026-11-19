package com.freelancecommandcenter.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.freelancecommandcenter.ApiClient;
import com.freelancecommandcenter.IntegrationTest;
import com.freelancecommandcenter.Mailbox;
import com.freelancecommandcenter.Mailbox.Mail;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Password reset and resending the verification e-mail. */
@IntegrationTest
class AccountRecoveryIT {

    private static final String PASSWORD = "correct horse battery";
    private static final String NEW_PASSWORD = "a brand new passphrase";

    @Autowired
    private ApiClient api;

    @Autowired
    private Mailbox mailbox;

    @Test
    void passwordResetReplacesThePasswordAndSignsOutEverywhere() {
        String email = registeredUser();
        String oldRefreshToken = (String) signIn(email, PASSWORD).getBody().get("refreshToken");

        assertThat(api.post("/api/v1/auth/password-reset", Map.of("email", email))
                        .getStatusCode())
                .isEqualTo(HttpStatus.ACCEPTED);
        String token = mailbox.awaitMail(email, "Reset your password").token();

        Map<String, String> confirmation = Map.of("token", token, "newPassword", NEW_PASSWORD);
        assertThat(api.post("/api/v1/auth/password-reset/confirmation", confirmation)
                        .getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(signIn(email, PASSWORD).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(signIn(email, NEW_PASSWORD).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(api.post("/api/v1/auth/refresh", Map.of("refreshToken", oldRefreshToken))
                        .getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(api.post("/api/v1/auth/password-reset/confirmation", confirmation)
                        .getBody())
                .containsEntry("code", "identity.invalid_token");
    }

    @Test
    void passwordResetLooksTheSameForUnknownAddresses() {
        ResponseEntity<Map<String, Object>> response =
                api.post("/api/v1/auth/password-reset", Map.of("email", SignUpIT.uniqueEmail()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    void newPasswordMustFollowThePolicy() {
        String email = registeredUser();
        api.post("/api/v1/auth/password-reset", Map.of("email", email));
        String token = mailbox.awaitMail(email, "Reset your password").token();

        ResponseEntity<Map<String, Object>> response =
                api.post("/api/v1/auth/password-reset/confirmation", Map.of("token", token, "newPassword", "short"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("code", "identity.weak_password");
    }

    @Test
    void resentVerificationLinkReplacesTheEarlierOne() {
        String email = registeredUser();
        mailbox.awaitMail(email, "Confirm your e-mail address");
        HttpHeaders bearer = new HttpHeaders();
        bearer.setBearerAuth((String) signIn(email, PASSWORD).getBody().get("accessToken"));

        assertThat(api.post("/api/v1/me/email-verification", Map.of(), bearer).getStatusCode())
                .isEqualTo(HttpStatus.ACCEPTED);

        List<Mail> mails = mailbox.awaitMails(email, "Confirm your e-mail address", 2);
        String newest = mails.get(0).token();
        String earlier = mails.get(1).token();
        assertThat(api.post("/api/v1/auth/email-verification", Map.of("token", earlier))
                        .getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(api.post("/api/v1/auth/email-verification", Map.of("token", newest))
                        .getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(api.get("/api/v1/me", bearer).getBody()).containsEntry("emailVerified", true);
    }

    private String registeredUser() {
        String email = SignUpIT.uniqueEmail();
        api.post("/api/v1/auth/sign-up", Map.of("email", email, "password", PASSWORD));
        return email;
    }

    private ResponseEntity<Map<String, Object>> signIn(String email, String password) {
        return api.post("/api/v1/auth/sign-in", Map.of("email", email, "password", password));
    }
}
