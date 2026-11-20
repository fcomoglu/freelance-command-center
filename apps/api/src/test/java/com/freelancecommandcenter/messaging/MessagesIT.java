package com.freelancecommandcenter.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import com.freelancecommandcenter.ApiClient;
import com.freelancecommandcenter.FakeMailParser;
import com.freelancecommandcenter.FakePushSender;
import com.freelancecommandcenter.IntegrationTest;
import com.freelancecommandcenter.notification.application.port.out.PushSender.PushMessage;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** From a forwarded message notice through the inbox to a push message, plus the conversation endpoints. */
@IntegrationTest
class MessagesIT {

    @Autowired
    private ApiClient api;

    @Autowired
    private FakePushSender push;

    @Test
    void messageNoticesFormAConversationThatIsPushedAndReadInTheWorkspaceOnly() {
        HttpHeaders user = api.newSignedInUser();
        HttpHeaders other = api.newSignedInUser();
        String phone = "ExponentPushToken[" + UUID.randomUUID() + "]";
        api.post("/api/v1/devices", Map.of("pushToken", phone, "language", "tr"), user);
        String address = (String) api.get("/api/v1/mail-inbox", user).getBody().get("address");
        String first = notice("Fri, 20 Nov 2026 12:00:00 +0300", "Hi, are you available?");

        deliver(address, first);
        deliver(address, notice("Fri, 20 Nov 2026 12:05:00 +0300", "We would like to\ninterview you."));
        deliver(address, first);

        await().atMost(Duration.ofSeconds(10)).until(() -> push.sentTo(phone).size() == 2);
        PushMessage pushed = push.sentTo(phone).stream()
                .filter(message -> message.body().contains("interview"))
                .findFirst()
                .orElseThrow();
        assertThat(pushed.title()).isEqualTo("Yeni mesaj · Arc.dev");
        assertThat(pushed.body()).isEqualTo("Ada Lovelace: We would like to interview you.");
        assertThat(pushed.channel()).isEqualTo("messages");
        assertThat(pushed.data())
                .containsEntry("type", "message")
                .containsEntry("url", "https://arc.dev/messages/ada-lovelace")
                .containsKey("conversationId");

        Map<String, Object> conversation =
                items(api.get("/api/v1/conversations", user)).getFirst();
        assertThat(conversation)
                .containsEntry("platform", "arc_dev")
                .containsEntry("counterpart", "Ada Lovelace")
                .containsEntry("url", "https://arc.dev/messages/ada-lovelace")
                .containsEntry("detail", "notice")
                .containsEntry("lastMessageAt", "2026-11-20T09:05:00Z")
                .containsEntry("preview", "We would like to interview you.")
                .containsEntry("unreadCount", 2);
        String messagesPath = "/api/v1/conversations/" + conversation.get("id") + "/messages";
        assertThat(items(api.get(messagesPath, user)))
                .extracting(message -> message.get("body"), message -> message.get("direction"))
                .containsExactly(
                        tuple("We would like to\ninterview you.", "inbound"),
                        tuple("Hi, are you available?", "inbound"));

        String readPath = "/api/v1/conversations/" + conversation.get("id") + "/read";
        assertThat(items(api.get("/api/v1/conversations", other))).isEmpty();
        ResponseEntity<Map<String, Object>> foreign = api.get(messagesPath, other);
        assertThat(foreign.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(foreign.getBody()).containsEntry("code", "messaging.conversation_not_found");
        assertThat(api.post(readPath, Map.of(), other).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(api.post(readPath, Map.of(), user).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(items(api.get("/api/v1/conversations", user)).getFirst()).containsEntry("unreadCount", 0);
    }

    @Test
    void theInboxRequiresASignedInUser() {
        assertThat(api.get("/api/v1/conversations", new HttpHeaders()).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private void deliver(String recipient, String mail) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("test-inbound-secret");
        headers.set("X-Envelope-To", recipient);
        api.postRaw(
                "/api/v1/inbound-mail",
                mail.getBytes(StandardCharsets.UTF_8),
                MediaType.parseMediaType("message/rfc822"),
                headers);
    }

    /** A made-up Arc.dev message notice that {@link FakeMailParser} reads; the Message-ID follows from the text. */
    private static String notice(String date, String text) {
        return "From: Arc <messages@arc.dev>\r\n"
                + "To: someone@hotmail.com\r\n"
                + "Subject: " + FakeMailParser.MESSAGE_PREFIX + "Ada Lovelace\r\n"
                + "Date: " + date + "\r\n"
                + "Message-ID: <" + UUID.nameUUIDFromBytes(text.getBytes(StandardCharsets.UTF_8)) + "@arc.dev>\r\n"
                + "MIME-Version: 1.0\r\n"
                + "Content-Type: text/plain; charset=UTF-8\r\n"
                + "\r\n"
                + text + "\r\n";
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> items(ResponseEntity<Map<String, Object>> response) {
        return (List<Map<String, Object>>) response.getBody().get("items");
    }
}
