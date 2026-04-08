package com.cairedine.finance.app.financialanalysis.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "financial_analysis", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"ticker", "fiscal_year_end_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialAnalysisJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ticker;

    @Embedded
    private GrowthMetricsEmbeddable growthMetrics;

    @Embedded
    private ValueMetricsEmbeddable valueMetrics;

    @Embedded
    private QualityMetricsEmbeddable qualityMetrics;

    @Column(nullable = false)
    private Instant lastUpdated;

    @Column(name = "fiscal_year_end_date", nullable = false)
    private String fiscalYearEndDate;

    @Column(nullable = false, updatable = false)
    private Instant marketDataAsOf;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = Instant.now();
    }
}
