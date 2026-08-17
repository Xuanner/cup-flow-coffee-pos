package com.cupflow.pos.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Money(long cents) {

    public static Money zero() {
        return new Money(0);
    }

    public static Money fromYuan(BigDecimal yuan) {
        if (yuan == null) {
            throw new IllegalArgumentException("金额不能为空");
        }
        try {
            return new Money(
                    yuan.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact());
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("金额最多保留两位小数且不能超出范围", exception);
        }
    }

    public Money add(Money other) {
        return new Money(Math.addExact(cents, other.cents));
    }

    public Money multiply(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("数量不能为负数");
        }
        return new Money(Math.multiplyExact(cents, quantity));
    }

    public BigDecimal toYuan() {
        return BigDecimal.valueOf(cents, 2);
    }
}
