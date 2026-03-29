package com.cairedine.finance.app.financialanalysis.infrastructure.web.mapper;

import com.cairedine.finance.app.financialanalysis.domain.model.GrowthMetrics;
import com.cairedine.finance.app.financialanalysis.infrastructure.web.dto.GrowthMetricsResponse;
import org.springframework.stereotype.Component;

@Component
public class FinancialAnalysisWebMapper {

    public GrowthMetricsResponse toResponse(GrowthMetrics domain) {
        return new GrowthMetricsResponse(
                domain.revenueGrowth3Y(),
                domain.ebitdaGrowth(),
                domain.epsGrowth(),
                domain.fcfGrowth()
        );
    }
}
