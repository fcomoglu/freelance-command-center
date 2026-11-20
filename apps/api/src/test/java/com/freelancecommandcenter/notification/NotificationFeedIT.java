package com.freelancecommandcenter.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.freelancecommandcenter.ApiClient;
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

/** From a forwarded platform mail that no parser understands, through the feed to a push message and read state. */
@IntegrationTest
class NotificationFeedIT {

    private static final String FEED = "/api/v1/notifications";

    @Autowired
    private ApiClient api;

    @Autowired
    private FakePushSender push;

    @Test
    void aPlatformMailNoParserUnderstandsBecomesANoticeOfTheWorkspaceOnly() {
        HttpHeaders user = api.newSignedInUser();
        HttpHeaders other = api.newSignedInUser();
        String phone = "ExponentPushToken[" + UUID.randomUUID() + "]";
        api.post("/api/v1/devices", Map.of("pushToken", phone, "language", "tr"), user);
        String address = (String) api.get("/api/v1/mail-inbox", user).getBody().get("address");
        String order = fiverrMail(
                "Fri, 20 Nov 2026 12:00:00 +0300",
                "You received a new order",
                "<p>Grace&nbsp;ordered " + "<b>Logo design</b>.</p>");

        deliver(address, order);
        deliver(address, fiverrMail("Fri, 20 Nov 2026 12:30:00 +0300", "Your gig is live", "<p>Congratulations</p>"));
        deliver(address, order);

        await().atMost(Duration.ofSeconds(10)).until(() -> push.sentTo(phone).size() == 2);
        PushMessage pushed = push.sentTo(phone).stream()
                .filter(message -> message.body().equals("You received a new order"))
                .findFirst()
                .orElseThrow();
        assertThat(pushed.title()).isEqualTo("Bildirim · Fiverr");
        assertThat(pushed.channel()).isEqualTo("notifications");
        assertThat(pushed.data())
                .containsEntry("type", "notification")
                .containsEntry("url", "https://www.fiverr.com")
                .containsKey("notificationId");

        // The push goes out before the feed entry commits.
        await().atMost(Duration.ofSeconds(10))
                .until(() -> items(api.get(FEED, user)).size() == 2);
        List<Map<String, Object>> feed = items(api.get(FEED, user));
        assertThat(feed)
                .extracting(item -> item.get("title"))
                .containsExactly("Your gig is live", "You received a new order");
        assertThat(feed.get(1))
                .containsEntry("platform", "fiverr")
                .containsEntry("kind", "notice")
                .containsEntry("body", "Grace ordered Logo design.")
                .containsEntry("url", "https://www.fiverr.com")
                .containsEntry("occurredAt", "2026-11-20T09:00:00Z")
                .containsEntry("read", false);
        assertThat(items(api.get(FEED, other))).isEmpty();

        String readPath = FEED + "/" + feed.get(1).get("id") + "/read";
        ResponseEntity<Map<String, Object>> foreign = api.post(readPath, Map.of(), other);
        assertThat(foreign.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(foreign.getBody()).containsEntry("code", "notification.not_found");

        assertThat(api.post(readPath, Map.of(), user).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(items(api.get(FEED, user)))
                .extracting(item -> item.get("read"))
                .containsExactly(false, true);

        assertThat(api.post(FEED + "/read", Map.of(), user).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(items(api.get(FEED, user)))
                .extracting(item -> item.get("read"))
                .containsExactly(true, true);
    }

    @Test
    void theFeedIsPagedNewestFirst() {
        HttpHeaders user = api.newSignedInUser();
        String address = (String) api.get("/api/v1/mail-inbox", user).getBody().get("address");
        for (int minute = 10; minute < 13; minute++) {
            deliver(address, fiverrMail("Fri, 20 Nov 2026 12:" + minute + ":00 +0300", "Notice " + minute, "<p>x</p>"));
        }
        await().atMost(Duration.ofSeconds(10))
                .until(() -> items(api.get(FEED, user)).size() == 3);

        ResponseEntity<Map<String, Object>> first = api.get(FEED + "?limit=2", user);
        ResponseEntity<Map<String, Object>> second =
                api.get(FEED + "?limit=2&cursor=" + first.getBody().get("nextCursor"), user);

        assertThat(items(first)).extracting(item -> item.get("title")).containsExactly("Notice 12", "Notice 11");
        assertThat(items(second)).extracting(item -> item.get("title")).containsExactly("Notice 10");
        assertThat(second.getBody().get("nextCursor")).isNull();
    }

    @Test
    void theFeedRequiresASignedInUser() {
        assertThat(api.get(FEED, new HttpHeaders()).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
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

    /** An HTML-only Fiverr mail; no parser reads Fiverr mails, and the Message-ID follows from the subject. */
    private static String fiverrMail(String date, String subject, String html) {
        return "From: Fiverr <noreply@e.fiverr.com>\r\n"
                + "To: someone@hotmail.com\r\n"
                + "Subject: " + subject + "\r\n"
                + "Date: " + date + "\r\n"
                + "Message-ID: <" + UUID.nameUUIDFromBytes((date + subject).getBytes(StandardCharsets.UTF_8))
                + "@fiverr.com>\r\n"
                + "MIME-Version: 1.0\r\n"
                + "Content-Type: text/html; charset=UTF-8\r\n"
                + "\r\n"
                + "<html><body>" + html + "</body></html>\r\n";
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> items(ResponseEntity<Map<String, Object>> response) {
        return (List<Map<String, Object>>) response.getBody().get("items");
    }
}
