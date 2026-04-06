package com.cairedine.finance.app.financialanalysis.domain.port;

import com.cairedine.finance.app.financialanalysis.domain.model.FullMetrics;

public interface IFinancialAnalysisService {
    FullMetrics computeMetrics(String ticker);
}
