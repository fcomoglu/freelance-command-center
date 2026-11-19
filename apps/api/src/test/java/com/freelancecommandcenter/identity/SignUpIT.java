package com.freelancecommandcenter.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.freelancecommandcenter.ApiClient;
import com.freelancecommandcenter.IntegrationTest;
import com.freelancecommandcenter.Mailbox;
import com.freelancecommandcenter.Mailbox.Mail;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@IntegrationTest
class SignUpIT {

    private static final String PASSWORD = "correct horse battery";

    @Autowired
    private ApiClient api;

    @Autowired
    private Mailbox mailbox;

    @Test
    void newUserReceivesAVerificationLinkThatWorksOnce() {
        String email = uniqueEmail();

        assertThat(signUp(email).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        Mail mail = mailbox.awaitMail(email, "Confirm your e-mail address");
        Map<String, String> verification = Map.of("token", mail.token());
        assertThat(api.post("/api/v1/auth/email-verification", verification).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Map<String, Object>> reused = api.post("/api/v1/auth/email-verification", verification);
        assertThat(reused.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(reused.getBody()).containsEntry("code", "identity.invalid_token");
    }

    @Test
    void signingUpWithARegisteredAddressLooksTheSameButInformsTheOwner() {
        String email = uniqueEmail();
        signUp(email);
        mailbox.awaitMail(email, "Confirm your e-mail address");

        ResponseEntity<Map<String, Object>> second = signUp(email.toUpperCase());

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(mailbox.awaitMail(email, "Sign-up attempt").text()).contains("/sign-in");
    }

    @Test
    void emailsUseTheLanguageOfTheRequest() {
        String email = uniqueEmail();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT_LANGUAGE, "tr-TR,tr;q=0.9");

        api.post("/api/v1/auth/sign-up", Map.of("email", email, "password", PASSWORD), headers);

        assertThat(mailbox.awaitMail(email, "doğrulayın").text()).contains("/verify-email?token=");
    }

    @Test
    void rejectsWeakPasswordsAndMissingFieldsWithStableCodes() {
        ResponseEntity<Map<String, Object>> weak =
                api.post("/api/v1/auth/sign-up", Map.of("email", uniqueEmail(), "password", "short"));
        assertThat(weak.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(weak.getBody()).containsEntry("code", "identity.weak_password");

        ResponseEntity<Map<String, Object>> missing = api.post("/api/v1/auth/sign-up", Map.of("password", PASSWORD));
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(missing.getBody()).containsEntry("code", "validation.failed");
        assertThat(ApiClient.errorFields(missing)).containsExactly("email");
    }

    @Test
    void rejectsUnknownVerificationTokens() {
        ResponseEntity<Map<String, Object>> response =
                api.post("/api/v1/auth/email-verification", Map.of("token", "not-a-real-token"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("code", "identity.invalid_token");
    }

    private ResponseEntity<Map<String, Object>> signUp(String email) {
        return api.post("/api/v1/auth/sign-up", Map.of("email", email, "password", PASSWORD));
    }

    static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
