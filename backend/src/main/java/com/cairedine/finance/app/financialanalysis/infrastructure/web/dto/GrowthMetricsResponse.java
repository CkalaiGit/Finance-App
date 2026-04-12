package com.cairedine.finance.app.financialanalysis.infrastructure.web.dto;

import java.math.BigDecimal;

public record GrowthMetricsResponse(
    BigDecimal revenueGrowth3Y,
    BigDecimal ebitdaGrowth3Y,
    BigDecimal epsGrowth3Y,
    BigDecimal fcfGrowth
) {}