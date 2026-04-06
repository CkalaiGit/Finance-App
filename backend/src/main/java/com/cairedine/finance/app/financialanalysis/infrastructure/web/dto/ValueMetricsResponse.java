package com.cairedine.finance.app.financialanalysis.infrastructure.web.dto;

import java.math.BigDecimal;

public record ValueMetricsResponse(
    BigDecimal evToEbit,
    BigDecimal peRatioTTM,
    BigDecimal pegRatioForward,
    BigDecimal evToSales
) {}
