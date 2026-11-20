package com.freelancecommandcenter.job.domain;

import com.freelancecommandcenter.shared.money.Money;
import java.util.Objects;
import java.util.Optional;

/** What the client pays: a fixed price or an hourly rate, either a single amount or a range. */
public record JobBudget(Type type, Money minimum, Optional<Money> maximum) {

    public JobBudget {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(minimum, "minimum");
        Objects.requireNonNull(maximum, "maximum");
        maximum.ifPresent(max -> {
            if (max.compareAmount(minimum) < 0) {
                throw new IllegalArgumentException("Budget maximum is below its minimum");
            }
        });
    }

    public enum Type {
        FIXED,
        HOURLY
    }
}
