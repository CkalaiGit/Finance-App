package com.cairedine.finance.app.financialanalysis.domain.port;

import com.cairedine.finance.app.financialanalysis.domain.model.FullMetrics;
import java.util.Optional;

public interface IMetricsCachePort {
    Optional<FullMetrics> findByTicker(String ticker);
    void save(String ticker, FullMetrics metrics);
}
