package com.cairedine.finance.app.financialanalysis.domain.model;

public record FullMetrics(
    GrowthMetrics growth,
    ValueMetrics value,
    QualityMetrics quality
) {}
