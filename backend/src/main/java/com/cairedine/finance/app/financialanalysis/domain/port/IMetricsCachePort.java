package com.cairedine.finance.app.financialanalysis.domain.port;

import com.cairedine.finance.app.financialanalysis.domain.model.FullMetrics;
import java.util.List;
import java.util.Optional;

public interface IMetricsCachePort {
    List<FullMetrics> findAllByTicker(String ticker);
    Optional<FullMetrics> findByTickerAndFiscalYear(String ticker, String fiscalYearEndDate);
    void save(String ticker, FullMetrics metrics);
}
