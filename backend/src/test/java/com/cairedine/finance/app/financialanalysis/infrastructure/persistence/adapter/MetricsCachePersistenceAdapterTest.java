package com.cairedine.finance.app.financialanalysis.infrastructure.persistence.adapter;

import com.cairedine.finance.app.financialanalysis.domain.model.FullMetrics;
import com.cairedine.finance.app.financialanalysis.domain.model.GrowthMetrics;
import com.cairedine.finance.app.financialanalysis.domain.model.QualityMetrics;
import com.cairedine.finance.app.financialanalysis.domain.model.ValueMetrics;
import com.cairedine.finance.app.financialanalysis.infrastructure.persistence.entity.FinancialAnalysisJpaEntity;
import com.cairedine.finance.app.financialanalysis.infrastructure.persistence.entity.ValueMetricsEmbeddable;
import com.cairedine.finance.app.financialanalysis.infrastructure.persistence.mapper.FinancialAnalysisPersistenceMapper;
import com.cairedine.finance.app.financialanalysis.infrastructure.persistence.repository.IFinancialAnalysisRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricsCachePersistenceAdapterTest {

    @Mock
    private IFinancialAnalysisRepository repository;

    @Mock
    private FinancialAnalysisPersistenceMapper mapper;

    @InjectMocks
    private MetricsCachePersistenceAdapter adapter;

    private static final String TICKER = "MSFT";
    private static final String FISCAL_YEAR = "2023-12-31";
    private static final Instant UPDATED_AS_OF = Instant.parse("2025-01-01T12:00:00Z");

    @Test
    void shouldReturnAllFiscalYearEntriesWithoutExpiringHistoricalRows() {
        FinancialAnalysisJpaEntity historical = new FinancialAnalysisJpaEntity();
        FinancialAnalysisJpaEntity current = new FinancialAnalysisJpaEntity();
        when(repository.findAllByTickerOrderByFiscalYearEndDateDesc(TICKER))
                .thenReturn(List.of(current, historical));
        when(mapper.toDomain(current)).thenReturn(metrics(FISCAL_YEAR));
        when(mapper.toDomain(historical)).thenReturn(metrics("2022-12-31"));

        List<FullMetrics> result = adapter.findAllByTicker(TICKER);

        assertEquals(2, result.size());
        verify(mapper).toDomain(current);
        verify(mapper).toDomain(historical);
    }

    @Test
    void shouldFindExistingFiscalYearEntry() {
        FinancialAnalysisJpaEntity entity = new FinancialAnalysisJpaEntity();
        when(repository.findByTickerAndFiscalYearEndDate(TICKER, FISCAL_YEAR))
                .thenReturn(Optional.of(entity));
        FullMetrics expected = metrics(FISCAL_YEAR);
        when(mapper.toDomain(entity)).thenReturn(expected);

        assertEquals(Optional.of(expected), adapter.findByTickerAndFiscalYear(TICKER, FISCAL_YEAR));
    }

    @Test
    void shouldUpdateExistingRowInPlaceIncludingMarketDataAsOf() {
        long existingId = 42L;
        FinancialAnalysisJpaEntity existing = new FinancialAnalysisJpaEntity();
        existing.setId(existingId);
        existing.setMarketDataAsOf(Instant.parse("2024-01-01T00:00:00Z"));
        FullMetrics metrics = metrics(FISCAL_YEAR);
        FinancialAnalysisJpaEntity mapped = new FinancialAnalysisJpaEntity();
        mapped.setTicker(TICKER);
        mapped.setFiscalYearEndDate(FISCAL_YEAR);
        mapped.setMarketDataAsOf(UPDATED_AS_OF);
        mapped.setValueMetrics(ValueMetricsEmbeddable.builder()
                .peRatioTTM(new BigDecimal("25.5"))
                .build());

        when(repository.findByTickerAndFiscalYearEndDate(TICKER, FISCAL_YEAR))
                .thenReturn(Optional.of(existing));
        when(mapper.toEntity(TICKER, metrics)).thenReturn(mapped);

        adapter.save(TICKER, metrics);

        assertEquals(existingId, existing.getId());
        assertEquals(UPDATED_AS_OF, existing.getMarketDataAsOf());
        assertEquals(new BigDecimal("25.5"), existing.getValueMetrics().getPeRatioTTM());
        verify(repository).save(existing);
        verify(repository, never()).delete(any());
    }

    @Test
    void shouldInsertWhenFiscalYearEntryDoesNotExist() {
        FullMetrics metrics = metrics(FISCAL_YEAR);
        FinancialAnalysisJpaEntity newEntity = new FinancialAnalysisJpaEntity();
        when(repository.findByTickerAndFiscalYearEndDate(TICKER, FISCAL_YEAR))
                .thenReturn(Optional.empty());
        when(mapper.toEntity(TICKER, metrics)).thenReturn(newEntity);

        adapter.save(TICKER, metrics);

        verify(repository).save(newEntity);
    }

    private FullMetrics metrics(String fiscalYearEndDate) {
        return new FullMetrics(
                new GrowthMetrics(null, null, null, null),
                new ValueMetrics(null, null, null, null),
                new QualityMetrics(null, null, null, null, null),
                fiscalYearEndDate,
                MetricsCachePersistenceAdapterTest.UPDATED_AS_OF);
    }
}
