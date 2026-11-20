package com.freelancecommandcenter.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.freelancecommandcenter.ApiClient;
import com.freelancecommandcenter.FakeJobSource;
import com.freelancecommandcenter.FakePushSender;
import com.freelancecommandcenter.IntegrationTest;
import com.freelancecommandcenter.matching.api.JobMatched;
import com.freelancecommandcenter.notification.application.NotifyJobMatchUseCase;
import com.freelancecommandcenter.notification.application.port.out.PushSender.PushMessage;
import com.freelancecommandcenter.platform.api.JobListing;
import com.freelancecommandcenter.platform.application.JobSyncUseCase;
import com.freelancecommandcenter.shared.id.JobId;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** From a platform sync through matching to a push message, plus the job filter and device endpoints. */
@IntegrationTest
class JobNotificationIT {

    @Autowired
    private ApiClient api;

    @Autowired
    private FakeJobSource platform;

    @Autowired
    private JobSyncUseCase jobSync;

    @Autowired
    private FakePushSender push;

    @Autowired
    private NotifyJobMatchUseCase notifications;

    @Test
    void aNewJobMatchingTheFilterIsPushedToTheWorkspacesPhonesInTheirLanguage() {
        HttpHeaders user = api.newSignedInUser();
        // A keyword no other test uses, so jobs synced by other tests never match this workspace.
        String keyword = "kw" + UUID.randomUUID().toString().replace("-", "");
        api.put("/api/v1/job-filter", Map.of("keywords", List.of(keyword), "platforms", List.of("freelancer")), user);
        String english = pushToken();
        String turkish = pushToken();
        api.post("/api/v1/devices", Map.of("pushToken", english, "language", "en"), user);
        api.post("/api/v1/devices", Map.of("pushToken", turkish, "language", "tr-TR"), user);

        platform.offer(listing("Need a " + keyword.toUpperCase() + " expert"), listing("Unrelated logo design"));
        jobSync.syncAll();

        await().atMost(Duration.ofSeconds(10)).until(() -> push.sentTo(turkish).size() == 1);
        PushMessage message = push.sentTo(english).getFirst();
        assertThat(message.title()).isEqualTo("New job · Freelancer.com");
        assertThat(message.body()).isEqualTo("Need a " + keyword.toUpperCase() + " expert");
        assertThat(message.channel()).isEqualTo("jobs");
        assertThat(message.data())
                .containsEntry("type", "job")
                .containsEntry("url", "https://www.freelancer.com/projects/Need-a-" + keyword.toUpperCase() + "-expert")
                .containsKey("jobId");
        assertThat(push.sentTo(turkish).getFirst().title()).isEqualTo("Yeni ilan · Freelancer.com");
    }

    @Test
    void aRepeatedMatchIsPushedOnceAndUnregisteredPhonesAreForgotten() {
        HttpHeaders user = api.newSignedInUser();
        WorkspaceId workspace =
                WorkspaceId.of((String) api.get("/api/v1/me", user).getBody().get("workspaceId"));
        String gone = pushToken();
        String kept = pushToken();
        api.post("/api/v1/devices", Map.of("pushToken", gone, "language", "en"), user);
        api.post("/api/v1/devices", Map.of("pushToken", kept, "language", "en"), user);
        push.forget(gone);

        notifications.notify(match(workspace));
        JobMatched again = match(workspace);
        notifications.notify(again);
        notifications.notify(again);

        assertThat(push.sentTo(kept)).hasSize(2);
        assertThat(push.sentTo(gone)).hasSize(1);
    }

    @Test
    void theJobFilterStartsEmptyAndCanBeReplaced() {
        HttpHeaders user = api.newSignedInUser();

        assertThat(api.get("/api/v1/job-filter", user).getBody())
                .containsEntry("keywords", List.of())
                .containsEntry("platforms", List.of("freelancer", "upwork", "fiverr", "bionluk", "arc_dev"));

        ResponseEntity<Map<String, Object>> changed = api.put(
                "/api/v1/job-filter",
                Map.of("keywords", List.of(" Spring  Boot", "spring boot", "Laravel"), "platforms", List.of("upwork")),
                user);

        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(api.get("/api/v1/job-filter", user).getBody())
                .containsEntry("keywords", List.of("spring boot", "laravel"))
                .containsEntry("platforms", List.of("upwork"));
    }

    @Test
    void invalidFiltersAndPushTokensAreRejected() {
        HttpHeaders user = api.newSignedInUser();

        ResponseEntity<Map<String, Object>> longKeyword = api.put(
                "/api/v1/job-filter", Map.of("keywords", List.of("x".repeat(51)), "platforms", List.of()), user);
        assertThat(longKeyword.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ApiClient.errorFields(longKeyword)).containsExactly("keywords[0]");

        ResponseEntity<Map<String, Object>> badToken =
                api.post("/api/v1/devices", Map.of("pushToken", "not-a-token", "language", "en"), user);
        assertThat(badToken.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(badToken.getBody()).containsEntry("code", "notification.invalid_push_token");
    }

    @Test
    void aPhoneKeepsItsIdAcrossRegistrationsAndCanBeUnregisteredByItsUserOnly() {
        HttpHeaders owner = api.newSignedInUser();
        HttpHeaders other = api.newSignedInUser();
        String token = pushToken();

        Object id = api.post("/api/v1/devices", Map.of("pushToken", token, "language", "en"), owner)
                .getBody()
                .get("id");
        assertThat(api.post("/api/v1/devices", Map.of("pushToken", token, "language", "tr"), owner)
                        .getBody())
                .containsEntry("id", id);

        assertThat(api.delete("/api/v1/devices/" + id, other).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        WorkspaceId workspace =
                WorkspaceId.of((String) api.get("/api/v1/me", owner).getBody().get("workspaceId"));
        notifications.notify(match(workspace));
        assertThat(push.sentTo(token)).hasSize(1);

        api.delete("/api/v1/devices/" + id, owner);
        notifications.notify(match(workspace));
        assertThat(push.sentTo(token)).hasSize(1);
    }

    private static String pushToken() {
        return "ExponentPushToken[" + UUID.randomUUID() + "]";
    }

    private static JobMatched match(WorkspaceId workspace) {
        return new JobMatched(
                workspace,
                JobId.newId(),
                PlatformCode.UPWORK,
                "Kotlin backend",
                URI.create("https://www.upwork.com/jobs/~01"),
                Instant.parse("2026-11-20T12:00:00Z"));
    }

    private static JobListing listing(String title) {
        Instant now = Instant.now();
        return new JobListing(
                PlatformCode.FREELANCER,
                UUID.randomUUID().toString(),
                title,
                "",
                URI.create("https://www.freelancer.com/projects/" + title.replace(' ', '-')),
                null,
                List.of(),
                null,
                null,
                now,
                now);
    }
}
