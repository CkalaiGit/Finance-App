package com.cairedine.finance.app.financialanalysis.domain.model;

import java.math.BigDecimal;

public record ValueMetrics(
        BigDecimal evToEbit,
        BigDecimal peRatioTTM,
        BigDecimal pegRatioForward,
        BigDecimal evToSales
) {}