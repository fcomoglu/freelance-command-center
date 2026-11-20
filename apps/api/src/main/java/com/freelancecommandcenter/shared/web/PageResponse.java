package com.freelancecommandcenter.shared.web;

import com.freelancecommandcenter.shared.pagination.KeysetCursor;
import com.freelancecommandcenter.shared.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.function.Function;

/**
 * JSON form of a {@link Page}: {@code nextCursor} is passed back as {@code ?cursor=} and is absent on the last page
 * (see docs/architecture.md, API conventions).
 */
@Schema(requiredProperties = "items")
public record PageResponse<T>(List<T> items, String nextCursor) {

    public static <S, T> PageResponse<T> from(Page<S> page, Function<S, T> mapper) {
        return new PageResponse<>(
                page.items().stream().map(mapper).toList(),
                page.next().map(KeysetCursor::encode).orElse(null));
    }
}
