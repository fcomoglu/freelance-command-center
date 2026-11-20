package com.freelancecommandcenter.job.adapter.out.persistence;

import com.freelancecommandcenter.job.domain.Job;
import com.freelancecommandcenter.job.domain.JobBudget;
import com.freelancecommandcenter.job.domain.JobContent;
import com.freelancecommandcenter.shared.id.JobId;
import com.freelancecommandcenter.shared.money.Money;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(schema = "job", name = "jobs")
class JobEntity {

    @Id
    private UUID id;

    private String platform;
    private String externalId;
    private String title;
    private String description;
    private String url;
    private String budgetType;
    private String budgetCurrency;
    private BigDecimal budgetMinimum;
    private BigDecimal budgetMaximum;

    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] skills;

    private Integer bidCount;
    private String language;
    private Instant postedAt;
    private Instant sourceUpdatedAt;
    private Instant discoveredAt;

    @Version
    private Long version;

    protected JobEntity() {}

    JobEntity(UUID id) {
        this.id = id;
    }

    void apply(Job job) {
        JobContent content = job.content();
        platform = job.platform().value();
        externalId = job.externalId();
        title = content.title();
        description = content.description();
        url = content.url().toString();
        budgetType = content.budget().map(budget -> budget.type().name()).orElse(null);
        budgetCurrency = content.budget()
                .map(budget -> budget.minimum().currency().getCurrencyCode())
                .orElse(null);
        budgetMinimum =
                content.budget().map(budget -> budget.minimum().amount()).orElse(null);
        budgetMaximum =
                content.budget().flatMap(JobBudget::maximum).map(Money::amount).orElse(null);
        skills = content.skills().toArray(String[]::new);
        bidCount = content.bidCount().orElse(null);
        language = content.language().orElse(null);
        postedAt = job.postedAt();
        sourceUpdatedAt = job.sourceUpdatedAt();
        discoveredAt = job.discoveredAt();
    }

    Job toDomain() {
        JobContent content = new JobContent(
                title,
                description,
                URI.create(url),
                budget(),
                List.of(skills),
                Optional.ofNullable(bidCount),
                Optional.ofNullable(language));
        return Job.restore(
                new JobId(id),
                PlatformCode.fromValue(platform),
                externalId,
                content,
                postedAt,
                sourceUpdatedAt,
                discoveredAt);
    }

    private Optional<JobBudget> budget() {
        if (budgetType == null) {
            return Optional.empty();
        }
        Currency currency = Currency.getInstance(budgetCurrency);
        return Optional.of(new JobBudget(
                JobBudget.Type.valueOf(budgetType),
                new Money(budgetMinimum, currency),
                Optional.ofNullable(budgetMaximum).map(amount -> new Money(amount, currency))));
    }
}
