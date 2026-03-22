package com.cairedine.finance.app.webclient;

import java.math.BigDecimal;

public record KeyMetricsRecord(
        String symbol,
        BigDecimal returnOnInvestedCapitalTTM,
        BigDecimal netDebtToEbitda,
        BigDecimal evToEbit
) {}