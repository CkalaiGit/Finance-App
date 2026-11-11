package com.cairedine.finance.app.financialanalysis.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FinancialMathTest {

    @Test
    void shouldPreserveMissingValuesAndRealZeros() {
        assertNull(FinancialMath.safeValue(null));
        assertEquals(new BigDecimal("0.0000"), FinancialMath.safeValue(BigDecimal.ZERO));
        assertEquals(new BigDecimal("0.0000"),
                FinancialMath.calculateRatio(BigDecimal.ZERO, BigDecimal.TEN));
    }

    @Test
    void shouldReturnNullWhenRatioOrGrowthCannotBeCalculated() {
        assertNull(FinancialMath.calculateRatio(null, BigDecimal.ONE));
        assertNull(FinancialMath.calculateRatio(BigDecimal.ONE, BigDecimal.ZERO));
        assertNull(FinancialMath.calculateGrowth(BigDecimal.ONE, null));
        assertNull(FinancialMath.calculateGrowth(BigDecimal.ONE, BigDecimal.ZERO));
    }

    @Test
    void shouldReturnNullWhenCagrInputsAreMissingOrNonPositive() {
        assertNull(FinancialMath.calculateCAGR(null, BigDecimal.ONE));
        assertNull(FinancialMath.calculateCAGR(BigDecimal.ONE, BigDecimal.ZERO));
        assertNull(FinancialMath.calculateCAGR(BigDecimal.ONE, BigDecimal.ONE.negate()));
        assertNull(FinancialMath.calculateCAGR(BigDecimal.ZERO, BigDecimal.ONE));
    }

    @Test
    void shouldReturnNullWhenPegCannotBeCalculated() {
        assertNull(FinancialMath.calculatePegRatio(null, BigDecimal.ONE));
        assertNull(FinancialMath.calculatePegRatio(BigDecimal.TEN, null));
        assertNull(FinancialMath.calculatePegRatio(BigDecimal.TEN, BigDecimal.ZERO));
        assertNull(FinancialMath.calculatePegRatio(BigDecimal.TEN, BigDecimal.ONE.negate()));
    }
}
