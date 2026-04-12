package com.cairedine.finance.app.financialanalysis.infrastructure.persistence.adapter;

import com.cairedine.finance.app.financialanalysis.domain.model.FullMetrics;
import com.cairedine.finance.app.financialanalysis.infrastructure.persistence.entity.FinancialAnalysisJpaEntity;
import com.cairedine.finance.app.financialanalysis.infrastructure.persistence.mapper.FinancialAnalysisPersistenceMapper;
import com.cairedine.finance.app.financialanalysis.infrastructure.persistence.repository.IFinancialAnalysisRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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

    @Test
    void shouldFindAllByTicker() {
        // Arrange
        FinancialAnalysisJpaEntity entity = new FinancialAnalysisJpaEntity();
        FullMetrics domain = mock(FullMetrics.class);
        
        when(repository.findAllByTickerOrderByFiscalYearEndDateDesc(TICKER)).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        // Act
        List<FullMetrics> result = adapter.findAllByTicker(TICKER);

        // Assert
        assertEquals(1, result.size());
        assertEquals(domain, result.getFirst());
    }

    @Test
    void shouldSaveOnlyIfDataDoesNotExist() {
        // Arrange
        FullMetrics metrics = mock(FullMetrics.class);
        when(metrics.fiscalYearEndDate()).thenReturn("2023-12-31");
        
        // Case 1: Already exists
        when(repository.findByTickerAndFiscalYearEndDate(TICKER, "2023-12-31"))
                .thenReturn(Optional.of(new FinancialAnalysisJpaEntity()));

        // Act
        adapter.save(TICKER, metrics);

        // Assert
        verify(repository, never()).save(any());

        // Case 2: Does not exist
        reset(repository);
        when(repository.findByTickerAndFiscalYearEndDate(TICKER, "2023-12-31"))
                .thenReturn(Optional.empty());
        
        FinancialAnalysisJpaEntity entityToSave = new FinancialAnalysisJpaEntity();
        when(mapper.toEntity(TICKER, metrics)).thenReturn(entityToSave);

        // Act
        adapter.save(TICKER, metrics);

        // Assert
        verify(repository).save(entityToSave);
    }

    @Test
    void shouldDelegateFindByTickerAndFiscalYear() {
        adapter.findByTickerAndFiscalYear(TICKER, "2023");
        verify(repository).findByTickerAndFiscalYearEndDate(TICKER, "2023");
    }
}