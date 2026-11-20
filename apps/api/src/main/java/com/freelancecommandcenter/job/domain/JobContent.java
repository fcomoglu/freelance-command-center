package com.freelancecommandcenter.job.domain;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** The part of a job its platform can change after posting. */
public record JobContent(
        String title,
        String description,
        URI url,
        Optional<JobBudget> budget,
        List<String> skills,
        Optional<Integer> bidCount,
        Optional<String> language) {

    public JobContent {
        Objects.requireNonNull(title, "title");
        if (title.isBlank()) {
            throw new IllegalArgumentException("A job needs a title");
        }
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(budget, "budget");
        skills = List.copyOf(skills);
        Objects.requireNonNull(bidCount, "bidCount");
        Objects.requireNonNull(language, "language");
    }
}
