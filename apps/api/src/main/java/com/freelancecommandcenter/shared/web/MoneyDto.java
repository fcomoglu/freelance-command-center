package com.freelancecommandcenter.shared.web;

import com.freelancecommandcenter.shared.money.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** JSON form of {@link Money}: a decimal string and an ISO 4217 code (see docs/architecture.md, API conventions). */
@Schema(
        name = "Money",
        requiredProperties = {"amount", "currency"})
public record MoneyDto(
        @Schema(example = "1250.00") @NotNull @Pattern(regexp = "^\\d{1,15}(\\.\\d{1,4})?$")
        String amount,

        @Schema(example = "USD") @NotNull @Pattern(regexp = "^[A-Z]{3}$")
        String currency) {

    public static MoneyDto from(Money money) {
        return new MoneyDto(money.amount().toPlainString(), money.currency().getCurrencyCode());
    }

    public Money toMoney() {
        return Money.of(amount, currency);
    }
}
