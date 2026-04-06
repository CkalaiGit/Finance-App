package com.cairedine.finance.app.financialanalysis.infrastructure.web.dto;

public record FullMetricsResponse(
    GrowthMetricsResponse growth,
    ValueMetricsResponse value,
    QualityMetricsResponse quality
) {}
