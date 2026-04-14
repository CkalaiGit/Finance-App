package com.cairedine.finance.app.financialanalysis.infrastructure.persistence.adapter;

import com.cairedine.finance.app.financialanalysis.domain.model.FullMetrics;
import com.cairedine.finance.app.financialanalysis.domain.port.IMetricsCachePort;
import com.cairedine.finance.app.financialanalysis.infrastructure.persistence.entity.FinancialAnalysisJpaEntity;
import com.cairedine.finance.app.financialanalysis.infrastructure.persistence.mapper.FinancialAnalysisPersistenceMapper;
import com.cairedine.finance.app.financialanalysis.infrastructure.persistence.repository.IFinancialAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
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
                .filter(entity -> !entity.isExpired())
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<FullMetrics> findByTickerAndFiscalYear(String ticker, String fiscalYearEndDate) {
        return repository.findByTickerAndFiscalYearEndDate(ticker, fiscalYearEndDate)
                .filter(entity -> !entity.isExpired())
                .map(mapper::toDomain);
    }

    @Override
    public void save(String ticker, FullMetrics metrics) {
        repository.findByTickerAndFiscalYearEndDate(ticker, metrics.fiscalYearEndDate())
                .ifPresentOrElse(
                    existing -> {
                        if (existing.isExpired()) {
                            repository.delete(existing);
                            saveNewEntry(ticker, metrics);
                        }
                    },
                    () -> saveNewEntry(ticker, metrics)
                );
    }

    private void saveNewEntry(String ticker, FullMetrics metrics) {
        FinancialAnalysisJpaEntity entity = mapper.toEntity(ticker, metrics);
        entity.setExpiresAt(Instant.now().plus(Duration.ofHours(24)));
        repository.save(entity);
    }
}
