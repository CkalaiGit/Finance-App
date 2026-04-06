package com.cairedine.finance.app.financialanalysis.infrastructure.web.dto;

import java.math.BigDecimal;

public record QualityMetricsResponse(
    BigDecimal roic,
    BigDecimal operatingMargin,
    BigDecimal netDebtToEbitda,
    BigDecimal freeCashFlowMargin,
    BigDecimal sgaToRevenue
) {}
