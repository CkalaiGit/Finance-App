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

    @Test
    void shouldComputeGrowthMetricsCorrectly() {
        // Arrange
        List<IncomeStatementRecord> incomeStatements = List.of(
                new IncomeStatementRecord(TICKER, "2023", new BigDecimal("1000"), null, new BigDecimal("200"), new BigDecimal("10"), null),
                new IncomeStatementRecord(TICKER, "2022", new BigDecimal("900"), null, new BigDecimal("180"), new BigDecimal("9"), null),
                new IncomeStatementRecord(TICKER, "2021", new BigDecimal("800"), null, new BigDecimal("160"), new BigDecimal("8"), null),
                new IncomeStatementRecord(TICKER, "2020", new BigDecimal("700"), null, new BigDecimal("140"), new BigDecimal("7"), null)
        );

        List<CashFlowRecord> cashFlowStatements = List.of(
                new CashFlowRecord(TICKER, "2023", new BigDecimal("150"), null),
                new CashFlowRecord(TICKER, "2022", new BigDecimal("120"), null)
        );

        when(metricsCache.findByTicker(TICKER)).thenReturn(Optional.empty());
        when(marketDataPort.fetchIncomeStatements(TICKER, 4)).thenReturn(incomeStatements);
        when(marketDataPort.fetchCashFlowStatements(TICKER, 2)).thenReturn(cashFlowStatements);
        when(marketDataPort.fetchKeyMetricsTtm(TICKER)).thenReturn(Optional.empty());
        when(marketDataPort.fetchAnalystEstimates(TICKER)).thenReturn(List.of());

        // Act
        FullMetrics metrics = financialAnalysisService.computeMetrics(TICKER);

        // Assert
        assertNotNull(metrics);
        assertNotNull(metrics.growth());
        assertEquals(0, metrics.growth().revenueGrowth3Y().compareTo(new BigDecimal("0.1262")));
        assertEquals(0, metrics.growth().ebitdaGrowth().compareTo(new BigDecimal("0.1111")));
        assertEquals(0, metrics.growth().epsGrowth().compareTo(new BigDecimal("0.1111")));
        assertEquals(0, metrics.growth().fcfGrowth().compareTo(new BigDecimal("0.2500")));
    }

    @Test
    void shouldComputeValueMetricsCorrectly() {
        // Arrange
        List<IncomeStatementRecord> incomeStatements = List.of(
                new IncomeStatementRecord(TICKER, "2023", new BigDecimal("1000"), null, new BigDecimal("200"), new BigDecimal("10"), null),
                new IncomeStatementRecord(TICKER, "2022", new BigDecimal("900"), null, new BigDecimal("180"), new BigDecimal("9"), null),
                new IncomeStatementRecord(TICKER, "2021", new BigDecimal("800"), null, new BigDecimal("160"), new BigDecimal("8"), null),
                new IncomeStatementRecord(TICKER, "2020", new BigDecimal("700"), null, new BigDecimal("140"), new BigDecimal("7"), null)
        );
        List<CashFlowRecord> cashFlowStatements = List.of(
                new CashFlowRecord(TICKER, "2023", new BigDecimal("150"), null),
                new CashFlowRecord(TICKER, "2022", new BigDecimal("120"), null)
        );
        KeyMetricsRecord keyMetrics = new KeyMetricsRecord(TICKER, new BigDecimal("0.25"), new BigDecimal("1.5"), new BigDecimal("18.5"), new BigDecimal("28.4"), new BigDecimal("7.2"));

        when(metricsCache.findByTicker(TICKER)).thenReturn(Optional.empty());
        when(marketDataPort.fetchIncomeStatements(TICKER, 4)).thenReturn(incomeStatements);
        when(marketDataPort.fetchCashFlowStatements(TICKER, 2)).thenReturn(cashFlowStatements);
        when(marketDataPort.fetchKeyMetricsTtm(TICKER)).thenReturn(Optional.of(keyMetrics));
        when(marketDataPort.fetchAnalystEstimates(TICKER)).thenReturn(List.of());

        // Act
        FullMetrics metrics = financialAnalysisService.computeMetrics(TICKER);

        // Assert
        assertNotNull(metrics.value());
        assertEquals(0, metrics.value().evToEbit().compareTo(new BigDecimal("18.5000")));
        assertEquals(0, metrics.value().peRatioTTM().compareTo(new BigDecimal("28.4000")));
        assertEquals(0, metrics.value().evToSales().compareTo(new BigDecimal("7.2000")));
        // epsGrowth = 0.1111, pe = 28.4 -> PEG = 28.4 / (0.1111 * 100) = 2.5563
        assertEquals(0, metrics.value().pegRatioForward().compareTo(new BigDecimal("2.5563")));
    }

    @Test
    void shouldComputeQualityMetricsCorrectly() {
        // Arrange
        List<IncomeStatementRecord> incomeStatements = List.of(
                new IncomeStatementRecord(TICKER, "2023", new BigDecimal("1000"), new BigDecimal("298"), new BigDecimal("350"), new BigDecimal("10"), new BigDecimal("65")),
                new IncomeStatementRecord(TICKER, "2022", new BigDecimal("900"), null, new BigDecimal("300"), new BigDecimal("9"), null),
                new IncomeStatementRecord(TICKER, "2021", new BigDecimal("800"), null, new BigDecimal("250"), new BigDecimal("8"), null),
                new IncomeStatementRecord(TICKER, "2020", new BigDecimal("700"), null, new BigDecimal("200"), new BigDecimal("7"), null)
        );
        List<CashFlowRecord> cashFlowStatements = List.of(
                new CashFlowRecord(TICKER, "2023", new BigDecimal("142"), null),
                new CashFlowRecord(TICKER, "2022", new BigDecimal("120"), null)
        );
        KeyMetricsRecord keyMetrics = new KeyMetricsRecord(TICKER, new BigDecimal("0.25"), new BigDecimal("1.5"), new BigDecimal("18.5"), new BigDecimal("28.4"), new BigDecimal("7.2"));

        when(metricsCache.findByTicker(TICKER)).thenReturn(Optional.empty());
        when(marketDataPort.fetchIncomeStatements(TICKER, 4)).thenReturn(incomeStatements);
        when(marketDataPort.fetchCashFlowStatements(TICKER, 2)).thenReturn(cashFlowStatements);
        when(marketDataPort.fetchKeyMetricsTtm(TICKER)).thenReturn(Optional.of(keyMetrics));
        when(marketDataPort.fetchAnalystEstimates(TICKER)).thenReturn(List.of());

        // Act
        FullMetrics metrics = financialAnalysisService.computeMetrics(TICKER);

        // Assert
        assertNotNull(metrics.quality());
        assertEquals(0, metrics.quality().roic().compareTo(new BigDecimal("0.2500")));
        assertEquals(0, metrics.quality().netDebtToEbitda().compareTo(new BigDecimal("1.5000")));
        assertEquals(0, metrics.quality().operatingMargin().compareTo(new BigDecimal("0.2980")));
        assertEquals(0, metrics.quality().freeCashFlowMargin().compareTo(new BigDecimal("0.1420")));
        assertEquals(0, metrics.quality().sgaToRevenue().compareTo(new BigDecimal("0.0650")));
    }

    @Test
    void shouldReturnZeroQualityMetricsWhenKeyMetricsAbsent() {
        // Arrange
        List<IncomeStatementRecord> incomeStatements = List.of(
                new IncomeStatementRecord(TICKER, "2023", new BigDecimal("1000"), new BigDecimal("298"), new BigDecimal("350"), new BigDecimal("10"), new BigDecimal("65")),
                new IncomeStatementRecord(TICKER, "2022", new BigDecimal("900"), null, new BigDecimal("300"), new BigDecimal("9"), null),
                new IncomeStatementRecord(TICKER, "2021", new BigDecimal("800"), null, new BigDecimal("250"), new BigDecimal("8"), null),
                new IncomeStatementRecord(TICKER, "2020", new BigDecimal("700"), null, new BigDecimal("200"), new BigDecimal("7"), null)
        );
        List<CashFlowRecord> cashFlowStatements = List.of(
                new CashFlowRecord(TICKER, "2023", new BigDecimal("142"), null),
                new CashFlowRecord(TICKER, "2022", new BigDecimal("120"), null)
        );

        when(metricsCache.findByTicker(TICKER)).thenReturn(Optional.empty());
        when(marketDataPort.fetchIncomeStatements(TICKER, 4)).thenReturn(incomeStatements);
        when(marketDataPort.fetchCashFlowStatements(TICKER, 2)).thenReturn(cashFlowStatements);
        when(marketDataPort.fetchKeyMetricsTtm(TICKER)).thenReturn(Optional.empty());
        when(marketDataPort.fetchAnalystEstimates(TICKER)).thenReturn(List.of());

        // Act
        FullMetrics metrics = financialAnalysisService.computeMetrics(TICKER);

        // Assert
        assertEquals(0, metrics.quality().roic().compareTo(BigDecimal.ZERO));
        assertEquals(0, metrics.quality().netDebtToEbitda().compareTo(BigDecimal.ZERO));
        assertEquals(0, metrics.value().evToEbit().compareTo(BigDecimal.ZERO));
    }

    @Test
    void shouldThrowTickerNotFoundExceptionIfInsufficientData() {
        when(metricsCache.findByTicker(TICKER)).thenReturn(Optional.empty());
        when(marketDataPort.fetchIncomeStatements(TICKER, 4)).thenReturn(List.of());
        when(marketDataPort.fetchCashFlowStatements(TICKER, 2)).thenReturn(List.of());

        assertThrows(TickerNotFoundException.class, () -> financialAnalysisService.computeMetrics(TICKER));
    }

    @Test
    void shouldReturnCachedMetricsIfAvailable() {
        // Arrange
        FullMetrics cachedMetrics = mock(FullMetrics.class);
        when(metricsCache.findByTicker(TICKER)).thenReturn(Optional.of(cachedMetrics));

        // Act
        FullMetrics result = financialAnalysisService.computeMetrics(TICKER);

        // Assert
        assertEquals(cachedMetrics, result);
        verifyNoInteractions(marketDataPort);
    }
}
