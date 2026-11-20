package com.freelancecommandcenter.matching.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.freelancecommandcenter.shared.error.DomainException;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class JobFilterTest {

    private final JobFilter filter = JobFilter.defaultFor(WorkspaceId.newId());

    @Test
    void theDefaultFilterIncludesAllPlatformsButMatchesNothing() {
        assertThat(filter.platforms()).containsExactly(PlatformCode.values());
        assertThat(filter.matches(PlatformCode.UPWORK, "Spring Boot API", "", List.of("Java")))
                .isFalse();
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "java        | Senior Java developer          | true",
                "java        | JavaScript widget              | false",
                "spring boot | Migrate to SPRING   BOOT 4     | true",
                "spring boot | Spring Bootstrap theme       | false",
                "spring boot | Migrate to Spring Boot 4       | true",
                "c++         | Fix a C++ build                | true",
                "node.js     | Node.js and React              | true",
                "laravel     | Laravel-based shop             | true",
                "çeviri      | Türkçe ÇEVİRİ işi              | true"
            })
    void keywordsMatchWholeWordsIgnoringCase(String keyword, String title, boolean expected) {
        filter.change(List.of(Keyword.of(keyword)), List.of(PlatformCode.FREELANCER));

        assertThat(filter.matches(PlatformCode.FREELANCER, title, "", List.of()))
                .isEqualTo(expected);
    }

    @Test
    void descriptionAndSkillsAreSearchedToo() {
        filter.change(List.of(Keyword.of("postgresql"), Keyword.of("kotlin")), List.of(PlatformCode.FREELANCER));

        assertThat(filter.matches(PlatformCode.FREELANCER, "Backend", "Uses PostgreSQL", List.of()))
                .isTrue();
        assertThat(filter.matches(PlatformCode.FREELANCER, "Backend", "", List.of("Kotlin")))
                .isTrue();
        assertThat(filter.matches(PlatformCode.FREELANCER, "Backend", "", List.of("Go")))
                .isFalse();
    }

    @Test
    void jobsOfOtherPlatformsDoNotMatch() {
        filter.change(List.of(Keyword.of("java")), List.of(PlatformCode.FREELANCER));

        assertThat(filter.matches(PlatformCode.UPWORK, "Java developer", "", List.of()))
                .isFalse();
    }

    @Test
    void keywordsAreNormalizedAndDeduplicated() {
        filter.change(
                List.of(Keyword.of("  Spring   Boot "), Keyword.of("spring boot"), Keyword.of("Java")),
                List.of(PlatformCode.UPWORK, PlatformCode.FREELANCER));

        assertThat(filter.keywords()).extracting(Keyword::value).containsExactly("spring boot", "java");
        assertThat(filter.platforms()).containsExactly(PlatformCode.FREELANCER, PlatformCode.UPWORK);
    }

    @Test
    void rejectsBlankOrLongKeywordsAndTooManyKeywords() {
        assertThatThrownBy(() -> Keyword.of(" "))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "matching.invalid_keyword");
        assertThatThrownBy(() -> Keyword.of("x".repeat(Keyword.MAX_LENGTH + 1))).isInstanceOf(DomainException.class);

        List<Keyword> tooMany = IntStream.rangeClosed(0, JobFilter.MAX_KEYWORDS)
                .mapToObj(i -> Keyword.of("k" + i))
                .toList();
        assertThatThrownBy(() -> filter.change(tooMany, List.of(PlatformCode.FREELANCER)))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "matching.too_many_keywords");
    }
}
