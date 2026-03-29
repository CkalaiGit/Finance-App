package com.cairedine.finance.app.financialanalysis.infrastructure.web.dto;

import java.math.BigDecimal;

public record GrowthMetricsResponse(
        BigDecimal revenueGrowth3Y,
        BigDecimal ebitdaGrowth,
        BigDecimal epsGrowth,
        BigDecimal fcfGrowth
) {}
