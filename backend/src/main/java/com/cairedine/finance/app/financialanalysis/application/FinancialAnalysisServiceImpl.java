package com.cairedine.finance.app.financialanalysis.application;

import com.cairedine.finance.app.financialanalysis.domain.FinancialMath;
import com.cairedine.finance.app.financialanalysis.domain.model.FullMetrics;
import com.cairedine.finance.app.financialanalysis.domain.model.GrowthMetrics;
import com.cairedine.finance.app.financialanalysis.domain.model.QualityMetrics;
import com.cairedine.finance.app.financialanalysis.domain.model.ValueMetrics;
import com.cairedine.finance.app.financialanalysis.domain.port.IFinancialAnalysisService;
import com.cairedine.finance.app.financialanalysis.domain.port.IMetricsCachePort;
import com.cairedine.finance.app.shared.exceptions.TickerNotFoundException;
import com.cairedine.finance.app.webclient.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static com.cairedine.finance.app.financialanalysis.domain.FinancialMath.calculateCAGR;
import static com.cairedine.finance.app.financialanalysis.domain.FinancialMath.calculateGrowth;

@Slf4j
@Service
public class FinancialAnalysisServiceImpl implements IFinancialAnalysisService {

    private static final int TTM_QUARTERS = 4;
    /** Tolérance entre la date de clôture publiée et celle des estimations (exercices de 52/53 semaines). */
    private static final int FISCAL_YEAR_DATE_TOLERANCE_DAYS = 45;

    private final IMarketDataPort marketDataPort;
    private final IMetricsCachePort metricsCache;
    private final Clock clock;
    private final Duration marketDataTtl;

    public FinancialAnalysisServiceImpl(
            IMarketDataPort marketDataPort,
            IMetricsCachePort metricsCache,
            Clock clock,
            @Value("${financial-analysis.market-data-ttl:24h}") Duration marketDataTtl) {
        this.marketDataPort = marketDataPort;
        this.metricsCache = metricsCache;
        this.clock = clock;
        this.marketDataTtl = marketDataTtl;
    }

    /**
     * Caffeine protège le quota FMP pendant une heure ; la table JPA conserve l'historique
     * persistant des métriques par exercice.
     */
    @Override
    @Cacheable(value = "analysis-metrics", key = "#ticker.toUpperCase()")
    public List<FullMetrics> computeMetrics(String ticker) {
        String normalizedTicker = ticker.toUpperCase();
        List<IncomeStatementRecord> incomeStatements =
                marketDataPort.fetchIncomeStatements(normalizedTicker, 4);

        if (incomeStatements.isEmpty()) {
            throw new TickerNotFoundException(normalizedTicker);
        }

        String currentFiscalYearEndDate = incomeStatements.getFirst().date();

        Optional<FullMetrics> currentMetrics =
                metricsCache.findByTickerAndFiscalYear(normalizedTicker, currentFiscalYearEndDate);
        Instant staleBefore = clock.instant().minus(marketDataTtl);
        boolean shouldRecalculate = currentMetrics.isEmpty()
                || currentMetrics.get().marketDataAsOf() == null
                || currentMetrics.get().marketDataAsOf().isBefore(staleBefore);

        if (shouldRecalculate) {
            FullMetrics calculated = calculateMetricsFromApi(normalizedTicker, incomeStatements);
            try {
                metricsCache.save(normalizedTicker, calculated);
            } catch (DataIntegrityViolationException e) {
                log.info("Les métriques pour {} à la date {} ont déjà été calculées et sauvées par une autre requête concurrentielle.",
                        normalizedTicker, currentFiscalYearEndDate);
            }
        }

        return metricsCache.findAllByTicker(normalizedTicker);
    }

    private FullMetrics calculateMetricsFromApi(String ticker, List<IncomeStatementRecord> preFetchedIncome) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            var cashFlowFuture = CompletableFuture.supplyAsync(
                    () -> marketDataPort.fetchCashFlowStatements(ticker, 2), executor);

            var keyMetricsFuture = CompletableFuture.supplyAsync(
                    () -> marketDataPort.fetchKeyMetricsTtm(ticker), executor);

            var analystEstimatesFuture = CompletableFuture.supplyAsync(
                    () -> marketDataPort.fetchAnalystEstimates(ticker), executor);

            var quarterlyIncomeFuture = CompletableFuture.supplyAsync(
                    () -> fetchQuarterlyIncomeSafely(ticker), executor);

            List<CashFlowRecord> cashFlowStatements = join(cashFlowFuture);
            Optional<KeyMetricsRecord> keyMetricsOpt = join(keyMetricsFuture);
            List<AnalystEstimateRecord> analystEstimates = join(analystEstimatesFuture);
            List<IncomeStatementRecord> quarterlyIncome = join(quarterlyIncomeFuture);

            if (preFetchedIncome.size() < 4 || cashFlowStatements.size() < 2) {
                throw new TickerNotFoundException(ticker + " (Insufficient data)");
            }

            IncomeStatementRecord ttmIncome = preFetchedIncome.getFirst();
            CashFlowRecord ttmCashFlow = cashFlowStatements.getFirst();
            String fiscalYearEndDate = ttmIncome.date();
            Instant marketDataAsOf = clock.instant();

            // Growth
            BigDecimal revenueGrowth3Y = calculateCAGR(ttmIncome.revenue(), preFetchedIncome.get(3).revenue());
            BigDecimal ebitdaGrowth3Y = calculateCAGR(ttmIncome.ebitda(), preFetchedIncome.get(3).ebitda());
            BigDecimal epsGrowth3Y = calculateCAGR(ttmIncome.eps(), preFetchedIncome.get(3).eps());
            BigDecimal fcfGrowth = calculateGrowth(ttmCashFlow.freeCashFlow(), cashFlowStatements.get(1).freeCashFlow());
            GrowthMetrics growth = new GrowthMetrics(revenueGrowth3Y, ebitdaGrowth3Y, epsGrowth3Y, fcfGrowth);

            // Value & Quality base from KeyMetrics
            BigDecimal roic = null;
            BigDecimal netDebtToEbitda = null;
            BigDecimal enterpriseValue = null;
            BigDecimal peRatioTTM = null;
            BigDecimal evToSales = null;

            if (keyMetricsOpt.isPresent()) {
                KeyMetricsRecord km = keyMetricsOpt.get();
                roic = FinancialMath.safeValue(km.returnOnInvestedCapitalTTM());
                netDebtToEbitda = FinancialMath.safeValue(km.netDebtToEbitda());
                enterpriseValue = FinancialMath.safeValue(km.enterpriseValueTTM());
                peRatioTTM = FinancialMath.safeValue(km.peRatioTTM());
                evToSales = FinancialMath.safeValue(km.evToSalesTTM());
            }

            // Value
            // EV (valeur de marché du jour) / EBIT des 12 derniers mois (somme des 4 derniers
            // trimestres). Diviser par l'EBIT du dernier exercice annuel surévalue le ratio
            // des entreprises en forte croissance dès que la clôture date de quelques mois.
            BigDecimal ebitTtm = trailingTwelveMonthsEbit(ticker, ttmIncome, quarterlyIncome);
            BigDecimal evToEbit = FinancialMath.calculateRatio(enterpriseValue, ebitTtm);
            BigDecimal epsGrowthForward = forwardEpsGrowth(ticker, fiscalYearEndDate, analystEstimates);
            BigDecimal pegRatioForward = FinancialMath.calculatePegRatio(peRatioTTM, epsGrowthForward);
            ValueMetrics value = new ValueMetrics(evToEbit, peRatioTTM, pegRatioForward, evToSales);

            // Quality
            BigDecimal operatingMargin = FinancialMath.calculateRatio(ttmIncome.operatingIncome(), ttmIncome.revenue());
            BigDecimal freeCashFlowMargin = FinancialMath.calculateRatio(ttmCashFlow.freeCashFlow(), ttmIncome.revenue());
            BigDecimal sgaToRevenue = FinancialMath.calculateRatio(ttmIncome.sellingGeneralAndAdministrativeExpenses(), ttmIncome.revenue());
            QualityMetrics quality = new QualityMetrics(roic, operatingMargin, netDebtToEbitda, freeCashFlowMargin, sgaToRevenue);

            return new FullMetrics(growth, value, quality, fiscalYearEndDate, marketDataAsOf);
        }
    }

    /**
     * Les trimestres ne servent qu'à affiner l'EV/EBIT : s'ils sont indisponibles, on retombe
     * sur l'EBIT annuel plutôt que de faire échouer tout le calcul.
     */
    private List<IncomeStatementRecord> fetchQuarterlyIncomeSafely(String ticker) {
        try {
            List<IncomeStatementRecord> quarters =
                    marketDataPort.fetchQuarterlyIncomeStatements(ticker, TTM_QUARTERS);
            return quarters == null ? List.of() : quarters;
        } catch (RuntimeException exception) {
            log.warn("Comptes trimestriels indisponibles pour {} ; EV/EBIT calculé sur l'exercice annuel.",
                    ticker, exception);
            return List.of();
        }
    }

    /**
     * EBIT des 12 derniers mois = somme des 4 derniers trimestres publiés.
     * Repli sur l'EBIT du dernier exercice annuel si les trimestres sont incomplets
     * ou plus anciens que cette clôture.
     */
    private BigDecimal trailingTwelveMonthsEbit(
            String ticker, IncomeStatementRecord lastFiscalYear, List<IncomeStatementRecord> quarterlyIncome) {
        BigDecimal annualEbit = lastFiscalYear.operatingIncome();
        List<IncomeStatementRecord> lastQuarters = quarterlyIncome.stream()
                .filter(quarter -> quarter != null && parseDate(quarter.date()) != null)
                .sorted(Comparator.comparing((IncomeStatementRecord quarter) -> parseDate(quarter.date())).reversed())
                .limit(TTM_QUARTERS)
                .toList();
        if (lastQuarters.size() < TTM_QUARTERS
                || lastQuarters.stream().anyMatch(quarter -> quarter.operatingIncome() == null)) {
            return annualEbit;
        }
        LocalDate fiscalYearEnd = parseDate(lastFiscalYear.date());
        LocalDate latestQuarterEnd = parseDate(lastQuarters.getFirst().date());
        if (fiscalYearEnd != null && latestQuarterEnd.isBefore(fiscalYearEnd)) {
            log.warn("Trimestres de {} antérieurs à la clôture annuelle {} ; EV/EBIT calculé sur l'exercice annuel.",
                    ticker, lastFiscalYear.date());
            return annualEbit;
        }
        return lastQuarters.stream()
                .map(IncomeStatementRecord::operatingIncome)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Croissance attendue du BPA entre le dernier exercice clos et le suivant, d'après le
     * consensus des analystes (les deux valeurs viennent du consensus, pour rester homogènes).
     * Les estimations sont choisies par date et non par position : FMP les renvoie de la plus
     * lointaine à la plus ancienne.
     */
    private BigDecimal forwardEpsGrowth(
            String ticker, String fiscalYearEndDate, List<AnalystEstimateRecord> analystEstimates) {
        if (analystEstimates == null || analystEstimates.isEmpty()) {
            return null;
        }
        LocalDate fiscalYearEnd = parseDate(fiscalYearEndDate);
        if (fiscalYearEnd == null) {
            log.warn("Date de clôture non analysable pour {} : '{}' ; PEG non calculé.", ticker, fiscalYearEndDate);
            return null;
        }
        LocalDate closedYearStart = fiscalYearEnd.minusDays(FISCAL_YEAR_DATE_TOLERANCE_DAYS);
        LocalDate closedYearLimit = fiscalYearEnd.plusDays(FISCAL_YEAR_DATE_TOLERANCE_DAYS);

        AnalystEstimateRecord closedYear = null;
        AnalystEstimateRecord nextYear = null;
        LocalDate nextYearDate = null;
        for (AnalystEstimateRecord estimate : analystEstimates) {
            LocalDate estimateDate = estimate == null ? null : parseDate(estimate.date());
            if (estimateDate == null) {
                continue;
            }
            if (estimateDate.isAfter(closedYearLimit)) {
                if (nextYearDate == null || estimateDate.isBefore(nextYearDate)) {
                    nextYear = estimate;
                    nextYearDate = estimateDate;
                }
            } else if (!estimateDate.isBefore(closedYearStart)) {
                closedYear = estimate;
            }
        }
        if (closedYear == null || nextYear == null) {
            return null;
        }
        return calculateGrowth(nextYear.epsAvg(), closedYear.epsAvg());
    }

    private static LocalDate parseDate(String date) {
        if (date == null) {
            return null;
        }
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private static <T> T join(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException exception) {
            throw unwrap(exception);
        }
    }

    private static RuntimeException unwrap(Throwable exception) {
        if (exception instanceof CompletionException || exception instanceof ExecutionException) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                return runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            return exception instanceof RuntimeException runtimeException
                    ? runtimeException
                    : new CompletionException(exception);
        }
        if (exception instanceof RuntimeException runtimeException) return runtimeException;
        if (exception instanceof Error error) throw error;
        return new CompletionException(exception);
    }

}
