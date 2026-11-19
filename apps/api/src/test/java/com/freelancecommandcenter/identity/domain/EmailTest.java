package com.freelancecommandcenter.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.freelancecommandcenter.shared.error.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class EmailTest {

    @ParameterizedTest
    @ValueSource(strings = {"Jane.Doe@Example.COM", "  jane.doe@example.com  "})
    void isNormalizedToTrimmedLowerCase(String input) {
        assertThat(new Email(input).value()).isEqualTo("jane.doe@example.com");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "jane", "jane@", "@example.com", "jane@example", "jane doe@example.com"})
    void rejectsInvalidAddresses(String input) {
        assertThatThrownBy(() -> new Email(input))
                .isInstanceOf(DomainException.class)
                .extracting("code")
                .isEqualTo("identity.invalid_email");
    }

    @Test
    void rejectsAddressesLongerThan254Characters() {
        String local = "a".repeat(255 - "@example.com".length());

        assertThatThrownBy(() -> new Email(local + "@example.com")).isInstanceOf(DomainException.class);
    }
}
