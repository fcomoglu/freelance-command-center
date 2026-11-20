package com.freelancecommandcenter.platform;

import static org.assertj.core.api.Assertions.assertThat;

import com.freelancecommandcenter.ApiClient;
import com.freelancecommandcenter.IntegrationTest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Forwarded platform e-mails: the inbox address, the webhook of the mail routing service and the mail list. */
@IntegrationTest
class InboundMailIT {

    private static final MediaType RFC822 = MediaType.parseMediaType("message/rfc822");

    @Autowired
    private ApiClient api;

    @Test
    void mailsForwardedToTheInboxAreListedNewestFirstOnceEach() {
        HttpHeaders user = api.newSignedInUser();
        String address = address(user);
        assertThat(address).matches("[a-z2-7]{20}@inbound\\.localhost");

        String bionluk = mail("Bionluk <noreply@bionluk.com>", "Yeni alıcı isteği", "<a-" + UUID.randomUUID() + ">");
        assertThat(deliver(address, bionluk).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(deliver(address, bionluk).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        deliver(
                address.replace("@", "+tag@"),
                mail("Friend <friend@example.com>", "Hello", "<b-" + UUID.randomUUID() + ">"));

        List<Map<String, Object>> mails = items(api.get("/api/v1/inbound-mails", user));

        assertThat(mails).hasSize(2);
        assertThat(mails.get(0))
                .containsEntry("sender", "friend@example.com")
                .containsEntry("status", "unknown_sender")
                .containsEntry("platform", null);
        assertThat(mails.get(1))
                .containsEntry("platform", "bionluk")
                .containsEntry("sender", "noreply@bionluk.com")
                .containsEntry("subject", "Yeni alıcı isteği")
                .containsEntry("status", "unrecognized");
    }

    @Test
    void aRenewedAddressReplacesTheOldOne() {
        HttpHeaders user = api.newSignedInUser();
        String old = address(user);

        String renewed = (String)
                api.post("/api/v1/mail-inbox/renewal", Map.of(), user).getBody().get("address");

        assertThat(renewed).isNotEqualTo(old);
        assertThat(address(user)).isEqualTo(renewed);
        assertThat(deliver(old, mail("noreply@bionluk.com", "Lost", "<c-" + UUID.randomUUID() + ">"))
                        .getStatusCode())
                .isEqualTo(HttpStatus.ACCEPTED);
        assertThat(items(api.get("/api/v1/inbound-mails", user))).isEmpty();
    }

    @Test
    void theWebhookRequiresTheRoutingServicesSecretAndAMimeMessage() {
        HttpHeaders user = api.newSignedInUser();
        String address = address(user);
        HttpHeaders wrongSecret = new HttpHeaders();
        wrongSecret.setBearerAuth("guess");
        wrongSecret.set("X-Envelope-To", address);

        assertThat(api.postRaw("/api/v1/inbound-mail", bytes(mail("a@bionluk.com", "x", "<d>")), RFC822, wrongSecret)
                        .getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        HttpHeaders userToken = new HttpHeaders();
        userToken.addAll(user);
        userToken.set("X-Envelope-To", address);
        assertThat(api.postRaw("/api/v1/inbound-mail", bytes(mail("a@bionluk.com", "x", "<e>")), RFC822, userToken)
                        .getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(items(api.get("/api/v1/inbound-mails", user))).isEmpty();
    }

    private String address(HttpHeaders user) {
        return (String) api.get("/api/v1/mail-inbox", user).getBody().get("address");
    }

    private ResponseEntity<Map<String, Object>> deliver(String recipient, String mail) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("test-inbound-secret");
        headers.set("X-Envelope-To", recipient);
        return api.postRaw("/api/v1/inbound-mail", bytes(mail), RFC822, headers);
    }

    private static String mail(String from, String subject, String messageId) {
        String encodedSubject =
                "=?UTF-8?B?" + Base64.getEncoder().encodeToString(subject.getBytes(StandardCharsets.UTF_8)) + "?=";
        return "From: " + from + "\r\n"
                + "To: someone@hotmail.com\r\n"
                + "Subject: " + encodedSubject + "\r\n"
                + "Date: Fri, 20 Nov 2026 12:00:00 +0300\r\n"
                + "Message-ID: " + messageId + "\r\n"
                + "MIME-Version: 1.0\r\n"
                + "Content-Type: text/plain; charset=UTF-8\r\n"
                + "\r\n"
                + "Body\r\n";
    }

    private static byte[] bytes(String mail) {
        return mail.getBytes(StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> items(ResponseEntity<Map<String, Object>> response) {
        return (List<Map<String, Object>>) response.getBody().get("items");
    }
}
