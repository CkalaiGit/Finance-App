package com.cairedine.finance.app.financialanalysis.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "financial_analysis")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FinancialAnalysisJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ticker;

    @Column(nullable = false)
    private String fiscalYearEndDate;

    private Instant marketDataAsOf;
    
    private Instant lastUpdated;

    @Embedded
    private GrowthMetricsEmbeddable growthMetrics;

    @Embedded
    private ValueMetricsEmbeddable valueMetrics;

    @Embedded
    private QualityMetricsEmbeddable qualityMetrics;
}