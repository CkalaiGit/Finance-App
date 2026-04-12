package com.cairedine.finance.app.financialanalysis.application;

import com.cairedine.finance.app.financialanalysis.domain.model.FullMetrics;
import com.cairedine.finance.app.financialanalysis.domain.port.IMetricsCachePort;
import com.cairedine.finance.app.shared.exceptions.TickerNotFoundException;
import com.cairedine.finance.app.webclient.*;
import org.junit.jupiter.api.DisplayName;
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
            new IncomeStatementRecord(TICKER, "2022", new BigDecimal("900"), new BigDecimal("180"), new BigDecimal("180"), new BigDecimal("9"), null),
            new IncomeStatementRecord(TICKER, "2021", new BigDecimal("800"), new BigDecimal("160"), new BigDecimal("160"), new BigDecimal("8"), null),
            new IncomeStatementRecord(TICKER, "2020", new BigDecimal("700"), new BigDecimal("140"), new BigDecimal("140"), new BigDecimal("7"), null)
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
        verify(metricsCache).save(eq(TICKER), argThat(metrics ->
                metrics.growth().revenueGrowth3Y().compareTo(new BigDecimal("0.1262")) == 0 &&
                metrics.growth().ebitdaGrowth3Y().compareTo(new BigDecimal("0.1262")) == 0 &&
                metrics.growth().epsGrowth3Y().compareTo(new BigDecimal("0.1262")) == 0
        ));
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
    @DisplayName("evToEbit doit être zéro si operatingIncome est nul")
    void shouldReturnZeroEvToEbitWhenOperatingIncomeNull() {
        // Arrange
        IncomeStatementRecord incomeWithNullEbit = new IncomeStatementRecord(TICKER, "2023", new BigDecimal("1000"), null, new BigDecimal("200"), new BigDecimal("10"), new BigDecimal("65"));
        List<IncomeStatementRecord> statements = List.of(incomeWithNullEbit, incomeStatements4.get(1), incomeStatements4.get(2), incomeStatements4.get(3));

        when(marketDataPort.fetchIncomeStatements(TICKER, 4)).thenReturn(statements);
        when(metricsCache.findByTickerAndFiscalYear(TICKER, "2023")).thenReturn(Optional.empty());
        when(marketDataPort.fetchCashFlowStatements(TICKER, 2)).thenReturn(cashFlowStatements2);
        when(marketDataPort.fetchKeyMetricsTtm(TICKER)).thenReturn(Optional.of(keyMetrics));
        when(marketDataPort.fetchAnalystEstimates(TICKER)).thenReturn(List.of());
        when(metricsCache.findAllByTicker(TICKER)).thenReturn(List.of());

        // Act
        financialAnalysisService.computeMetrics(TICKER);

        // Assert
        // EV dans keyMetrics est 3200, mais si EBIT est null, le ratio doit être 0
        verify(metricsCache).save(eq(TICKER), argThat(metrics ->
                metrics.value().evToEbit().compareTo(BigDecimal.ZERO) == 0
        ));
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

        when(metricsCache.findByTickerAndFiscalYear(TICKER, "2023")).thenReturn(Optional.of(existing));
        when(metricsCache.findAllByTicker(TICKER)).thenReturn(List.of(existing));

        // Act
        List<FullMetrics> results = financialAnalysisService.computeMetrics(TICKER);

        // Assert
        assertEquals(1, results.size());
        assertEquals(existing, results.getFirst());
        verify(metricsCache, never()).save(anyString(), any());
    }

    @Test
    @DisplayName("1. Doit lever une exception si moins de 4 rapports de revenus sont disponibles")
    void shouldThrowExceptionWhenIncomeStatementsInsufficient() {
        List<IncomeStatementRecord> insufficientIncome = incomeStatements4.subList(0, 3);
        when(marketDataPort.fetchIncomeStatements(TICKER, 4)).thenReturn(insufficientIncome);

        assertThrows(TickerNotFoundException.class, () -> financialAnalysisService.computeMetrics(TICKER));
    }

    @Test
    @DisplayName("2. Doit lever une exception si moins de 2 rapports de cash-flow sont disponibles")
    void shouldThrowExceptionWhenCashFlowStatementsInsufficient() {
        when(marketDataPort.fetchIncomeStatements(TICKER, 4)).thenReturn(incomeStatements4);
        when(marketDataPort.fetchCashFlowStatements(TICKER, 2)).thenReturn(List.of(cashFlowStatements2.getFirst()));

        assertThrows(TickerNotFoundException.class, () -> financialAnalysisService.computeMetrics(TICKER));
    }

    @Test
    @DisplayName("3. Doit initialiser les métriques à ZERO si KeyMetrics est absent")
    void shouldFallbackToZeroWhenKeyMetricsMissing() {
        when(marketDataPort.fetchIncomeStatements(TICKER, 4)).thenReturn(incomeStatements4);
        when(marketDataPort.fetchCashFlowStatements(TICKER, 2)).thenReturn(cashFlowStatements2);
        when(marketDataPort.fetchKeyMetricsTtm(TICKER)).thenReturn(Optional.empty());
        when(marketDataPort.fetchAnalystEstimates(TICKER)).thenReturn(List.of());

        financialAnalysisService.computeMetrics(TICKER);

        verify(metricsCache).save(eq(TICKER), argThat(metrics -> 
            metrics.quality().roic().compareTo(BigDecimal.ZERO) == 0 &&
            metrics.value().peRatioTTM().compareTo(BigDecimal.ZERO) == 0
        ));
    }

    @Test
    @DisplayName("4. CAGR doit utiliser la croissance simple si les valeurs de début sont négatives")
    void shouldReturnSimpleGrowthForNegativeOrZeroValues() {
        List<IncomeStatementRecord> negativeEbitdaStatements = List.of(
                new IncomeStatementRecord(TICKER, "2023", new BigDecimal("1000"), new BigDecimal("200"), new BigDecimal("200"), new BigDecimal("10"), null),
                new IncomeStatementRecord(TICKER, "2022", new BigDecimal("900"), new BigDecimal("180"), new BigDecimal("180"), new BigDecimal("9"), null),
                new IncomeStatementRecord(TICKER, "2021", new BigDecimal("800"), new BigDecimal("160"), new BigDecimal("160"), new BigDecimal("8"), null),
                new IncomeStatementRecord(TICKER, "2020", new BigDecimal("700"), new BigDecimal("140"), new BigDecimal("-50"), new BigDecimal("7"), null) // Start value (EBITDA) negative
        );

        when(marketDataPort.fetchIncomeStatements(TICKER, 4)).thenReturn(negativeEbitdaStatements);
        when(marketDataPort.fetchCashFlowStatements(TICKER, 2)).thenReturn(cashFlowStatements2);
        when(marketDataPort.fetchKeyMetricsTtm(TICKER)).thenReturn(Optional.empty());
        when(marketDataPort.fetchAnalystEstimates(TICKER)).thenReturn(List.of());

        financialAnalysisService.computeMetrics(TICKER);

        // Simple growth: (200 - (-50)) / 50 = 5.0 (500%)
        verify(metricsCache).save(eq(TICKER), argThat(metrics ->
            metrics.growth().ebitdaGrowth3Y().compareTo(new BigDecimal("5.0000")) == 0
        ));
    }

    @Test
    @DisplayName("5. PEG Ratio doit être ZERO si la croissance forward est négative ou nulle")
    void shouldReturnZeroPegWhenForwardGrowthIsNegative() {
        List<AnalystEstimateRecord> negativeEstimates = List.of(
                new AnalystEstimateRecord(TICKER, "2025", new BigDecimal("5.00"), null), // Next Year lower
                new AnalystEstimateRecord(TICKER, "2024", new BigDecimal("6.00"), null)  // Current Year higher
        );

        when(marketDataPort.fetchIncomeStatements(TICKER, 4)).thenReturn(incomeStatements4);
        when(marketDataPort.fetchCashFlowStatements(TICKER, 2)).thenReturn(cashFlowStatements2);
        when(marketDataPort.fetchKeyMetricsTtm(TICKER)).thenReturn(Optional.of(keyMetrics));
        when(marketDataPort.fetchAnalystEstimates(TICKER)).thenReturn(negativeEstimates);

        financialAnalysisService.computeMetrics(TICKER);

        verify(metricsCache).save(eq(TICKER), argThat(metrics -> 
            metrics.value().pegRatioForward().compareTo(BigDecimal.ZERO) == 0
        ));
    }

    @Test
    @DisplayName("6. Les marges doivent être ZERO si le revenu est nul (Division par zéro)")
    void shouldHandleZeroRevenueInMargins() {
        List<IncomeStatementRecord> zeroRevenueStatements = List.of(
                new IncomeStatementRecord(TICKER, "2023", BigDecimal.ZERO, new BigDecimal("100"), new BigDecimal("200"), new BigDecimal("10"), null),
                new IncomeStatementRecord(TICKER, "2022", new BigDecimal("900"), new BigDecimal("180"), new BigDecimal("180"), new BigDecimal("9"), null),
                new IncomeStatementRecord(TICKER, "2021", new BigDecimal("800"), new BigDecimal("160"), new BigDecimal("160"), new BigDecimal("8"), null),
                new IncomeStatementRecord(TICKER, "2020", new BigDecimal("700"), new BigDecimal("140"), new BigDecimal("140"), new BigDecimal("7"), null)
        );

        when(marketDataPort.fetchIncomeStatements(TICKER, 4)).thenReturn(zeroRevenueStatements);
        when(marketDataPort.fetchCashFlowStatements(TICKER, 2)).thenReturn(cashFlowStatements2);
        when(marketDataPort.fetchKeyMetricsTtm(TICKER)).thenReturn(Optional.empty());
        when(marketDataPort.fetchAnalystEstimates(TICKER)).thenReturn(List.of());

        financialAnalysisService.computeMetrics(TICKER);

        verify(metricsCache).save(eq(TICKER), argThat(metrics -> 
            metrics.quality().operatingMargin().compareTo(BigDecimal.ZERO) == 0 &&
            metrics.quality().freeCashFlowMargin().compareTo(BigDecimal.ZERO) == 0
        ));
    }
}
