package com.cairedine.finance.app.financialanalysis.infrastructure.persistence.adapter;

import com.cairedine.finance.app.financialanalysis.domain.model.FullMetrics;
import com.cairedine.finance.app.financialanalysis.domain.port.IMetricsCachePort;
import com.cairedine.finance.app.financialanalysis.infrastructure.persistence.entity.FinancialAnalysisJpaEntity;
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
        return repository.findAllByTickerOrderByFiscalYearEndDateDesc(ticker).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<FullMetrics> findByTickerAndFiscalYear(String ticker, String fiscalYearEndDate) {
        return repository.findByTickerAndFiscalYearEndDate(ticker, fiscalYearEndDate)
                .map(mapper::toDomain);
    }

    @Override
    public void save(String ticker, FullMetrics metrics) {
        repository.findByTickerAndFiscalYearEndDate(ticker, metrics.fiscalYearEndDate())
                .ifPresentOrElse(existing -> updateEntry(existing, ticker, metrics),
                        () -> repository.save(mapper.toEntity(ticker, metrics)));
    }

    private void updateEntry(FinancialAnalysisJpaEntity existing, String ticker, FullMetrics metrics) {
        FinancialAnalysisJpaEntity updated = mapper.toEntity(ticker, metrics);
        existing.setTicker(updated.getTicker());
        existing.setFiscalYearEndDate(updated.getFiscalYearEndDate());
        existing.setMarketDataAsOf(updated.getMarketDataAsOf());
        existing.setGrowthMetrics(updated.getGrowthMetrics());
        existing.setValueMetrics(updated.getValueMetrics());
        existing.setQualityMetrics(updated.getQualityMetrics());
        repository.save(existing);
    }
}
