package com.cairedine.finance.app.financialanalysis.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "financial_analysis", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"ticker", "fiscalYearEndDate"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FinancialAnalysisJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ticker;

    @Column(nullable = false)
    private String fiscalYearEndDate;

    @Column(nullable = false)
    private Instant marketDataAsOf;

    @Column
    private Instant lastUpdated;

    @Column
    private Instant expiresAt;

    @Embedded
    private GrowthMetricsEmbeddable growthMetrics;

    @Embedded
    private ValueMetricsEmbeddable valueMetrics;

    @Embedded
    private QualityMetricsEmbeddable qualityMetrics;

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = Instant.now();
    }
}
