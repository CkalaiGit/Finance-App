package com.cairedine.finance.app.financialanalysis.application;

import com.cairedine.finance.app.financialanalysis.domain.model.FullMetrics;
import com.cairedine.finance.app.financialanalysis.domain.model.GrowthMetrics;
import com.cairedine.finance.app.financialanalysis.domain.model.QualityMetrics;
import com.cairedine.finance.app.financialanalysis.domain.model.ValueMetrics;
import com.cairedine.finance.app.financialanalysis.domain.port.IMetricsCachePort;
import com.cairedine.finance.app.shared.exceptions.MarketDataUnavailableException;
import com.cairedine.finance.app.shared.exceptions.TickerNotFoundException;
import com.cairedine.finance.app.webclient.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
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

    private FinancialAnalysisServiceImpl financialAnalysisService;

    private static final String TICKER = "AAPL";
    private static final Instant NOW = Instant.parse("2025-01-01T12:00:00Z");
    private static final String FY_END = "2023-12-31";

    @BeforeEach
    void setUp() {
        financialAnalysisService = new FinancialAnalysisServiceImpl(
                marketDataPort, metricsCache, Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofHours(24));
    }

    private final List<IncomeStatementRecord> incomeStatements4 = List.of(
            new IncomeStatementRecord(TICKER, FY_END, new BigDecimal("1000"), new BigDecimal("200"), new BigDecimal("200"), new BigDecimal("10"), new BigDecimal("65")),
            new IncomeStatementRecord(TICKER, "2022", new BigDecimal("900"), new BigDecimal("180"), new BigDecimal("180"), new BigDecimal("9"), null),
            new IncomeStatementRecord(TICKER, "2021", new BigDecimal("800"), new BigDecimal("160"), new BigDecimal("160"), new BigDecimal("8"), null),
            new IncomeStatementRecord(TICKER, "2020", new BigDecimal("700"), new BigDecimal("140"), new BigDecimal("140"), new BigDecimal("7"), null)
    );

    private final List<CashFlowRecord> cashFlowStatements2 = List.of(
            new CashFlowRecord(TICKER, FY_END, new BigDecimal("150"), null),
            new CashFlowRecord(TICKER, "2022", new BigDecimal("120"), null)
    );

    private final KeyMetricsRecord keyMetrics = new KeyMetricsRecord(TICKER, new BigDecimal("0.25"), new BigDecimal("1.5"), new BigDecimal("3200"), new BigDecimal("28.4"), new BigDecimal("7.2"));

    @Test
    void shouldComputeGrowthMetricsCorrectly() {
        // Arrange
        when(marketDataPort.fetchIncomeStatements(TICKER, 4)).thenReturn(incomeStatements4);
        when(metricsCache.findByTickerAndFiscalYear(TICKER, FY_END)).thenReturn(Optional.empty());
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
        // Ordre FMP : de la plus lointaine à la plus ancienne. Seuls l'exercice clos (2023)
        // et le suivant (2024) doivent être comparés : 7.50 / 6.80 -> +10.29 %.
        List<AnalystEstimateRecord> analystEstimates = List.of(
                new AnalystEstimateRecord("AAPL", "2026-12-31", new BigDecimal("20.00"), null),
                new AnalystEstimateRecord("AAPL", "2025-12-31", new BigDecimal("15.00"), null),
                new AnalystEstimateRecord("AAPL", "2024-12-31", new BigDecimal("7.50"), null),
                new AnalystEstimateRecord("AAPL", "2023-12-31", new BigDecimal("6.80"), null),
                new AnalystEstimateRecord("AAPL", "2022-12-31", new BigDecimal("3.00"), null)
        );

        when(marketDataPort.fetchIncomeStatements(TICKER, 4)).thenReturn(incomeStatements4);
        when(metricsCache.findByTickerAndFiscalYear(TICKER, FY_END)).thenReturn(Optional.empty());
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
    @DisplayName("evToEbit doit être null si operatingIncome est absent")
    void shouldReturnNullEvToEbitWhenOperatingIncomeNull() {
        // Arrange
        IncomeStatementRecord incomeWithNullEbit = new IncomeStatementRecord(TICKER, FY_END, new BigDecimal("1000"), null, new BigDecimal("200"), new BigDecimal("10"), new BigDecimal("65"));
        List<IncomeStatementRecord> statements = List.of(incomeWithNullEbit, incomeStatements4.get(1), incomeStatements4.get(2), incomeStatements4.get(3));

        when(marketDataPort.fetchIncomeStatements(TICKER, 4)).thenReturn(statements);
        when(metricsCache.findByTickerAndFiscalYear(TICKER, FY_END)).thenReturn(Optional.empty());
        when(marketDataPort.fetchCashFlowStatements(TICKER, 2)).thenReturn(cashFlowStatements2);
        when(marketDataPort.fetchKeyMetricsTtm(TICKER)).thenReturn(Optional.of(keyMetrics));
        when(marketDataPort.fetchAnalystEstimates(TICKER)).thenReturn(List.of());
        when(metricsCache.findAllByTicker(TICKER)).thenReturn(List.of());

        // Act
        financialAnalysisService.computeMetrics(TICKER);

        // Assert
        // EV dans keyMetrics est 3200, mais si EBIT est null, le ratio doit être null.
        verify(metricsCache).save(eq(TICKER), argThat(metrics ->
                metrics.value().evToEbit() == null
        ));
    }

    @Test
    void shouldComputeQualityMetricsCorrectly() {
        // Arrange
        when(marketDataPort.fetchIncomeStatements(TICKER, 4)).thenReturn(List.of(
                new IncomeStatementRecord(TICKER, FY_END, new BigDecimal("1000"), new BigDecimal("298"), new BigDecimal("350"), new BigDecimal("10"), new BigDecimal("65")),
                new IncomeStatementRecord(TICKER, "2022", new BigDecimal("900"), null, new BigDecimal("300"), new BigDecimal("9"), null),
                new IncomeStatementRecord(TICKER, "2021", new BigDecimal("800"), null, new BigDecimal("160"), new BigDecimal("8"), null),
                new IncomeStatementRecord(TICKER, "2020", new BigDecimal("700"), null, new BigDecimal("140"), new BigDecimal("7"), null)
        ));
        when(metricsCache.findByTickerAndFiscalYear(TICKER, FY_END)).thenReturn(Optional.empty());
        when(marketDataPort.fetchCashFlowStatements(TICKER, 2)).thenReturn(List.of(
                new CashFlowRecord(TICKER, FY_END, new BigDecimal("142"), null),
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
        FullMetrics existing = metricsAsOf(NOW.minus(Duration.ofHours(1)));

        when(metricsCache.findByTickerAndFiscalYear(TICKER, FY_END)).thenReturn(Optional.of(existing));
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
    @DisplayName("3. Les métriques dépendant de KeyMetrics restent nulles si les données manquent")
    void shouldKeepKeyMetricsNullWhenMissing() {
        when(marketDataPort.fetchIncomeStatements(TICKER, 4)).thenReturn(incomeStatements4);
        when(marketDataPort.fetchCashFlowStatements(TICKER, 2)).thenReturn(cashFlowStatements2);
        when(marketDataPort.fetchKeyMetricsTtm(TICKER)).thenReturn(Optional.empty());
        when(marketDataPort.fetchAnalystEstimates(TICKER)).thenReturn(List.of());

        financialAnalysisService.computeMetrics(TICKER);

        verify(metricsCache).save(eq(TICKER), argThat(metrics -> 
            metrics.quality().roic() == null &&
            metrics.quality().netDebtToEbitda() == null &&
            metrics.value().evToEbit() == null &&
            metrics.value().peRatioTTM() == null &&
            metrics.value().evToSales() == null &&
            metrics.value().pegRatioForward() == null
        ));
    }

    @Test
    @DisplayName("4. CAGR est nul si la valeur de base est négative")
    void shouldReturnNullCagrForNegativeBaseValue() {
        List<IncomeStatementRecord> negativeEbitdaStatements = List.of(
                new IncomeStatementRecord(TICKER, FY_END, new BigDecimal("1000"), new BigDecimal("200"), new BigDecimal("200"), new BigDecimal("10"), null),
                new IncomeStatementRecord(TICKER, "2022", new BigDecimal("900"), new BigDecimal("180"), new BigDecimal("180"), new BigDecimal("9"), null),
                new IncomeStatementRecord(TICKER, "2021", new BigDecimal("800"), new BigDecimal("160"), new BigDecimal("160"), new BigDecimal("8"), null),
                new IncomeStatementRecord(TICKER, "2020", new BigDecimal("700"), new BigDecimal("140"), new BigDecimal("-50"), new BigDecimal("7"), null) // Start value (EBITDA) negative
        );

        when(marketDataPort.fetchIncomeStatements(TICKER, 4)).thenReturn(negativeEbitdaStatements);
        when(marketDataPort.fetchCashFlowStatements(TICKER, 2)).thenReturn(cashFlowStatements2);
        when(marketDataPort.fetchKeyMetricsTtm(TICKER)).thenReturn(Optional.empty());
        when(marketDataPort.fetchAnalystEstimates(TICKER)).thenReturn(List.of());

        financialAnalysisService.computeMetrics(TICKER);

        verify(metricsCache).save(eq(TICKER), argThat(metrics ->
            metrics.growth().ebitdaGrowth3Y() == null
        ));
    }

    @Test
    @DisplayName("5. PEG Ratio est null si la croissance forward est négative ou nulle")
    void shouldReturnNullPegWhenForwardGrowthIsNegative() {
        List<AnalystEstimateRecord> negativeEstimates = List.of(
                new AnalystEstimateRecord(TICKER, "2024-12-31", new BigDecimal("5.00"), null), // Next Year lower
                new AnalystEstimateRecord(TICKER, "2023-12-31", new BigDecimal("6.00"), null)  // Closed Year higher
        );

        when(marketDataPort.fetchIncomeStatements(TICKER, 4)).thenReturn(incomeStatements4);
        when(marketDataPort.fetchCashFlowStatements(TICKER, 2)).thenReturn(cashFlowStatements2);
        when(marketDataPort.fetchKeyMetricsTtm(TICKER)).thenReturn(Optional.of(keyMetrics));
        when(marketDataPort.fetchAnalystEstimates(TICKER)).thenReturn(negativeEstimates);

        financialAnalysisService.computeMetrics(TICKER);

        verify(metricsCache).save(eq(TICKER), argThat(metrics -> 
            metrics.value().pegRatioForward() == null
        ));
    }

    @Test
    @DisplayName("6. Les marges sont nulles si le revenu est nul (division impossible)")
    void shouldHandleZeroRevenueInMargins() {
        List<IncomeStatementRecord> zeroRevenueStatements = List.of(
                new IncomeStatementRecord(TICKER, FY_END, BigDecimal.ZERO, new BigDecimal("100"), new BigDecimal("200"), new BigDecimal("10"), null),
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
            metrics.quality().operatingMargin() == null &&
            metrics.quality().freeCashFlowMargin() == null
        ));
    }

    @Test
    void shouldRecalculateCurrentFiscalYearWhenMarketDataIsStale() {
        when(marketDataPort.fetchIncomeStatements(TICKER, 4)).thenReturn(incomeStatements4);
        when(metricsCache.findByTickerAndFiscalYear(TICKER, FY_END))
                .thenReturn(Optional.of(metricsAsOf(NOW.minus(Duration.ofHours(25)))));
        when(marketDataPort.fetchCashFlowStatements(TICKER, 2)).thenReturn(cashFlowStatements2);
        when(marketDataPort.fetchKeyMetricsTtm(TICKER)).thenReturn(Optional.of(keyMetrics));
        when(marketDataPort.fetchAnalystEstimates(TICKER)).thenReturn(List.of());
        when(metricsCache.findAllByTicker(TICKER)).thenReturn(List.of());

        financialAnalysisService.computeMetrics(TICKER);

        verify(metricsCache).save(eq(TICKER), argThat(metrics -> NOW.equals(metrics.marketDataAsOf())));
    }

    @Test
    @DisplayName("La source de données indisponible doit remonter en MarketDataUnavailableException (503), pas en RuntimeException (500)")
    void shouldPropagateMarketDataUnavailableFromIncomeFetch() {
        when(marketDataPort.fetchIncomeStatements(TICKER, 4)).thenThrow(new MarketDataUnavailableException(TICKER));

        assertThrows(MarketDataUnavailableException.class, () -> financialAnalysisService.computeMetrics(TICKER));
        verify(marketDataPort, times(1)).fetchIncomeStatements(TICKER, 4);
    }

    @Test
    @DisplayName("La source indisponible lors du calcul asynchrone doit remonter en MarketDataUnavailableException (503)")
    void shouldPropagateMarketDataUnavailableFromAsyncCashFlowFetch() {
        when(marketDataPort.fetchIncomeStatements(TICKER, 4)).thenReturn(incomeStatements4);
        when(metricsCache.findByTickerAndFiscalYear(TICKER, FY_END)).thenReturn(Optional.empty());
        when(marketDataPort.fetchCashFlowStatements(TICKER, 2)).thenThrow(new MarketDataUnavailableException(TICKER));

        assertThrows(MarketDataUnavailableException.class, () -> financialAnalysisService.computeMetrics(TICKER));
    }

    @Test
    @DisplayName("Une erreur d'accès aux données remonte sans être enveloppée")
    void shouldPropagateDataAccessExceptionFromMetricsCache() {
        DataAccessResourceFailureException expected =
                new DataAccessResourceFailureException("Database unavailable");
        when(marketDataPort.fetchIncomeStatements(TICKER, 4)).thenReturn(incomeStatements4);
        when(metricsCache.findByTickerAndFiscalYear(TICKER, FY_END)).thenThrow(expected);

        DataAccessResourceFailureException actual = assertThrows(
                DataAccessResourceFailureException.class,
                () -> financialAnalysisService.computeMetrics(TICKER));

        assertSame(expected, actual);
    }

    @Test
    @DisplayName("EV/EBIT utilise l'EBIT des 4 derniers trimestres quand ils sont disponibles")
    void shouldUseTrailingTwelveMonthsEbitFromQuarters() {
        // Trimestres volontairement dans le désordre et avec un 5e, plus ancien, à ignorer.
        List<IncomeStatementRecord> quarters = List.of(
                quarter("2024-03-31", "70"),
                quarter("2024-09-30", "100"),
                quarter("2024-06-30", "90"),
                quarter("2023-12-31", "60"),
                quarter("2023-09-30", "999"));
        stubCalculation(Optional.of(keyMetrics), List.of());
        when(marketDataPort.fetchQuarterlyIncomeStatements(TICKER, 4)).thenReturn(quarters);

        financialAnalysisService.computeMetrics(TICKER);

        // EV = 3200, EBIT TTM = 100 + 90 + 70 + 60 = 320 -> 10.0 (et non 3200 / 200 = 16.0)
        verify(metricsCache).save(eq(TICKER), argThat(metrics ->
                metrics.value().evToEbit().compareTo(new BigDecimal("10.0000")) == 0 &&
                // Les marges restent calculées sur l'exercice annuel : 200 / 1000
                metrics.quality().operatingMargin().compareTo(new BigDecimal("0.2000")) == 0));
    }

    @Test
    @DisplayName("EV/EBIT retombe sur l'EBIT annuel si moins de 4 trimestres sont exploitables")
    void shouldFallBackToAnnualEbitWhenQuartersAreIncomplete() {
        stubCalculation(Optional.of(keyMetrics), List.of());
        when(marketDataPort.fetchQuarterlyIncomeStatements(TICKER, 4)).thenReturn(List.of(
                quarter("2024-09-30", "100"),
                quarter("2024-06-30", "90"),
                quarter("2024-03-31", null),
                quarter("2023-12-31", "60")));

        financialAnalysisService.computeMetrics(TICKER);

        verify(metricsCache).save(eq(TICKER), argThat(metrics ->
                metrics.value().evToEbit().compareTo(new BigDecimal("16.0000")) == 0));
    }

    @Test
    @DisplayName("EV/EBIT retombe sur l'EBIT annuel si les trimestres sont antérieurs à la clôture")
    void shouldFallBackToAnnualEbitWhenQuartersAreOlderThanFiscalYearEnd() {
        stubCalculation(Optional.of(keyMetrics), List.of());
        when(marketDataPort.fetchQuarterlyIncomeStatements(TICKER, 4)).thenReturn(List.of(
                quarter("2023-09-30", "100"),
                quarter("2023-06-30", "90"),
                quarter("2023-03-31", "70"),
                quarter("2022-12-31", "60")));

        financialAnalysisService.computeMetrics(TICKER);

        verify(metricsCache).save(eq(TICKER), argThat(metrics ->
                metrics.value().evToEbit().compareTo(new BigDecimal("16.0000")) == 0));
    }

    @Test
    @DisplayName("Une panne de l'endpoint trimestriel ne fait pas échouer le calcul")
    void shouldFallBackToAnnualEbitWhenQuarterlyFetchFails() {
        stubCalculation(Optional.of(keyMetrics), List.of());
        when(marketDataPort.fetchQuarterlyIncomeStatements(TICKER, 4))
                .thenThrow(new MarketDataUnavailableException(TICKER));

        financialAnalysisService.computeMetrics(TICKER);

        verify(metricsCache).save(eq(TICKER), argThat(metrics ->
                metrics.value().evToEbit().compareTo(new BigDecimal("16.0000")) == 0));
    }

    @Test
    @DisplayName("PEG : tolère quelques jours d'écart entre la clôture et la date des estimations")
    void shouldMatchEstimatesToFiscalYearWithDateTolerance() {
        stubCalculation(Optional.of(keyMetrics), List.of(
                new AnalystEstimateRecord(TICKER, "2025-01-03", new BigDecimal("12.00"), null),
                new AnalystEstimateRecord(TICKER, "2024-12-28", new BigDecimal("8.00"), null),
                new AnalystEstimateRecord(TICKER, "2023-12-30", new BigDecimal("4.00"), null)));

        financialAnalysisService.computeMetrics(TICKER);

        // Croissance = 8 / 4 - 1 = 100 % ; PE = 28.4 -> PEG = 0.284
        verify(metricsCache).save(eq(TICKER), argThat(metrics ->
                metrics.value().pegRatioForward().compareTo(new BigDecimal("0.2840")) == 0));
    }

    @Test
    @DisplayName("PEG est null si l'estimation de l'exercice clos ou du suivant manque")
    void shouldReturnNullPegWhenAnEstimateIsMissing() {
        stubCalculation(Optional.of(keyMetrics), List.of(
                new AnalystEstimateRecord(TICKER, "2026-12-31", new BigDecimal("20.00"), null),
                new AnalystEstimateRecord(TICKER, "2025-12-31", new BigDecimal("15.00"), null)));

        financialAnalysisService.computeMetrics(TICKER);

        verify(metricsCache).save(eq(TICKER), argThat(metrics -> metrics.value().pegRatioForward() == null));
    }

    private void stubCalculation(Optional<KeyMetricsRecord> keyMetricsRecord, List<AnalystEstimateRecord> estimates) {
        when(marketDataPort.fetchIncomeStatements(TICKER, 4)).thenReturn(incomeStatements4);
        when(metricsCache.findByTickerAndFiscalYear(TICKER, FY_END)).thenReturn(Optional.empty());
        when(marketDataPort.fetchCashFlowStatements(TICKER, 2)).thenReturn(cashFlowStatements2);
        when(marketDataPort.fetchKeyMetricsTtm(TICKER)).thenReturn(keyMetricsRecord);
        when(marketDataPort.fetchAnalystEstimates(TICKER)).thenReturn(estimates);
    }

    private static IncomeStatementRecord quarter(String date, String operatingIncome) {
        return new IncomeStatementRecord(TICKER, date, new BigDecimal("250"),
                operatingIncome == null ? null : new BigDecimal(operatingIncome),
                null, null, null);
    }

    private FullMetrics metricsAsOf(Instant marketDataAsOf) {
        return new FullMetrics(
                new GrowthMetrics(null, null, null, null),
                new ValueMetrics(null, null, null, null),
                new QualityMetrics(null, null, null, null, null),
                FY_END,
                marketDataAsOf);
    }
}
