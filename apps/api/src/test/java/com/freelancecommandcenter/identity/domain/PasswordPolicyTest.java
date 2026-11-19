package com.freelancecommandcenter.identity.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.freelancecommandcenter.shared.error.DomainException;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    @Test
    void acceptsPasswordsWithinTheLengthBounds() {
        assertThatCode(() -> PasswordPolicy.check("a".repeat(PasswordPolicy.MIN_LENGTH)))
                .doesNotThrowAnyException();
        assertThatCode(() -> PasswordPolicy.check("a".repeat(PasswordPolicy.MAX_LENGTH)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsTooShortTooLongAndBlankPasswords() {
        for (String password : new String[] {
            null, "a".repeat(PasswordPolicy.MIN_LENGTH - 1), "a".repeat(PasswordPolicy.MAX_LENGTH + 1), " ".repeat(12)
        }) {
            assertThatThrownBy(() -> PasswordPolicy.check(password))
                    .isInstanceOf(DomainException.class)
                    .extracting("code")
                    .isEqualTo("identity.weak_password");
        }
    }
}
