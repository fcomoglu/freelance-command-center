package com.freelancecommandcenter.shared.money;

import com.freelancecommandcenter.shared.error.DomainException;
import com.freelancecommandcenter.shared.error.ErrorKind;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * An amount in one ISO 4217 currency. The amount is stored with the currency's minor-unit scale (2 for USD, 0 for
 * JPY), so equal values are equal objects.
 */
public record Money(BigDecimal amount, Currency currency) {

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        int scale = Math.max(0, currency.getDefaultFractionDigits());
        if (amount.stripTrailingZeros().scale() > scale) {
            throw invalidAmount();
        }
        amount = amount.setScale(scale);
    }

    /** Parses the API representation: a decimal string and a currency code such as {@code USD}. */
    public static Money of(String amount, String currencyCode) {
        Currency currency;
        try {
            currency = Currency.getInstance(currencyCode);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new DomainException(ErrorKind.INVALID, "money.invalid_currency", "Unknown currency code");
        }
        try {
            return new Money(new BigDecimal(amount), currency);
        } catch (NumberFormatException | NullPointerException exception) {
            throw invalidAmount();
        }
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean hasSameCurrencyAs(Money other) {
        return currency.equals(other.currency);
    }

    /** Compares amounts of the same currency; fails for different currencies. */
    public int compareAmount(Money other) {
        if (!hasSameCurrencyAs(other)) {
            throw new IllegalArgumentException("Cannot compare " + currency + " with " + other.currency);
        }
        return amount.compareTo(other.amount);
    }

    private static DomainException invalidAmount() {
        return new DomainException(ErrorKind.INVALID, "money.invalid_amount", "Amount is not valid for the currency");
    }
}
