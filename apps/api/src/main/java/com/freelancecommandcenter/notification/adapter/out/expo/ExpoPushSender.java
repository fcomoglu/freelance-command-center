package com.freelancecommandcenter.notification.adapter.out.expo;

import com.freelancecommandcenter.notification.application.port.out.PushSender;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Sends through the Expo Push Service, which forwards to FCM (Android) and APNs (iOS). It reads the push tickets of
 * the send request; delivery receipts are not read.
 */
class ExpoPushSender implements PushSender {

    /** The service accepts at most 100 messages per request. */
    static final int MAX_MESSAGES_PER_REQUEST = 100;

    private final RestClient client;
    private final JsonMapper json;

    ExpoPushSender(RestClient client, JsonMapper json) {
        this.client = client;
        this.json = json;
    }

    @Override
    public List<String> send(List<PushMessage> messages) {
        List<String> unregistered = new ArrayList<>();
        for (int start = 0; start < messages.size(); start += MAX_MESSAGES_PER_REQUEST) {
            List<PushMessage> chunk =
                    messages.subList(start, Math.min(messages.size(), start + MAX_MESSAGES_PER_REQUEST));
            JsonNode tickets = post(chunk);
            // Tickets come back in the order of the messages.
            for (int i = 0; i < chunk.size(); i++) {
                JsonNode ticket = tickets.path(i);
                if ("DeviceNotRegistered"
                        .equals(ticket.path("details").path("error").stringValue(""))) {
                    unregistered.add(chunk.get(i).pushToken());
                }
            }
        }
        return unregistered;
    }

    private JsonNode post(List<PushMessage> chunk) {
        List<Map<String, Object>> body =
                chunk.stream().map(ExpoPushSender::toExpo).toList();
        try {
            String response = client.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(json.writeValueAsString(body))
                    .retrieve()
                    .body(String.class);
            JsonNode root = json.readTree(response == null ? "" : response);
            JsonNode errors = root.path("errors");
            if (!errors.isEmpty()) {
                throw new PushUnavailableException(
                        "Expo push error " + errors.path(0).path("code").stringValue("unknown"), null);
            }
            return root.path("data");
        } catch (RestClientResponseException exception) {
            throw new PushUnavailableException(
                    "Expo push HTTP " + exception.getStatusCode().value(), exception);
        } catch (RestClientException | JacksonException exception) {
            throw new PushUnavailableException("Expo push unreachable: " + exception.getMessage(), exception);
        }
    }

    private static Map<String, Object> toExpo(PushMessage message) {
        Map<String, Object> expo = new LinkedHashMap<>();
        expo.put("to", message.pushToken());
        expo.put("title", message.title());
        expo.put("body", message.body());
        expo.put("data", message.data());
        expo.put("channelId", message.channel());
        expo.put("priority", "high");
        expo.put("sound", "default");
        return expo;
    }
}
