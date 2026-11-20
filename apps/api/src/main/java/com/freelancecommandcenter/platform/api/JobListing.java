package com.freelancecommandcenter.platform.api;

import com.freelancecommandcenter.shared.money.Money;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * A job listing in the platform-neutral form that every platform adapter translates into (the anti-corruption layer of
 * ADR 0010). {@code platform} and {@code externalId} identify the listing on its platform.
 *
 * @param description plain text; empty when the platform gives none
 * @param budget {@code null} when the platform gives no budget
 * @param bidCount {@code null} when the platform does not publish it
 * @param language ISO 639-1 code of the listing's language, {@code null} when unknown
 * @param updatedAt when the platform last changed the listing; a newer value replaces older data
 */
public record JobListing(
        PlatformCode platform,
        String externalId,
        String title,
        String description,
        URI url,
        Budget budget,
        List<String> skills,
        Integer bidCount,
        String language,
        Instant postedAt,
        Instant updatedAt) {

    public JobListing {
        Objects.requireNonNull(platform, "platform");
        Objects.requireNonNull(externalId, "externalId");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(url, "url");
        skills = List.copyOf(skills);
        Objects.requireNonNull(postedAt, "postedAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /** What the client pays: a fixed price or an hourly rate, as a range when {@code maximum} is set. */
    public record Budget(BudgetType type, Money minimum, Money maximum) {

        public Budget {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(minimum, "minimum");
        }
    }

    public enum BudgetType {
        FIXED,
        HOURLY
    }
}
