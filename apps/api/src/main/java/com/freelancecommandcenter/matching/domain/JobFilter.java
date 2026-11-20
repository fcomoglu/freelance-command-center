package com.freelancecommandcenter.matching.domain;

import com.freelancecommandcenter.shared.error.DomainException;
import com.freelancecommandcenter.shared.error.ErrorKind;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Which new jobs a workspace wants to hear about: jobs on one of the chosen platforms whose title, description or
 * skills contain at least one keyword. A filter without keywords matches nothing, so a new workspace gets no
 * notifications until it chooses its keywords.
 */
public final class JobFilter {

    public static final int MAX_KEYWORDS = 20;

    private final WorkspaceId workspaceId;
    private List<Keyword> keywords;
    private Set<PlatformCode> platforms;

    private JobFilter(WorkspaceId workspaceId, Collection<Keyword> keywords, Collection<PlatformCode> platforms) {
        this.workspaceId = Objects.requireNonNull(workspaceId, "workspaceId");
        change(keywords, platforms);
    }

    /** The filter of a workspace that has not set one: all platforms, no keywords. */
    public static JobFilter defaultFor(WorkspaceId workspaceId) {
        return new JobFilter(workspaceId, List.of(), EnumSet.allOf(PlatformCode.class));
    }

    public static JobFilter restore(
            WorkspaceId workspaceId, Collection<Keyword> keywords, Collection<PlatformCode> platforms) {
        return new JobFilter(workspaceId, keywords, platforms);
    }

    /** Replaces keywords and platforms; duplicate keywords are dropped, the order of the rest is kept. */
    public void change(Collection<Keyword> newKeywords, Collection<PlatformCode> newPlatforms) {
        List<Keyword> distinct = List.copyOf(new LinkedHashSet<>(newKeywords));
        if (distinct.size() > MAX_KEYWORDS) {
            throw new DomainException(
                    ErrorKind.INVALID, "matching.too_many_keywords", "At most " + MAX_KEYWORDS + " keywords");
        }
        this.keywords = distinct;
        this.platforms = newPlatforms.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(newPlatforms));
    }

    public boolean matches(PlatformCode platform, String title, String description, List<String> skills) {
        if (!platforms.contains(platform) || keywords.isEmpty()) {
            return false;
        }
        String text = title + "\n" + description + "\n" + String.join("\n", skills);
        return keywords.stream().anyMatch(keyword -> keyword.occursIn(text));
    }

    public WorkspaceId workspaceId() {
        return workspaceId;
    }

    public List<Keyword> keywords() {
        return keywords;
    }

    /** The chosen platforms in a stable order. */
    public List<PlatformCode> platforms() {
        return platforms.stream().sorted().toList();
    }
}
