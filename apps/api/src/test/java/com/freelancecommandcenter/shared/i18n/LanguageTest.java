package com.freelancecommandcenter.shared.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LanguageTest {

    @ParameterizedTest
    @CsvSource(
            nullValues = "null",
            value = {"tr-TR,TR", "tr,TR", "en-GB,EN", "de-DE,EN", "'',EN", "null,EN"})
    void resolvesTagsToSupportedLanguagesWithEnglishFallback(String tag, Language expected) {
        assertThat(Language.fromTag(tag)).isEqualTo(expected);
    }
}
