package com.freelancecommandcenter;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

/** Reads e-mails delivered to Mailpit. */
public class Mailbox {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON_OBJECT =
            new ParameterizedTypeReference<>() {};
    private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+)");

    private final RestClient client;

    Mailbox(String apiUrl) {
        this.client = RestClient.create(apiUrl);
    }

    /** Waits for an e-mail to the address whose subject contains {@code subjectPart}. */
    public Mail awaitMail(String to, String subjectPart) {
        return awaitMails(to, subjectPart, 1).getFirst();
    }

    /** Waits until at least {@code count} matching e-mails arrived; newest first. */
    public List<Mail> awaitMails(String to, String subjectPart, int count) {
        List<Map<String, Object>> found = await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(200))
                .until(() -> search(to, subjectPart), messages -> messages.size() >= count);
        return found.stream().map(this::read).toList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> search(String to, String subjectPart) {
        Map<String, Object> result = client.get()
                .uri(uri -> uri.path("/search")
                        .queryParam("query", "to:\"" + to + "\"")
                        .build())
                .retrieve()
                .body(JSON_OBJECT);
        return ((List<Map<String, Object>>) result.get("messages"))
                .stream()
                        .filter(message -> ((String) message.get("Subject")).contains(subjectPart))
                        .toList();
    }

    private Mail read(Map<String, Object> summary) {
        Map<String, Object> full =
                client.get().uri("/message/{id}", summary.get("ID")).retrieve().body(JSON_OBJECT);
        return new Mail((String) full.get("Subject"), (String) full.get("Text"));
    }

    public record Mail(String subject, String text) {

        /** The token query parameter of the link in the e-mail. */
        public String token() {
            Matcher matcher = TOKEN.matcher(text);
            if (!matcher.find()) {
                throw new AssertionError("No token link in e-mail: " + text);
            }
            return matcher.group(1);
        }
    }
}
