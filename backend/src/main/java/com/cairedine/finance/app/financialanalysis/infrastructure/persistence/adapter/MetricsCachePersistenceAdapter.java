package com.cairedine.finance.app.financialanalysis.infrastructure.persistence.adapter;

import com.cairedine.finance.app.financialanalysis.domain.model.FullMetrics;
import com.cairedine.finance.app.financialanalysis.domain.port.IMetricsCachePort;
import com.cairedine.finance.app.financialanalysis.infrastructure.persistence.mapper.FinancialAnalysisPersistenceMapper;
import com.cairedine.finance.app.financialanalysis.infrastructure.persistence.repository.IFinancialAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MetricsCachePersistenceAdapter implements IMetricsCachePort {

    private final IFinancialAnalysisRepository repository;
    private final FinancialAnalysisPersistenceMapper mapper;

    @Override
    public List<FullMetrics> findAllByTicker(String ticker) {
        return repository.findAllByTickerOrderByPeriodEndDateDesc(ticker).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<FullMetrics> findByTickerAndPeriod(String ticker, String period) {
        return repository.findByTickerAndPeriodEndDate(ticker, period).map(mapper::toDomain);
    }

    @Override
    public void save(String ticker, FullMetrics metrics) {
        if (repository.findByTickerAndPeriodEndDate(ticker, metrics.periodEndDate()).isEmpty()) {
            repository.save(mapper.toEntity(ticker, metrics));
        }
    }
}
