package com.cairedine.finance.app.financialanalysis.domain.model;

import java.math.BigDecimal;

public record QualityMetrics(
        BigDecimal roic,
        BigDecimal operatingMargin,
        BigDecimal netDebtToEbitda,
        BigDecimal freeCashFlowMargin,
        BigDecimal sgaToRevenue
) {}