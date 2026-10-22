package com.cairedine.finance.app.financialanalysis.domain.port;

import com.cairedine.finance.app.financialanalysis.domain.model.FullMetrics;
import java.util.List;

public interface IFinancialAnalysisService {
    List<FullMetrics> computeMetrics(String ticker);
}
