package com.cairedine.finance.app.financialanalysis.application;

import com.cairedine.finance.app.financialanalysis.domain.model.FullMetrics;
import com.cairedine.finance.app.financialanalysis.domain.model.GrowthMetrics;
import com.cairedine.finance.app.financialanalysis.domain.model.QualityMetrics;
import com.cairedine.finance.app.financialanalysis.domain.model.ValueMetrics;
import com.cairedine.finance.app.financialanalysis.domain.port.IFinancialAnalysisService;
import com.cairedine.finance.app.financialanalysis.domain.port.IMetricsCachePort;
import com.cairedine.finance.app.shared.exceptions.TickerNotFoundException;
import com.cairedine.finance.app.webclient.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class FinancialAnalysisServiceImpl implements IFinancialAnalysisService {

    private final IMarketDataPort marketDataPort;
    private final IMetricsCachePort metricsCache;

    private static final MathContext MC = new MathContext(8, RoundingMode.HALF_UP);

    @Override
    @Transactional
    public List<FullMetrics> computeMetrics(String ticker) {
        String normalizedTicker = ticker.toUpperCase();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var incomeFuture = CompletableFuture.supplyAsync(
                    () -> marketDataPort.fetchIncomeStatements(normalizedTicker, 4), executor);

            List<IncomeStatementRecord> incomeStatements = incomeFuture.join();
            if (incomeStatements.isEmpty()) {
                throw new TickerNotFoundException(normalizedTicker);
            }

            String currentPeriodEndDate = incomeStatements.getFirst().date();

            // 1. Check if the current period is already in history
            if (metricsCache.findByTickerAndPeriod(normalizedTicker, currentPeriodEndDate).isEmpty()) {
                FullMetrics calculated = calculateMetricsFromApi(normalizedTicker, incomeStatements);
                metricsCache.save(normalizedTicker, calculated);
            }

            // 2. Return the full history
            return metricsCache.findAllByTicker(normalizedTicker);

        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof TickerNotFoundException tnf) throw tnf;
            throw new RuntimeException("Error computing historical metrics for " + ticker, cause);
        }
    }

    private FullMetrics calculateMetricsFromApi(String ticker, List<IncomeStatementRecord> preFetchedIncome) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            var cashFlowFuture = CompletableFuture.supplyAsync(
                    () -> marketDataPort.fetchCashFlowStatements(ticker, 2), executor);

            var keyMetricsFuture = CompletableFuture.supplyAsync(
                    () -> marketDataPort.fetchKeyMetricsTtm(ticker), executor);

            var analystEstimatesFuture = CompletableFuture.supplyAsync(
                    () -> marketDataPort.fetchAnalystEstimates(ticker), executor);

            List<CashFlowRecord> cashFlowStatements = cashFlowFuture.join();
            Optional<KeyMetricsRecord> keyMetricsOpt = keyMetricsFuture.join();
            List<AnalystEstimateRecord> analystEstimates = analystEstimatesFuture.join();

            if (preFetchedIncome.size() < 4 || cashFlowStatements.size() < 2) {
                throw new TickerNotFoundException(ticker + " (Insufficient data)");
            }

            IncomeStatementRecord ttmIncome = preFetchedIncome.getFirst();
            CashFlowRecord ttmCashFlow = cashFlowStatements.getFirst();
            String periodEndDate = ttmIncome.date();

            // Growth
            BigDecimal revenueGrowth3Y = calculateCAGR(ttmIncome.revenue(), preFetchedIncome.get(3).revenue());
            BigDecimal ebitdaGrowth = calculateGrowth(ttmIncome.ebitda(), preFetchedIncome.get(1).ebitda());
            BigDecimal epsGrowth = calculateGrowth(ttmIncome.eps(), preFetchedIncome.get(1).eps());
            BigDecimal fcfGrowth = calculateGrowth(ttmCashFlow.freeCashFlow(), cashFlowStatements.get(1).freeCashFlow());
            GrowthMetrics growth = new GrowthMetrics(revenueGrowth3Y, ebitdaGrowth, epsGrowth, fcfGrowth);

            // Value & Quality base from KeyMetrics
            BigDecimal roic = BigDecimal.ZERO;
            BigDecimal netDebtToEbitda = BigDecimal.ZERO;
            BigDecimal enterpriseValue = BigDecimal.ZERO;
            BigDecimal peRatioTTM = BigDecimal.ZERO;
            BigDecimal evToSales = BigDecimal.ZERO;

            if (keyMetricsOpt.isPresent()) {
                KeyMetricsRecord km = keyMetricsOpt.get();
                roic = safeValue(km.returnOnInvestedCapitalTTM());
                netDebtToEbitda = safeValue(km.netDebtToEbitda());
                enterpriseValue = safeValue(km.enterpriseValueTTM());
                peRatioTTM = safeValue(km.peRatioTTM());
                evToSales = safeValue(km.evToSalesTTM());
            }

            // Value
            BigDecimal evToEbit = calculateRatio(enterpriseValue, ttmIncome.operatingIncome());
            BigDecimal epsGrowthForward = BigDecimal.ZERO;
            if (analystEstimates.size() >= 2) {
                BigDecimal epsNext = analystEstimates.get(0).epsAvg();
                BigDecimal epsCurrent = analystEstimates.get(1).epsAvg();
                epsGrowthForward = calculateGrowth(epsNext, epsCurrent);
            }
            BigDecimal pegRatioForward = calculatePegRatio(peRatioTTM, epsGrowthForward);
            ValueMetrics value = new ValueMetrics(evToEbit, peRatioTTM, pegRatioForward, evToSales);

            // Quality
            BigDecimal operatingMargin = calculateRatio(ttmIncome.operatingIncome(), ttmIncome.revenue());
            BigDecimal freeCashFlowMargin = calculateRatio(ttmCashFlow.freeCashFlow(), ttmIncome.revenue());
            BigDecimal sgaToRevenue = calculateRatio(ttmIncome.sellingGeneralAndAdministrativeExpenses(), ttmIncome.revenue());
            QualityMetrics quality = new QualityMetrics(roic, operatingMargin, netDebtToEbitda, freeCashFlowMargin, sgaToRevenue);

            return new FullMetrics(growth, value, quality, periodEndDate);

        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof TickerNotFoundException tnf) throw tnf;
            throw new RuntimeException("Error calculating current metrics for " + ticker, cause);
        }
    }

    private BigDecimal calculateCAGR(BigDecimal endValue, BigDecimal startValue) {
        if (startValue == null || endValue == null || startValue.signum() <= 0 || endValue.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        double ratio = endValue.divide(startValue, MC).doubleValue();
        double cagr = Math.pow(ratio, 1.0 / 3.0) - 1;
        return BigDecimal.valueOf(cagr).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateGrowth(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null || previous.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .divide(previous.abs(), MC)
                .setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateRatio(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, MC).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePegRatio(BigDecimal pe, BigDecimal epsGrowth) {
        if (pe == null || epsGrowth == null || epsGrowth.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal epsGrowthPercent = epsGrowth.multiply(BigDecimal.valueOf(100));
        return pe.divide(epsGrowthPercent, MC).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal safeValue(BigDecimal value) {
        return value != null ? value.setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }
}
