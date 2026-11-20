package com.freelancecommandcenter.shared.platform;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

/** A freelance platform the product integrates with. */
public enum PlatformCode {
    FREELANCER,
    UPWORK,
    FIVERR,
    BIONLUK,
    ARC_DEV;

    /** The API and storage form: {@code freelancer}, {@code arc_dev}. */
    @JsonValue
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static PlatformCode fromValue(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
