package com.freelancecommandcenter.platform.adapter.out.freelancer;

import com.freelancecommandcenter.platform.api.JobListing;
import com.freelancecommandcenter.platform.api.JobListing.Budget;
import com.freelancecommandcenter.platform.api.JobListing.BudgetType;
import com.freelancecommandcenter.shared.money.Money;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import tools.jackson.databind.JsonNode;

/**
 * Translates a Freelancer.com project (projects API 0.1) into a {@link JobListing}. Tolerant of missing optional
 * fields; a project without an id, title or submit time is skipped.
 */
class FreelancerProjectTranslator {

    private final String siteUrl;

    FreelancerProjectTranslator(URI siteUrl) {
        this.siteUrl = siteUrl.toString().replaceAll("/+$", "");
    }

    Optional<JobListing> translate(JsonNode project) {
        Optional<Long> id = longValue(project.path("id"));
        String title = text(project.path("title"));
        Optional<Instant> submitted = longValue(project.path("time_submitted")).map(Instant::ofEpochSecond);
        if (id.isEmpty() || title.isBlank() || submitted.isEmpty()) {
            return Optional.empty();
        }
        Instant updated = longValue(project.path("time_updated"))
                .map(Instant::ofEpochSecond)
                .orElse(submitted.get());
        String language = text(project.path("language"));
        return Optional.of(new JobListing(
                PlatformCode.FREELANCER,
                id.get().toString(),
                title.strip(),
                description(project),
                url(project, id.get()),
                budget(project).orElse(null),
                skills(project),
                longValue(project.path("bid_stats").path("bid_count"))
                        .map(Long::intValue)
                        .orElse(null),
                language.isBlank() ? null : language,
                submitted.get(),
                updated));
    }

    /** The full description is present only when the request asks for it; the preview is the fallback. */
    private static String description(JsonNode project) {
        String full = text(project.path("description"));
        return (full.isBlank() ? text(project.path("preview_description")) : full).strip();
    }

    private URI url(JsonNode project, long id) {
        String seoUrl = text(project.path("seo_url"));
        return URI.create(siteUrl + "/projects/" + (seoUrl.isBlank() ? Long.toString(id) : seoUrl));
    }

    private static Optional<Budget> budget(JsonNode project) {
        BudgetType type = "hourly".equals(text(project.path("type"))) ? BudgetType.HOURLY : BudgetType.FIXED;
        Optional<Currency> currency = currency(text(project.path("currency").path("code")));
        Optional<BigDecimal> minimum = decimal(project.path("budget").path("minimum"));
        if (currency.isEmpty() || minimum.isEmpty()) {
            return Optional.empty();
        }
        Optional<BigDecimal> maximum = decimal(project.path("budget").path("maximum"));
        return Optional.of(new Budget(
                type,
                money(minimum.get(), currency.get()),
                maximum.map(amount -> money(amount, currency.get())).orElse(null)));
    }

    private static List<String> skills(JsonNode project) {
        List<String> skills = new ArrayList<>();
        for (JsonNode job : project.path("jobs").values()) {
            String name = text(job.path("name"));
            if (!name.isBlank()) {
                skills.add(name.strip());
            }
        }
        return skills;
    }

    private static Money money(BigDecimal amount, Currency currency) {
        int scale = Math.max(0, currency.getDefaultFractionDigits());
        return new Money(amount.setScale(scale, RoundingMode.HALF_UP), currency);
    }

    private static Optional<Currency> currency(String code) {
        try {
            return Optional.of(Currency.getInstance(code));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static String text(JsonNode node) {
        return node.isString() ? node.stringValue() : "";
    }

    private static Optional<Long> longValue(JsonNode node) {
        return node.isNumber() ? Optional.of(node.longValue()) : Optional.empty();
    }

    private static Optional<BigDecimal> decimal(JsonNode node) {
        return node.isNumber() ? Optional.of(node.decimalValue()) : Optional.empty();
    }
}
