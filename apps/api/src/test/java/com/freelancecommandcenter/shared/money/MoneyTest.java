package com.freelancecommandcenter.shared.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.freelancecommandcenter.shared.error.DomainException;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void amountsUseTheMinorUnitScaleOfTheCurrency() {
        assertThat(Money.of("25", "USD").amount()).hasToString("25.00");
        assertThat(Money.of("25.5000", "USD")).isEqualTo(Money.of("25.50", "USD"));
        assertThat(Money.of("1500", "JPY").amount()).hasToString("1500");
    }

    @Test
    void moreDecimalsThanTheCurrencyAllowsAreRejected() {
        assertThatThrownBy(() -> Money.of("25.001", "USD"))
                .isInstanceOf(DomainException.class)
                .extracting("code")
                .isEqualTo("money.invalid_amount");
        assertThatThrownBy(() -> Money.of("10.5", "JPY")).extracting("code").isEqualTo("money.invalid_amount");
    }

    @Test
    void unknownCurrenciesAndMalformedAmountsAreRejected() {
        assertThatThrownBy(() -> Money.of("10", "ABC")).extracting("code").isEqualTo("money.invalid_currency");
        assertThatThrownBy(() -> Money.of("ten", "USD")).extracting("code").isEqualTo("money.invalid_amount");
    }

    @Test
    void amountsCompareOnlyWithinOneCurrency() {
        assertThat(Money.of("10", "EUR").compareAmount(Money.of("20", "EUR"))).isNegative();
        assertThatThrownBy(() -> Money.of("10", "EUR").compareAmount(Money.of("10", "USD")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
