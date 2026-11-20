package com.freelancecommandcenter.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import com.freelancecommandcenter.ApiClient;
import com.freelancecommandcenter.FakeAccountConnector;
import com.freelancecommandcenter.FakeMessageSource;
import com.freelancecommandcenter.FakeProposalStatusTracker;
import com.freelancecommandcenter.IntegrationTest;
import com.freelancecommandcenter.Mailbox;
import com.freelancecommandcenter.platform.api.PlatformMessage;
import com.freelancecommandcenter.platform.api.PlatformMessage.Detail;
import com.freelancecommandcenter.platform.api.PlatformMessage.Direction;
import com.freelancecommandcenter.platform.application.ConnectionSyncUseCase;
import com.freelancecommandcenter.platform.domain.ProposalSnapshot;
import com.freelancecommandcenter.platform.domain.ProposalStatus;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Connecting a Freelancer.com account through OAuth, reading its messages into the inbox and its bid decisions into the
 * notification feed, and disconnecting it.
 */
@IntegrationTest
class PlatformConnectionsIT {

    private static final String CONNECTIONS = "/api/v1/platform-connections";

    @Autowired
    private ApiClient api;

    @Autowired
    private Mailbox mailbox;

    @Autowired
    private FakeMessageSource messages;

    @Autowired
    private FakeProposalStatusTracker proposals;

    @Autowired
    private ConnectionSyncUseCase connectionSync;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void aConnectedAccountsMessagesReachTheWorkspaceInboxWithFullDetail() {
        HttpHeaders user = api.newVerifiedUser(mailbox);
        String code = UUID.randomUUID().toString();
        List<Map<String, Object>> before = items(api.get(CONNECTIONS, user));
        assertThat(before).hasSize(1);
        assertThat(before.getFirst()).containsEntry("platform", "freelancer").containsEntry("status", "not_connected");

        ResponseEntity<Map<String, Object>> connected =
                api.post(CONNECTIONS + "/freelancer", Map.of("code", code, "state", startAuthorization(user)), user);

        assertThat(connected.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(connected.getBody())
                .containsEntry("platform", "freelancer")
                .containsEntry("status", "active")
                .containsEntry("accountName", "Ada L.")
                .containsKey("connectedAt");
        assertThat(items(api.get(CONNECTIONS, user)).getFirst()).containsEntry("status", "active");
        byte[] storedToken = jdbc.sql("SELECT access_token FROM platform.connections WHERE account_name = 'Ada L.' "
                        + "ORDER BY connected_at DESC LIMIT 1")
                .query(byte[].class)
                .single();
        assertThat(new String(storedToken, StandardCharsets.ISO_8859_1)).doesNotContain("access-" + code);

        String thread = UUID.randomUUID().toString();
        messages.offer(
                "access-" + code,
                message(thread, "9001", Direction.INBOUND, "Can you start on Monday?", "2026-11-20T09:00:00Z"),
                message(thread, "9002", Direction.OUTBOUND, "Yes, I can.", "2026-11-20T09:05:00Z"));
        connectionSync.syncAll();

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(items(api.get("/api/v1/conversations", user)))
                        .hasSize(1));
        Map<String, Object> conversation =
                items(api.get("/api/v1/conversations", user)).getFirst();
        assertThat(conversation)
                .containsEntry("platform", "freelancer")
                .containsEntry("counterpart", "Grace H.")
                .containsEntry("detail", "full")
                .containsEntry("url", "https://www.freelancer.com/messages/thread/" + thread)
                .containsEntry("unreadCount", 1);
        assertThat(items(api.get("/api/v1/conversations/" + conversation.get("id") + "/messages", user)))
                .extracting(message -> message.get("body"), message -> message.get("direction"))
                .containsExactly(tuple("Yes, I can.", "outbound"), tuple("Can you start on Monday?", "inbound"));

        assertThat(api.delete(CONNECTIONS + "/freelancer", user).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(items(api.get(CONNECTIONS, user)).getFirst()).containsEntry("status", "not_connected");
        assertThat(items(api.get("/api/v1/conversations", user))).hasSize(1);
        ResponseEntity<Map<String, Object>> again = api.delete(CONNECTIONS + "/freelancer", user);
        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(again.getBody()).containsEntry("code", "platform.connection_not_found");
    }

    @Test
    void decisionsOnTheAccountsBidsAfterConnectingReachTheNotificationFeed() {
        HttpHeaders user = api.newVerifiedUser(mailbox);
        String code = UUID.randomUUID().toString();
        api.post(CONNECTIONS + "/freelancer", Map.of("code", code, "state", startAuthorization(user)), user);
        proposals.set("access-" + code, bid("7001", ProposalStatus.PENDING), bid("7002", ProposalStatus.AWARDED));
        connectionSync.syncAll();

        proposals.set(
                "access-" + code,
                bid("7001", ProposalStatus.SHORTLISTED),
                bid("7002", ProposalStatus.AWARDED),
                bid("7003", ProposalStatus.REJECTED));
        connectionSync.syncAll();
        connectionSync.syncAll();

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(items(api.get("/api/v1/notifications", user)))
                        .extracting(item -> item.get("kind"), item -> item.get("title"))
                        .containsExactlyInAnyOrder(
                                tuple("proposal_shortlisted", "Job 7001"), tuple("proposal_rejected", "Job 7003")));
        assertThat(items(api.get("/api/v1/notifications", user)))
                .allSatisfy(item -> assertThat(item)
                        .containsEntry("platform", "freelancer")
                        .containsEntry("body", "")
                        .containsEntry("read", false));

        String again = UUID.randomUUID().toString();
        api.post(CONNECTIONS + "/freelancer", Map.of("code", again, "state", startAuthorization(user)), user);
        proposals.set("access-" + again, bid("7001", ProposalStatus.AWARDED));
        connectionSync.syncAll();

        assertThat(jdbc.sql("SELECT count(*) FROM platform.proposal_statuses s JOIN platform.connections c "
                                + "ON c.id = s.connection_id WHERE c.account_name = 'Ada L.' AND s.status = 'AWARDED' "
                                + "AND s.proposal_id = '7001'")
                        .query(Long.class)
                        .single())
                .isPositive();
        assertThat(items(api.get("/api/v1/notifications", user))).hasSize(2);
    }

    @Test
    void anAuthorizationCompletesOnceAndOnlyInTheWorkspaceThatStartedIt() {
        HttpHeaders user = api.newVerifiedUser(mailbox);
        HttpHeaders other = api.newVerifiedUser(mailbox);
        String state = startAuthorization(user);

        ResponseEntity<Map<String, Object>> foreign =
                api.post(CONNECTIONS + "/freelancer", Map.of("code", "code", "state", state), other);
        ResponseEntity<Map<String, Object>> reused =
                api.post(CONNECTIONS + "/freelancer", Map.of("code", "code", "state", state), user);

        assertThat(foreign.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(foreign.getBody()).containsEntry("code", "platform.connection_expired");
        assertThat(reused.getBody()).containsEntry("code", "platform.connection_expired");
        assertThat(items(api.get(CONNECTIONS, other)).getFirst()).containsEntry("status", "not_connected");
    }

    @Test
    void aRefusedAuthorizationConnectsNothing() {
        HttpHeaders user = api.newVerifiedUser(mailbox);

        ResponseEntity<Map<String, Object>> refused = api.post(
                CONNECTIONS + "/freelancer",
                Map.of("code", FakeAccountConnector.REFUSED_CODE, "state", startAuthorization(user)),
                user);

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(refused.getBody()).containsEntry("code", "platform.connection_rejected");
        assertThat(items(api.get(CONNECTIONS, user)).getFirst()).containsEntry("status", "not_connected");
    }

    @Test
    void connectingRequiresAConfirmedEmailAddress() {
        ResponseEntity<Map<String, Object>> response =
                api.post(CONNECTIONS + "/freelancer/authorizations", Map.of(), api.newSignedInUser());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("code", "platform.email_not_verified");
    }

    @Test
    void platformsWithoutAnOAuthAppCannotBeConnected() {
        HttpHeaders user = api.newVerifiedUser(mailbox);

        ResponseEntity<Map<String, Object>> response = api.post(CONNECTIONS + "/upwork/authorizations", Map.of(), user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("code", "platform.connection_unavailable");
    }

    @Test
    void connectionsRequireASignedInUser() {
        assertThat(api.get(CONNECTIONS, new HttpHeaders()).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private String startAuthorization(HttpHeaders user) {
        ResponseEntity<Map<String, Object>> started =
                api.post(CONNECTIONS + "/freelancer/authorizations", Map.of(), user);
        assertThat(started.getStatusCode()).isEqualTo(HttpStatus.OK);
        String url = (String) started.getBody().get("authorizationUrl");
        assertThat(url).startsWith(FakeAccountConnector.CONSENT_PAGE + "?state=");
        return url.substring((FakeAccountConnector.CONSENT_PAGE + "?state=").length());
    }

    private static PlatformMessage message(String thread, String id, Direction direction, String body, String sentAt) {
        return new PlatformMessage(
                PlatformCode.FREELANCER,
                thread,
                id,
                "Grace H.",
                direction,
                Detail.FULL,
                body,
                URI.create("https://www.freelancer.com/messages/thread/" + thread),
                Instant.parse(sentAt));
    }

    private static ProposalSnapshot bid(String id, ProposalStatus status) {
        return new ProposalSnapshot(
                id, "Job " + id, URI.create("https://www.freelancer.com/projects/" + id), status, Optional.empty());
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> items(ResponseEntity<Map<String, Object>> response) {
        return (List<Map<String, Object>>) response.getBody().get("items");
    }
}
