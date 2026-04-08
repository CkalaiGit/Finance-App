package com.cairedine.finance.app.financialanalysis.application;

import com.cairedine.finance.app.financialanalysis.domain.model.FullMetrics;
import com.cairedine.finance.app.financialanalysis.domain.port.IMetricsCachePort;
import com.cairedine.finance.app.shared.exceptions.TickerNotFoundException;
import com.cairedine.finance.app.webclient.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancialAnalysisServiceImplTest {

    @Mock
    private IMarketDataPort marketDataPort;

    @Mock
    private IMetricsCachePort metricsCache;

    @InjectMocks
    private FinancialAnalysisServiceImpl financialAnalysisService;

    private static final String TICKER = "AAPL";

    private final List<IncomeStatementRecord> incomeStatements4 = List.of(
            new IncomeStatementRecord(TICKER, "2023", new BigDecimal("1000"), new BigDecimal("200"), new BigDecimal("200"), new BigDecimal("10"), new BigDecimal("65")),
            new IncomeStatementRecord(TICKER, "2022", new BigDecimal("900"), null, new BigDecimal("180"), new BigDecimal("9"), null),
            new IncomeStatementRecord(TICKER, "2021", new BigDecimal("800"), null, new BigDecimal("160"), new BigDecimal("8"), null),
            new IncomeStatementRecord(TICKER, "2020", new BigDecimal("700"), null, new BigDecimal("140"), new BigDecimal("7"), null)
    );

    private final List<CashFlowRecord> cashFlowStatements2 = List.of(
            new CashFlowRecord(TICKER, "2023", new BigDecimal("150"), null),
            new CashFlowRecord(TICKER, "2022", new BigDecimal("120"), null)
    );

    private final KeyMetricsRecord keyMetrics = new KeyMetricsRecord(TICKER, new BigDecimal("0.25"), new BigDecimal("1.5"), new BigDecimal("3200"), new BigDecimal("28.4"), new BigDecimal("7.2"));

    @Test
    void shouldComputeGrowthMetricsCorrectly() {
        // Arrange
        when(marketDataPort.fetchIncomeStatements(TICKER, 4)).thenReturn(incomeStatements4);
        when(metricsCache.findByTickerAndFiscalYear(TICKER, "2023")).thenReturn(Optional.empty());
        when(marketDataPort.fetchCashFlowStatements(TICKER, 2)).thenReturn(cashFlowStatements2);
        when(marketDataPort.fetchKeyMetricsTtm(TICKER)).thenReturn(Optional.empty());
        when(marketDataPort.fetchAnalystEstimates(TICKER)).thenReturn(List.of());
        
        FullMetrics calculated = mock(FullMetrics.class);
        when(metricsCache.findAllByTicker(TICKER)).thenReturn(List.of(calculated));

        // Act
        List<FullMetrics> results = financialAnalysisService.computeMetrics(TICKER);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(metricsCache).save(eq(TICKER), any(FullMetrics.class));
    }

    @Test
    void shouldComputeValueMetricsCorrectly() {
        // Arrange
        List<AnalystEstimateRecord> analystEstimates = List.of(
                new AnalystEstimateRecord("AAPL", "2025", new BigDecimal("7.50"), null),
                new AnalystEstimateRecord("AAPL", "2024", new BigDecimal("6.80"), null)
        );

        when(marketDataPort.fetchIncomeStatements(TICKER, 4)).thenReturn(incomeStatements4);
        when(metricsCache.findByTickerAndFiscalYear(TICKER, "2023")).thenReturn(Optional.empty());
        when(marketDataPort.fetchCashFlowStatements(TICKER, 2)).thenReturn(cashFlowStatements2);
        when(marketDataPort.fetchKeyMetricsTtm(TICKER)).thenReturn(Optional.of(keyMetrics));
        when(marketDataPort.fetchAnalystEstimates(TICKER)).thenReturn(analystEstimates);

        // Act
        financialAnalysisService.computeMetrics(TICKER);

        // Assert
        verify(metricsCache).save(eq(TICKER), argThat(metrics -> {
            // EV = 3200, EBIT = 200 -> 16.0
            return metrics.value().evToEbit().compareTo(new BigDecimal("16.0000")) == 0 &&
                   metrics.value().pegRatioForward().compareTo(new BigDecimal("2.7600")) == 0;
        }));
    }

    @Test
    void shouldComputeQualityMetricsCorrectly() {
        // Arrange
        when(marketDataPort.fetchIncomeStatements(TICKER, 4)).thenReturn(List.of(
                new IncomeStatementRecord(TICKER, "2023", new BigDecimal("1000"), new BigDecimal("298"), new BigDecimal("350"), new BigDecimal("10"), new BigDecimal("65")),
                new IncomeStatementRecord(TICKER, "2022", new BigDecimal("900"), null, new BigDecimal("300"), new BigDecimal("9"), null),
                new IncomeStatementRecord(TICKER, "2021", new BigDecimal("800"), null, new BigDecimal("160"), new BigDecimal("8"), null),
                new IncomeStatementRecord(TICKER, "2020", new BigDecimal("700"), null, new BigDecimal("140"), new BigDecimal("7"), null)
        ));
        when(metricsCache.findByTickerAndFiscalYear(TICKER, "2023")).thenReturn(Optional.empty());
        when(marketDataPort.fetchCashFlowStatements(TICKER, 2)).thenReturn(List.of(
                new CashFlowRecord(TICKER, "2023", new BigDecimal("142"), null),
                new CashFlowRecord(TICKER, "2022", new BigDecimal("120"), null)
        ));
        when(marketDataPort.fetchKeyMetricsTtm(TICKER)).thenReturn(Optional.of(keyMetrics));
        when(marketDataPort.fetchAnalystEstimates(TICKER)).thenReturn(List.of());

        // Act
        financialAnalysisService.computeMetrics(TICKER);

        // Assert
        verify(metricsCache).save(eq(TICKER), argThat(metrics -> 
            metrics.quality().operatingMargin().compareTo(new BigDecimal("0.2980")) == 0 &&
            metrics.quality().freeCashFlowMargin().compareTo(new BigDecimal("0.1420")) == 0
        ));
    }

    @Test
    void shouldThrowTickerNotFoundExceptionIfIncomeEmpty() {
        when(marketDataPort.fetchIncomeStatements(TICKER, 4)).thenReturn(List.of());

        assertThrows(TickerNotFoundException.class, () -> financialAnalysisService.computeMetrics(TICKER));
    }

    @Test
    void shouldReturnCachedMetricsIfPeriodMatches() {
        // Arrange
        when(marketDataPort.fetchIncomeStatements(TICKER, 4)).thenReturn(incomeStatements4);
        FullMetrics existing = mock(FullMetrics.class);
        //when(existing.fiscalYearEndDate()).thenReturn("2023");

        when(metricsCache.findByTickerAndFiscalYear(TICKER, "2023")).thenReturn(Optional.of(existing));
        when(metricsCache.findAllByTicker(TICKER)).thenReturn(List.of(existing));

        // Act
        List<FullMetrics> results = financialAnalysisService.computeMetrics(TICKER);

        // Assert
        assertEquals(1, results.size());
        assertEquals(existing, results.getFirst());
        verify(metricsCache, never()).save(anyString(), any());
    }
}
