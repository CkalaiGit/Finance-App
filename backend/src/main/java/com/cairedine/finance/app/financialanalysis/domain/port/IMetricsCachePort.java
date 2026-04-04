package com.cairedine.finance.app.financialanalysis.domain.port;

import com.cairedine.finance.app.financialanalysis.domain.model.GrowthMetrics;
import java.util.Optional;

public interface IMetricsCachePort {
    Optional<GrowthMetrics> findByTicker(String ticker);
    void save(String ticker, GrowthMetrics metrics);
}