package com.cairedine.finance.app.financialanalysis.domain.model;

import java.math.BigDecimal;

public record GrowthMetrics(
        BigDecimal revenueGrowth3Y,
        BigDecimal ebitdaGrowth3Y,
        BigDecimal epsGrowth3Y,
        BigDecimal fcfGrowth
) {}
