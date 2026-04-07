package com.cairedine.finance.app.financialanalysis.infrastructure.persistence.mapper;

import com.cairedine.finance.app.financialanalysis.domain.model.FullMetrics;
import com.cairedine.finance.app.financialanalysis.domain.model.GrowthMetrics;
import com.cairedine.finance.app.financialanalysis.domain.model.QualityMetrics;
import com.cairedine.finance.app.financialanalysis.domain.model.ValueMetrics;
import com.cairedine.finance.app.financialanalysis.infrastructure.persistence.entity.FinancialAnalysisJpaEntity;
import com.cairedine.finance.app.financialanalysis.infrastructure.persistence.entity.GrowthMetricsEmbeddable;
import com.cairedine.finance.app.financialanalysis.infrastructure.persistence.entity.QualityMetricsEmbeddable;
import com.cairedine.finance.app.financialanalysis.infrastructure.persistence.entity.ValueMetricsEmbeddable;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class FinancialAnalysisPersistenceMapper {

    public FullMetrics toDomain(FinancialAnalysisJpaEntity entity) {
        if (entity == null) return null;
        return new FullMetrics(
                toGrowthDomain(entity.getGrowthMetrics()),
                toValueDomain(entity.getValueMetrics()),
                toQualityDomain(entity.getQualityMetrics()),
                entity.getPeriodEndDate()
        );
    }

    private GrowthMetrics toGrowthDomain(GrowthMetricsEmbeddable embeddable) {
        if (embeddable == null) return null;
        return new GrowthMetrics(
                embeddable.getRevenueGrowth3Y(),
                embeddable.getEbitdaGrowth(),
                embeddable.getEpsGrowth(),
                embeddable.getFcfGrowth()
        );
    }

    private ValueMetrics toValueDomain(ValueMetricsEmbeddable embeddable) {
        if (embeddable == null) return null;
        return new ValueMetrics(
                embeddable.getEvToEbit(),
                embeddable.getPeRatioTTM(),
                embeddable.getPegRatioForward(),
                embeddable.getEvToSales()
        );
    }

    private QualityMetrics toQualityDomain(QualityMetricsEmbeddable embeddable) {
        if (embeddable == null) return null;
        return new QualityMetrics(
                embeddable.getRoic(),
                embeddable.getOperatingMargin(),
                embeddable.getNetDebtToEbitda(),
                embeddable.getFreeCashFlowMargin(),
                embeddable.getSgaToRevenue()
        );
    }

    public FinancialAnalysisJpaEntity toEntity(String ticker, FullMetrics domain) {
        return FinancialAnalysisJpaEntity.builder()
                .ticker(ticker)
                .growthMetrics(toGrowthEmbeddable(domain.growth()))
                .valueMetrics(toValueEmbeddable(domain.value()))
                .qualityMetrics(toQualityEmbeddable(domain.quality()))
                .lastUpdated(Instant.now())
                .periodEndDate(domain.periodEndDate())
                .build();
    }

    private GrowthMetricsEmbeddable toGrowthEmbeddable(GrowthMetrics domain) {
        if (domain == null) return null;
        return GrowthMetricsEmbeddable.builder()
                .revenueGrowth3Y(domain.revenueGrowth3Y())
                .ebitdaGrowth(domain.ebitdaGrowth())
                .epsGrowth(domain.epsGrowth())
                .fcfGrowth(domain.fcfGrowth())
                .build();
    }

    private ValueMetricsEmbeddable toValueEmbeddable(ValueMetrics domain) {
        if (domain == null) return null;
        return ValueMetricsEmbeddable.builder()
                .evToEbit(domain.evToEbit())
                .peRatioTTM(domain.peRatioTTM())
                .pegRatioForward(domain.pegRatioForward())
                .evToSales(domain.evToSales())
                .build();
    }

    private QualityMetricsEmbeddable toQualityEmbeddable(QualityMetrics domain) {
        if (domain == null) return null;
        return QualityMetricsEmbeddable.builder()
                .roic(domain.roic())
                .operatingMargin(domain.operatingMargin())
                .netDebtToEbitda(domain.netDebtToEbitda())
                .freeCashFlowMargin(domain.freeCashFlowMargin())
                .sgaToRevenue(domain.sgaToRevenue())
                .build();
    }
}
