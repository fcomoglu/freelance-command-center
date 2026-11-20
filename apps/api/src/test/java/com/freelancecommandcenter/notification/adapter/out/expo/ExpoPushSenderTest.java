package com.freelancecommandcenter.notification.adapter.out.expo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.freelancecommandcenter.notification.application.port.out.PushSender.PushMessage;
import com.freelancecommandcenter.notification.application.port.out.PushSender.PushUnavailableException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class ExpoPushSenderTest {

    private static final String SEND_URL = "https://exp.host/--/api/v2/push/send";

    private final RestClient.Builder builder = RestClient.builder().baseUrl(SEND_URL);
    private final MockRestServiceServer server =
            MockRestServiceServer.bindTo(builder).build();
    private final ExpoPushSender sender =
            new ExpoPushSender(builder.build(), JsonMapper.builder().build());

    @Test
    void sendsMessagesInChunksOfAHundredAndReportsUnregisteredDevices() {
        List<PushMessage> messages =
                IntStream.range(0, 150).mapToObj(ExpoPushSenderTest::message).toList();
        server.expect(requestTo(SEND_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.length()").value(100))
                .andExpect(jsonPath("$[0].to").value("ExponentPushToken[0]"))
                .andExpect(jsonPath("$[0].title").value("New job · Upwork"))
                .andExpect(jsonPath("$[0].body").value("Job 0"))
                .andExpect(jsonPath("$[0].channelId").value("jobs"))
                .andExpect(jsonPath("$[0].data.url").value("https://www.upwork.com/jobs/~0"))
                .andRespond(withSuccess(tickets(100, 7), MediaType.APPLICATION_JSON));
        server.expect(requestTo(SEND_URL))
                .andExpect(jsonPath("$.length()").value(50))
                .andExpect(jsonPath("$[0].to").value("ExponentPushToken[100]"))
                .andRespond(withSuccess(tickets(50, 49), MediaType.APPLICATION_JSON));

        List<String> unregistered = sender.send(messages);

        server.verify();
        assertThat(unregistered).containsExactly("ExponentPushToken[7]", "ExponentPushToken[149]");
    }

    @Test
    void aRejectedRequestMeansThePushServiceIsUnavailable() {
        server.expect(requestTo(SEND_URL))
                .andRespond(withSuccess(
                        "{\"errors\":[{\"code\":\"PUSH_TOO_MANY_EXPERIENCE_IDS\",\"message\":\"...\"}]}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> sender.send(List.of(message(0))))
                .isInstanceOf(PushUnavailableException.class)
                .hasMessageContaining("PUSH_TOO_MANY_EXPERIENCE_IDS");
    }

    @Test
    void anErrorStatusMeansThePushServiceIsUnavailable() {
        server.expect(requestTo(SEND_URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> sender.send(List.of(message(0))))
                .isInstanceOf(PushUnavailableException.class)
                .hasMessageContaining("429");
    }

    private static PushMessage message(int i) {
        return new PushMessage(
                "ExponentPushToken[" + i + "]",
                "New job · Upwork",
                "Job " + i,
                "jobs",
                Map.of("type", "job", "url", "https://www.upwork.com/jobs/~" + i));
    }

    /** Ok tickets, except a DeviceNotRegistered error at {@code unregisteredIndex}. */
    private static String tickets(int count, int unregisteredIndex) {
        return IntStream.range(0, count)
                .mapToObj(i -> i == unregisteredIndex
                        ? "{\"status\":\"error\",\"message\":\"not registered\",\"details\":{\"error\":\"DeviceNotRegistered\"}}"
                        : "{\"status\":\"ok\",\"id\":\"receipt-" + i + "\"}")
                .collect(Collectors.joining(",", "{\"data\":[", "]}"));
    }
}
