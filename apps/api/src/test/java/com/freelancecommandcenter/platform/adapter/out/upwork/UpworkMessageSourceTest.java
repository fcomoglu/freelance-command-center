package com.freelancecommandcenter.platform.adapter.out.upwork;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.freelancecommandcenter.platform.api.PlatformMessage;
import com.freelancecommandcenter.platform.api.PlatformMessage.Detail;
import com.freelancecommandcenter.platform.api.PlatformMessage.Direction;
import com.freelancecommandcenter.platform.application.port.out.ConnectionRejectedException;
import com.freelancecommandcenter.platform.application.port.out.PlatformUnavailableException;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class UpworkMessageSourceTest {

    private static final String API = "https://api.upwork.com/graphql";
    private static final Instant SINCE = Instant.parse("2026-11-20T08:00:00Z");

    private final RestClient.Builder builder = RestClient.builder().baseUrl(API);
    private final MockRestServiceServer server =
            MockRestServiceServer.bindTo(builder).build();

    @Test
    void readsTheMessagesOfActiveRoomsSinceTheReadPosition() {
        server.expect(requestTo(API))
                .andExpect(header("Authorization", "Bearer user-token"))
                .andExpect(jsonPath("$.variables.filter.activeSince_eq").value("2026-11-20T08:00:00Z"))
                .andExpect(jsonPath("$.variables.filter.subscribed_eq").value(true))
                .andExpect(jsonPath("$.variables.stories.storyCreatedDateTime_bt.rangeStart")
                        .value("2026-11-20T08:00:00Z"))
                .andExpect(jsonPath("$.variables.pagination.first").value(20))
                .andRespond(withSuccess(fixture("upwork/room-messages.json"), MediaType.APPLICATION_JSON));

        List<PlatformMessage> messages = source(3).fetchSince("1001", "user-token", SINCE);

        server.verify();
        assertThat(messages).extracting(PlatformMessage::externalId).containsExactly("story-1", "story-2");
        PlatformMessage received = messages.getFirst();
        assertThat(received.platform()).isEqualTo(PlatformCode.UPWORK);
        assertThat(received.conversationId()).isEqualTo("room_made_up_1");
        assertThat(received.counterpart()).isEqualTo("Grace H.");
        assertThat(received.direction()).isEqualTo(Direction.INBOUND);
        assertThat(received.detail()).isEqualTo(Detail.FULL);
        assertThat(received.body()).isEqualTo("Could you start on Monday?");
        assertThat(received.url()).isEqualTo(URI.create("https://www.upwork.com/ab/messages/rooms/room_made_up_1"));
        assertThat(received.sentAt()).isEqualTo(Instant.parse("2026-11-20T09:00:00Z"));
        PlatformMessage sent = messages.get(1);
        assertThat(sent.direction()).isEqualTo(Direction.OUTBOUND);
        assertThat(sent.sentAt()).isEqualTo(Instant.ofEpochMilli(1795165500000L));
    }

    @Test
    void followsRoomPagesUpToTheLimit() {
        String morePages =
                fixture("upwork/room-messages.json").replace("\"hasNextPage\": false", "\"hasNextPage\": true");
        server.expect(jsonPath("$.variables.pagination.after").doesNotExist())
                .andRespond(withSuccess(morePages, MediaType.APPLICATION_JSON));
        server.expect(jsonPath("$.variables.pagination.after").value("20"))
                .andRespond(withSuccess(morePages, MediaType.APPLICATION_JSON));

        assertThat(source(2).fetchSince("1001", "user-token", SINCE)).hasSize(4);
        server.verify();
    }

    @Test
    void aRejectedTokenAsksForANewConnection() {
        server.expect(requestTo(API)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> source(3).fetchSince("1001", "expired", SINCE))
                .isInstanceOf(ConnectionRejectedException.class);
    }

    @Test
    void graphQlErrorsMakeThePlatformUnavailable() {
        server.expect(requestTo(API))
                .andRespond(
                        withSuccess("{\"errors\":[{\"message\":\"Permission denied\"}]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> source(3).fetchSince("1001", "user-token", SINCE))
                .isInstanceOf(PlatformUnavailableException.class)
                .isNotInstanceOf(ConnectionRejectedException.class)
                .hasMessageContaining("Permission denied");
    }

    private UpworkMessageSource source(int maxPages) {
        return new UpworkMessageSource(
                new UpworkAccountApi(builder.build(), JsonMapper.builder().build()),
                URI.create("https://www.upwork.com"),
                UpworkConfiguration.query(UpworkConfiguration.ROOM_MESSAGES_QUERY),
                20,
                maxPages,
                100);
    }

    private static String fixture(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
