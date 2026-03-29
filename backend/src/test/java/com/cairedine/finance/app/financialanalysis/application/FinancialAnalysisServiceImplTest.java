package com.cairedine.finance.app.financialanalysis.application;

import com.cairedine.finance.app.financialanalysis.domain.model.GrowthMetrics;
import com.cairedine.finance.app.shared.exceptions.TickerNotFoundException;
import com.cairedine.finance.app.webclient.CashFlowRecord;
import com.cairedine.finance.app.webclient.IMarketDataPort;
import com.cairedine.finance.app.webclient.IncomeStatementRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialAnalysisServiceImplTest {

    @Mock
    private IMarketDataPort marketDataPort;

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

        when(marketDataPort.fetchIncomeStatements(TICKER, 4)).thenReturn(incomeStatements);
        when(marketDataPort.fetchCashFlowStatements(TICKER, 2)).thenReturn(cashFlowStatements);

        // Act
        GrowthMetrics metrics = financialAnalysisService.computeMetrics(TICKER);

        // Assert
        assertNotNull(metrics);
        // Revenue CAGR: (1000/700)^(1/3) - 1 ≈ 0.1262
        assertEquals(0, metrics.revenueGrowth3Y().compareTo(new BigDecimal("0.1262")));
        // EBITDA Growth: (200-180)/180 ≈ 0.1111
        assertEquals(0, metrics.ebitdaGrowth().compareTo(new BigDecimal("0.1111")));
        // EPS Growth: (10-9)/9 ≈ 0.1111
        assertEquals(0, metrics.epsGrowth().compareTo(new BigDecimal("0.1111")));
        // FCF Growth: (150-120)/120 = 0.25
        assertEquals(0, metrics.fcfGrowth().compareTo(new BigDecimal("0.25")));
    }

    @Test
    void shouldHandleNegativeValues() {
        // Arrange
        List<IncomeStatementRecord> incomeStatements = List.of(
                new IncomeStatementRecord(TICKER, "2023", new BigDecimal("1000"), null, new BigDecimal("50"), new BigDecimal("1"), null),
                new IncomeStatementRecord(TICKER, "2022", new BigDecimal("900"), null, new BigDecimal("-50"), new BigDecimal("-1"), null),
                new IncomeStatementRecord(TICKER, "2021", new BigDecimal("800"), null, new BigDecimal("160"), new BigDecimal("8"), null),
                new IncomeStatementRecord(TICKER, "2020", new BigDecimal("700"), null, new BigDecimal("140"), new BigDecimal("7"), null)
        );

        List<CashFlowRecord> cashFlowStatements = List.of(
                new CashFlowRecord(TICKER, "2023", new BigDecimal("50"), null),
                new CashFlowRecord(TICKER, "2022", new BigDecimal("-50"), null)
        );

        when(marketDataPort.fetchIncomeStatements(TICKER, 4)).thenReturn(incomeStatements);
        when(marketDataPort.fetchCashFlowStatements(TICKER, 2)).thenReturn(cashFlowStatements);

        // Act
        GrowthMetrics metrics = financialAnalysisService.computeMetrics(TICKER);

        // Assert
        // EBITDA Growth: (50 - (-50)) / |-50| = 100 / 50 = 2.0
        assertEquals(0, metrics.ebitdaGrowth().compareTo(new BigDecimal("2.0")));
        // EPS Growth: (1 - (-1)) / |-1| = 2 / 1 = 2.0
        assertEquals(0, metrics.epsGrowth().compareTo(new BigDecimal("2.0")));
        // FCF Growth: (50 - (-50)) / |-50| = 2.0
        assertEquals(0, metrics.fcfGrowth().compareTo(new BigDecimal("2.0")));
    }

    @Test
    void shouldThrowTickerNotFoundExceptionIfInsufficientData() {
        when(marketDataPort.fetchIncomeStatements(TICKER, 4)).thenReturn(List.of());
        when(marketDataPort.fetchCashFlowStatements(TICKER, 2)).thenReturn(List.of());

        assertThrows(TickerNotFoundException.class, () -> financialAnalysisService.computeMetrics(TICKER));
    }
}
