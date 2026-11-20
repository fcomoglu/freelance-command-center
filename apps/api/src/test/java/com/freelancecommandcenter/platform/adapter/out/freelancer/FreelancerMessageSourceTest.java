package com.freelancecommandcenter.platform.adapter.out.freelancer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
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

class FreelancerMessageSourceTest {

    private static final String SITE = "https://www.freelancer.com";
    private static final Instant SINCE = Instant.ofEpochSecond(1795599000);

    private final RestClient.Builder builder = RestClient.builder().baseUrl(SITE);
    private final MockRestServiceServer server =
            MockRestServiceServer.bindTo(builder).build();

    @Test
    void readsTheAccountsMessagesWithTheirConversationAndOtherParty() {
        server.expect(requestTo(startsWith(SITE + FreelancerMessageSource.MESSAGES_PATH)))
                .andExpect(header(FreelancerConfiguration.TOKEN_HEADER, "user-token"))
                .andExpect(queryParam("from_updated_time", "1795599000"))
                .andExpect(queryParam("thread_details", "true"))
                .andExpect(queryParam("user_details", "true"))
                .andExpect(queryParam("offset", "0"))
                .andRespond(withSuccess(fixture("freelancer/messages.json"), MediaType.APPLICATION_JSON));

        List<PlatformMessage> messages = source(100, 5).fetchSince("1001", "user-token", SINCE);

        server.verify();
        assertThat(messages).hasSize(2);
        PlatformMessage received = messages.getFirst();
        assertThat(received.platform()).isEqualTo(PlatformCode.FREELANCER);
        assertThat(received.conversationId()).isEqualTo("501");
        assertThat(received.externalId()).isEqualTo("9001");
        assertThat(received.counterpart()).isEqualTo("Grace H.");
        assertThat(received.direction()).isEqualTo(Direction.INBOUND);
        assertThat(received.detail()).isEqualTo(Detail.FULL);
        assertThat(received.body()).isEqualTo("Hi, can you start on Monday?");
        assertThat(received.url()).isEqualTo(URI.create(SITE + "/messages/thread/501"));
        assertThat(received.sentAt()).isEqualTo(Instant.ofEpochSecond(1795600000));
        PlatformMessage sent = messages.get(1);
        assertThat(sent.direction()).isEqualTo(Direction.OUTBOUND);
        assertThat(sent.counterpart()).isEqualTo("Grace H.");
    }

    @Test
    void readsFurtherPagesUpToTheLimit() {
        server.expect(queryParam("offset", "0"))
                .andRespond(withSuccess(fixture("freelancer/messages.json"), MediaType.APPLICATION_JSON));
        server.expect(queryParam("offset", "3"))
                .andRespond(withSuccess(fixture("freelancer/messages.json"), MediaType.APPLICATION_JSON));

        assertThat(source(3, 2).fetchSince("1001", "user-token", SINCE)).hasSize(4);
        server.verify();
    }

    @Test
    void aRejectedTokenAsksForANewConnection() {
        server.expect(requestTo(startsWith(SITE))).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> source(100, 5).fetchSince("1001", "expired", SINCE))
                .isInstanceOf(ConnectionRejectedException.class);
    }

    @Test
    void otherFailuresMakeThePlatformUnavailable() {
        server.expect(requestTo(startsWith(SITE))).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> source(100, 5).fetchSince("1001", "user-token", SINCE))
                .isInstanceOf(PlatformUnavailableException.class)
                .isNotInstanceOf(ConnectionRejectedException.class);
    }

    private FreelancerMessageSource source(int pageSize, int maxPages) {
        return new FreelancerMessageSource(
                builder.build(), URI.create(SITE), JsonMapper.builder().build(), pageSize, maxPages);
    }

    private static String fixture(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
