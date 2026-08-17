package com.cupflow.pos.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void convertsYuanToMinorUnitsWithoutFloatingPointLoss() {
        Money money = Money.fromYuan(new BigDecimal("19.90"));

        assertThat(money.cents()).isEqualTo(1990);
        assertThat(money.toYuan()).isEqualByComparingTo("19.90");
    }

    @Test
    void rejectsAmountsWithMoreThanTwoDecimalPlaces() {
        assertThatThrownBy(() -> Money.fromYuan(new BigDecimal("1.001")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("两位小数");
    }

    @Test
    void multipliesUsingExactIntegerArithmetic() {
        assertThat(new Money(1250).multiply(3)).isEqualTo(new Money(3750));
    }
}
