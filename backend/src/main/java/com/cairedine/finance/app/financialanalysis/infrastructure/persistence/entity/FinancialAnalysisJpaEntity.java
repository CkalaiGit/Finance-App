package com.cairedine.finance.app.financialanalysis.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "financial_analysis", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"ticker", "periodEndDate"})
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

    @Column(name = "period_end_date", nullable = false)
    private String periodEndDate;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = Instant.now();
    }
}
