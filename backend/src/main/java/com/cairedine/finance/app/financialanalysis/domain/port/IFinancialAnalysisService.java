package com.cairedine.finance.app.financialanalysis.domain.port;

import com.cairedine.finance.app.financialanalysis.domain.model.GrowthMetrics;

public interface IFinancialAnalysisService {
    GrowthMetrics computeMetrics(String ticker);
}
