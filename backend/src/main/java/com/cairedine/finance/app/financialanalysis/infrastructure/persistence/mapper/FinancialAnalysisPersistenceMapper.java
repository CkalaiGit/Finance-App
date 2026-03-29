package com.cairedine.finance.app.financialanalysis.infrastructure.persistence.mapper;

import com.cairedine.finance.app.financialanalysis.domain.model.GrowthMetrics;
import com.cairedine.finance.app.financialanalysis.infrastructure.persistence.entity.FinancialAnalysisJpaEntity;
import com.cairedine.finance.app.financialanalysis.infrastructure.persistence.entity.GrowthMetricsEmbeddable;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class FinancialAnalysisPersistenceMapper {

    public GrowthMetrics toDomain(FinancialAnalysisJpaEntity entity) {
        if (entity == null || entity.getGrowthMetrics() == null) return null;
        var metrics = entity.getGrowthMetrics();
        return new GrowthMetrics(
                metrics.getRevenueGrowth3Y(),
                metrics.getEbitdaGrowth(),
                metrics.getEpsGrowth(),
                metrics.getFcfGrowth()
        );
    }

    public FinancialAnalysisJpaEntity toEntity(String ticker, GrowthMetrics domain) {
        return FinancialAnalysisJpaEntity.builder()
                .ticker(ticker)
                .growthMetrics(toEmbeddable(domain))
                .lastUpdated(Instant.now())
                .build();
    }

    private GrowthMetricsEmbeddable toEmbeddable(GrowthMetrics domain) {
        return GrowthMetricsEmbeddable.builder()
                .revenueGrowth3Y(domain.revenueGrowth3Y())
                .ebitdaGrowth(domain.ebitdaGrowth())
                .epsGrowth(domain.epsGrowth())
                .fcfGrowth(domain.fcfGrowth())
                .build();
    }
}
