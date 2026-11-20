package com.freelancecommandcenter.shared.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class KeysetCursorTest {

    @Test
    void survivesTheRoundTripThroughItsOpaqueForm() {
        KeysetCursor cursor = new KeysetCursor(Instant.parse("2026-11-20T10:15:30.123456Z"), UUID.randomUUID());

        String encoded = cursor.encode();

        assertThat(encoded).matches("[A-Za-z0-9_-]+");
        assertThat(KeysetCursor.decode(encoded)).isEqualTo(cursor);
    }

    @Test
    void rejectsCursorsItDidNotIssue() {
        for (String cursor : List.of("not-a-cursor", "", "MTox", "%%%")) {
            assertThatThrownBy(() -> KeysetCursor.decode(cursor))
                    .extracting("code")
                    .isEqualTo("pagination.invalid_cursor");
        }
    }

    @Test
    void aPageHasANextCursorOnlyWhenMoreRowsExist() {
        UUID last = UUID.randomUUID();
        List<KeysetCursor> rows = List.of(
                new KeysetCursor(Instant.ofEpochSecond(3), UUID.randomUUID()),
                new KeysetCursor(Instant.ofEpochSecond(2), last),
                new KeysetCursor(Instant.ofEpochSecond(1), UUID.randomUUID()));

        assertThat(Page.of(rows, 2, row -> row).next()).contains(new KeysetCursor(Instant.ofEpochSecond(2), last));
        assertThat(Page.of(rows, 3, row -> row).next()).isEmpty();
    }
}
