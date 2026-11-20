package com.freelancecommandcenter.shared.pagination;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** One page of a keyset-paginated list; {@code next} is empty on the last page. */
public record Page<T>(List<T> items, Optional<KeysetCursor> next) {

    public Page {
        items = List.copyOf(items);
    }

    /**
     * Builds a page from a query that fetched one row more than {@code limit}: the extra row only signals that another
     * page exists.
     */
    public static <T> Page<T> of(List<T> rowsPlusOne, int limit, Function<T, KeysetCursor> position) {
        if (rowsPlusOne.size() <= limit) {
            return new Page<>(rowsPlusOne, Optional.empty());
        }
        List<T> items = rowsPlusOne.subList(0, limit);
        return new Page<>(items, Optional.of(position.apply(items.getLast())));
    }

    public <R> Page<R> map(Function<T, R> mapper) {
        return new Page<>(items.stream().map(mapper).toList(), next);
    }
}
