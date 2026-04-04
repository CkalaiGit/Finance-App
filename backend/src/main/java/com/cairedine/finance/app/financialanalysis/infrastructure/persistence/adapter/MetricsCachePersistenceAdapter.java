package com.cairedine.finance.app.financialanalysis.infrastructure.persistence.adapter;

import com.cairedine.finance.app.financialanalysis.domain.model.GrowthMetrics;
import com.cairedine.finance.app.financialanalysis.domain.port.IMetricsCachePort;
import com.cairedine.finance.app.financialanalysis.infrastructure.persistence.mapper.FinancialAnalysisPersistenceMapper;
import com.cairedine.finance.app.financialanalysis.infrastructure.persistence.repository.IFinancialAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MetricsCachePersistenceAdapter implements IMetricsCachePort {

    private final IFinancialAnalysisRepository repository;
    private final FinancialAnalysisPersistenceMapper mapper;

    @Override
    public Optional<GrowthMetrics> findByTicker(String ticker) {
        return repository.findByTicker(ticker).map(mapper::toDomain);
    }

    @Override
    public void save(String ticker, GrowthMetrics metrics) {
        repository.save(mapper.toEntity(ticker, metrics));
    }
}