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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
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
@RequiredArgsConstructor
public class FinancialAnalysisServiceImpl implements IFinancialAnalysisService {

    private final IMarketDataPort marketDataPort;
    private final IMetricsCachePort metricsCache;

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

        if (metricsCache.findByTickerAndFiscalYear(normalizedTicker, currentFiscalYearEndDate).isEmpty()) {
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

            List<CashFlowRecord> cashFlowStatements = join(cashFlowFuture);
            Optional<KeyMetricsRecord> keyMetricsOpt = join(keyMetricsFuture);
            List<AnalystEstimateRecord> analystEstimates = join(analystEstimatesFuture);

            if (preFetchedIncome.size() < 4 || cashFlowStatements.size() < 2) {
                throw new TickerNotFoundException(ticker + " (Insufficient data)");
            }

            IncomeStatementRecord ttmIncome = preFetchedIncome.getFirst();
            CashFlowRecord ttmCashFlow = cashFlowStatements.getFirst();
            String fiscalYearEndDate = ttmIncome.date();
            Instant marketDataAsOf = Instant.now();

            // Growth
            BigDecimal revenueGrowth3Y = calculateCAGR(ttmIncome.revenue(), preFetchedIncome.get(3).revenue());
            BigDecimal ebitdaGrowth3Y = calculateCAGR(ttmIncome.ebitda(), preFetchedIncome.get(3).ebitda());
            BigDecimal epsGrowth3Y = calculateCAGR(ttmIncome.eps(), preFetchedIncome.get(3).eps());
            BigDecimal fcfGrowth = calculateGrowth(ttmCashFlow.freeCashFlow(), cashFlowStatements.get(1).freeCashFlow());
            GrowthMetrics growth = new GrowthMetrics(revenueGrowth3Y, ebitdaGrowth3Y, epsGrowth3Y, fcfGrowth);

            // Value & Quality base from KeyMetrics
            BigDecimal roic = BigDecimal.ZERO;
            BigDecimal netDebtToEbitda = BigDecimal.ZERO;
            BigDecimal enterpriseValue = BigDecimal.ZERO;
            BigDecimal peRatioTTM = BigDecimal.ZERO;
            BigDecimal evToSales = BigDecimal.ZERO;

            if (keyMetricsOpt.isPresent()) {
                KeyMetricsRecord km = keyMetricsOpt.get();
                roic = FinancialMath.safeValue(km.returnOnInvestedCapitalTTM());
                netDebtToEbitda = FinancialMath.safeValue(km.netDebtToEbitda());
                enterpriseValue = FinancialMath.safeValue(km.enterpriseValueTTM());
                peRatioTTM = FinancialMath.safeValue(km.peRatioTTM());
                evToSales = FinancialMath.safeValue(km.evToSalesTTM());
            }

            // Value
            // EV (instantanée, TTM boursier) / EBIT (comptable, dernière clôture)
            // Ratio cohérent en convention marché — EV et EBIT doivent couvrir 12 mois TTM
            // KeyMetrics FMP retourne un EV TTM aligné sur les 12 derniers mois.
            BigDecimal evToEbit = FinancialMath.calculateRatio(enterpriseValue, ttmIncome.operatingIncome());
            BigDecimal epsGrowthForward = BigDecimal.ZERO;
            if (analystEstimates.size() >= 2) {
                BigDecimal epsNext = analystEstimates.get(0).epsAvg();
                BigDecimal epsCurrent = analystEstimates.get(1).epsAvg();
                epsGrowthForward = calculateGrowth(epsNext, epsCurrent);
            }
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
