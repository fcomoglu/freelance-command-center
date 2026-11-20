package com.freelancecommandcenter.platform.adapter.out.upwork;

import com.freelancecommandcenter.platform.api.PlatformMessage;
import com.freelancecommandcenter.platform.api.PlatformMessage.Detail;
import com.freelancecommandcenter.platform.api.PlatformMessage.Direction;
import com.freelancecommandcenter.platform.application.port.out.MessageSource;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import tools.jackson.databind.JsonNode;

/**
 * Reads a connected account's messages from Upwork's message rooms: one GraphQL request returns the rooms with
 * activity since the last read together with their new stories, a bounded number of room pages per read.
 */
class UpworkMessageSource implements MessageSource {

    private final UpworkAccountApi api;
    private final URI site;
    private final String query;
    private final int roomPageSize;
    private final int maxRoomPages;
    private final int storiesPerRoom;

    UpworkMessageSource(
            UpworkAccountApi api, URI site, String query, int roomPageSize, int maxRoomPages, int storiesPerRoom) {
        this.api = api;
        this.site = site;
        this.query = query;
        this.roomPageSize = roomPageSize;
        this.maxRoomPages = maxRoomPages;
        this.storiesPerRoom = storiesPerRoom;
    }

    @Override
    public PlatformCode platform() {
        return PlatformCode.UPWORK;
    }

    @Override
    public List<PlatformMessage> fetchSince(String accountId, String accessToken, Instant since) {
        List<PlatformMessage> messages = new ArrayList<>();
        String after = null;
        for (int page = 0; page < maxRoomPages; page++) {
            JsonNode rooms =
                    api.query(accessToken, query, variables(since, after)).path("roomList");
            for (JsonNode edge : rooms.path("edges").values()) {
                readRoom(edge.path("node"), accountId, since, messages);
            }
            JsonNode pageInfo = rooms.path("pageInfo");
            after = pageInfo.path("endCursor").asString("");
            if (!pageInfo.path("hasNextPage").asBoolean(false) || after.isBlank()) {
                break;
            }
        }
        return messages;
    }

    /** Date-time filters take ISO 8601 strings. */
    private Map<String, Object> variables(Instant since, String after) {
        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("first", roomPageSize);
        if (after != null) {
            pagination.put("after", after);
        }
        return Map.of(
                "filter", Map.of("subscribed_eq", true, "activeSince_eq", since.toString()),
                "pagination", pagination,
                "stories",
                        Map.of(
                                "storyCreatedDateTime_bt", Map.of("rangeStart", since.toString()),
                                "pagination", Map.of("first", storiesPerRoom)));
    }

    /** Stories without an author are room events (a contract started, someone joined), not messages. */
    private void readRoom(JsonNode room, String accountId, Instant since, List<PlatformMessage> messages) {
        String roomId = room.path("id").asString("");
        if (roomId.isBlank()) {
            return;
        }
        String counterpart = counterpart(room, accountId);
        URI url = site.resolve("/ab/messages/rooms/" + roomId);
        for (JsonNode edge : room.path("stories").path("edges").values()) {
            JsonNode story = edge.path("node");
            String id = story.path("id").asString("");
            String author = story.path("user").path("id").asString("");
            Optional<Instant> sentAt = UpworkJobTranslator.instant(story.path("createdDateTime"));
            if (id.isBlank()
                    || author.isBlank()
                    || sentAt.isEmpty()
                    || sentAt.get().isBefore(since)) {
                continue;
            }
            messages.add(new PlatformMessage(
                    PlatformCode.UPWORK,
                    roomId,
                    id,
                    counterpart,
                    author.equals(accountId) ? Direction.OUTBOUND : Direction.INBOUND,
                    Detail.FULL,
                    story.path("message").asString("").strip(),
                    url,
                    sentAt.get()));
        }
    }

    /** The other members by name; the room's name or topic when there are none. */
    private static String counterpart(JsonNode room, String accountId) {
        List<String> others = new ArrayList<>();
        for (JsonNode member : room.path("roomUsers").values()) {
            JsonNode user = member.path("user");
            String name = user.path("name").asString("").strip();
            if (!name.isEmpty() && !accountId.equals(user.path("id").asString(""))) {
                others.add(name);
            }
        }
        if (!others.isEmpty()) {
            return String.join(", ", others);
        }
        for (String field : new String[] {"roomName", "topic"}) {
            String value = room.path(field).asString("").strip();
            if (!value.isEmpty()) {
                return value;
            }
        }
        return "Upwork";
    }
}
