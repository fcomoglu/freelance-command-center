package com.freelancecommandcenter.shared.i18n;

import java.util.Locale;

/** Languages the product supports for user-facing text. {@link #EN} is the fallback. */
public enum Language {
    EN,
    TR;

    public String tag() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Locale locale() {
        return Locale.forLanguageTag(tag());
    }

    /** Returns the language for a tag such as {@code tr-TR}, falling back to {@link #EN}. */
    public static Language fromTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return EN;
        }
        String primary = Locale.forLanguageTag(tag.trim()).getLanguage();
        for (Language language : values()) {
            if (language.tag().equals(primary)) {
                return language;
            }
        }
        return EN;
    }
}
