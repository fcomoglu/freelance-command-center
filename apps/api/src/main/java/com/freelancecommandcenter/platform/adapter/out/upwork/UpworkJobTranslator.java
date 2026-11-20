package com.freelancecommandcenter.platform.adapter.out.upwork;

import com.freelancecommandcenter.platform.api.JobListing;
import com.freelancecommandcenter.platform.api.JobListing.Budget;
import com.freelancecommandcenter.platform.api.JobListing.BudgetType;
import com.freelancecommandcenter.shared.money.Money;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import tools.jackson.databind.JsonNode;

/**
 * Translates an Upwork job search result ({@code MarketplaceJobPostingSearchResult} of the GraphQL API) into a
 * {@link JobListing}. A result without an id, title, link or publish time is skipped.
 */
class UpworkJobTranslator {

    private final String siteUrl;

    UpworkJobTranslator(URI siteUrl) {
        this.siteUrl = siteUrl.toString().replaceAll("/+$", "");
    }

    Optional<JobListing> translate(JsonNode job) {
        String id = text(job.path("id"));
        String title = text(job.path("title"));
        String ciphertext = text(job.path("ciphertext"));
        Optional<Instant> published =
                instant(job.path("publishedDateTime")).or(() -> instant(job.path("createdDateTime")));
        if (id.isBlank() || title.isBlank() || ciphertext.isBlank() || published.isEmpty()) {
            return Optional.empty();
        }
        Instant updated = instant(job.path("renewedDateTime"))
                .filter(renewed -> renewed.isAfter(published.get()))
                .orElse(published.get());
        return Optional.of(new JobListing(
                PlatformCode.UPWORK,
                id,
                title.strip(),
                text(job.path("description")).strip(),
                // Job pages are addressed by the ciphertext, which starts with "~".
                URI.create(siteUrl + "/jobs/" + (ciphertext.startsWith("~") ? ciphertext : "~" + ciphertext)),
                budget(job).orElse(null),
                skills(job),
                job.path("totalApplicants").isNumber()
                        ? job.path("totalApplicants").intValue()
                        : null,
                null,
                published.get(),
                updated));
    }

    /** Hourly jobs carry a rate range; fixed-price jobs an amount. Zero means the client gave no budget. */
    private static Optional<Budget> budget(JsonNode job) {
        Optional<Money> hourlyMin = money(job.path("hourlyBudgetMin"));
        Optional<Money> hourlyMax = money(job.path("hourlyBudgetMax"));
        if (hourlyMin.isPresent() || hourlyMax.isPresent()) {
            Money minimum = hourlyMin.orElse(hourlyMax.get());
            Money maximum = hourlyMax
                    .filter(max -> hourlyMin.isPresent() && max.compareAmount(minimum) > 0)
                    .orElse(null);
            return Optional.of(new Budget(BudgetType.HOURLY, minimum, maximum));
        }
        return money(job.path("amount")).map(amount -> new Budget(BudgetType.FIXED, amount, null));
    }

    private static List<String> skills(JsonNode job) {
        List<String> skills = new ArrayList<>();
        for (JsonNode skill : job.path("skills").values()) {
            String name = text(skill.path("prettyName"));
            String fallback = text(skill.path("name"));
            String chosen = name.isBlank() ? fallback : name;
            if (!chosen.isBlank()) {
                skills.add(chosen.strip());
            }
        }
        return skills;
    }

    private static Optional<Money> money(JsonNode node) {
        Currency currency;
        BigDecimal amount;
        try {
            currency = Currency.getInstance(text(node.path("currency")));
            amount = new BigDecimal(text(node.path("rawValue")));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
        if (amount.signum() <= 0) {
            return Optional.empty();
        }
        int scale = Math.max(0, currency.getDefaultFractionDigits());
        return Optional.of(new Money(amount.setScale(scale, RoundingMode.HALF_UP), currency));
    }

    /** ISO 8601 with an offset, or epoch milliseconds as a string or number. */
    private static Optional<Instant> instant(JsonNode node) {
        if (node.isNumber()) {
            return Optional.of(Instant.ofEpochMilli(node.longValue()));
        }
        String value = text(node);
        if (value.isBlank()) {
            return Optional.empty();
        }
        try {
            if (value.chars().allMatch(Character::isDigit)) {
                return Optional.of(Instant.ofEpochMilli(Long.parseLong(value)));
            }
            return Optional.of(OffsetDateTime.parse(value).toInstant());
        } catch (DateTimeException | NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static String text(JsonNode node) {
        return node.isString() ? node.stringValue() : "";
    }
}
