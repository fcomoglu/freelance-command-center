package com.freelancecommandcenter.shared.pagination;

import com.freelancecommandcenter.shared.error.DomainException;
import com.freelancecommandcenter.shared.error.ErrorKind;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

/**
 * Position after the last item of a page in a list ordered by a timestamp and then by id, both descending. Clients
 * see it only as an opaque string (see docs/architecture.md, API conventions).
 */
public record KeysetCursor(Instant at, UUID id) {

    public KeysetCursor {
        Objects.requireNonNull(at, "at");
        Objects.requireNonNull(id, "id");
    }

    public String encode() {
        String raw = at.getEpochSecond() + ":" + at.getNano() + ":" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static KeysetCursor decode(String cursor) {
        try {
            String[] parts = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8).split(":");
            if (parts.length != 3) {
                throw invalid();
            }
            return new KeysetCursor(
                    Instant.ofEpochSecond(Long.parseLong(parts[0]), Long.parseLong(parts[1])),
                    UUID.fromString(parts[2]));
        } catch (IllegalArgumentException | DateTimeException exception) {
            throw invalid();
        }
    }

    private static DomainException invalid() {
        return new DomainException(ErrorKind.INVALID, "pagination.invalid_cursor", "The cursor is not valid");
    }
}
