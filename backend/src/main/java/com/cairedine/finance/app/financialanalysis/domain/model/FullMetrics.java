package com.cairedine.finance.app.financialanalysis.domain.model;

import java.time.Instant;

public record FullMetrics(
    GrowthMetrics growth,
    ValueMetrics value,
    QualityMetrics quality,
    String fiscalYearEndDate,
    Instant marketDataAsOf
) {}
