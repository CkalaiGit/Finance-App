package com.cairedine.finance.app.financialanalysis.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.math.BigDecimal;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrowthMetricsEmbeddable {
    @Column(precision = 19, scale = 4)
    private BigDecimal revenueGrowth3Y;
    @Column(precision = 19, scale = 4)
    private BigDecimal ebitdaGrowth;
    @Column(precision = 19, scale = 4)
    private BigDecimal epsGrowth;
    @Column(precision = 19, scale = 4)
    private BigDecimal fcfGrowth;

    // TODO: Future add ValueMetrics and QualityMetrics
}
