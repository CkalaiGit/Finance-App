package com.cairedine.finance.app.financialanalysis.infrastructure.web.mapper;

import com.cairedine.finance.app.financialanalysis.domain.model.FullMetrics;
import com.cairedine.finance.app.financialanalysis.domain.model.GrowthMetrics;
import com.cairedine.finance.app.financialanalysis.domain.model.QualityMetrics;
import com.cairedine.finance.app.financialanalysis.domain.model.ValueMetrics;
import com.cairedine.finance.app.financialanalysis.infrastructure.web.dto.FullMetricsResponse;
import com.cairedine.finance.app.financialanalysis.infrastructure.web.dto.GrowthMetricsResponse;
import com.cairedine.finance.app.financialanalysis.infrastructure.web.dto.QualityMetricsResponse;
import com.cairedine.finance.app.financialanalysis.infrastructure.web.dto.ValueMetricsResponse;
import org.springframework.stereotype.Component;

@Component
public class FinancialAnalysisWebMapper {

    public FullMetricsResponse toResponse(FullMetrics domain) {
        if (domain == null) return null;
        return new FullMetricsResponse(
                toGrowthResponse(domain.growth()),
                toValueResponse(domain.value()),
                toQualityResponse(domain.quality()),
                domain.fiscalYearEndDate(),
                domain.marketDataAsOf()
        );
    }

    private GrowthMetricsResponse toGrowthResponse(GrowthMetrics domain) {
        if (domain == null) return null;
        return new GrowthMetricsResponse(
                domain.revenueGrowth3Y(),
                domain.ebitdaGrowth3Y(),
                domain.epsGrowth3Y(),
                domain.fcfGrowth()
        );
    }

    private ValueMetricsResponse toValueResponse(ValueMetrics domain) {
        if (domain == null) return null;
        return new ValueMetricsResponse(
                domain.evToEbit(),
                domain.peRatioTTM(),
                domain.pegRatioForward(),
                domain.evToSales()
        );
    }

    private QualityMetricsResponse toQualityResponse(QualityMetrics domain) {
        if (domain == null) return null;
        return new QualityMetricsResponse(
                domain.roic(),
                domain.operatingMargin(),
                domain.netDebtToEbitda(),
                domain.freeCashFlowMargin(),
                domain.sgaToRevenue()
        );
    }
}
