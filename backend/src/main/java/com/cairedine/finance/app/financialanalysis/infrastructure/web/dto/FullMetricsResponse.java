package com.cairedine.finance.app.financialanalysis.infrastructure.web.dto;

import java.time.Instant;

public record FullMetricsResponse(
    GrowthMetricsResponse growth,
    ValueMetricsResponse value,
    QualityMetricsResponse quality,
    String fiscalYearEndDate,
    Instant marketDataAsOf
) {}
