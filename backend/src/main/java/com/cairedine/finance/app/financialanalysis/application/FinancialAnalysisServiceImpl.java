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
    public FullMetrics computeMetrics(String ticker) {
        String normalizedTicker = ticker.toUpperCase();

        return metricsCache.findByTicker(normalizedTicker)
                .orElseGet(() -> {
                    FullMetrics calculated = calculateMetricsFromApi(normalizedTicker);
                    metricsCache.save(normalizedTicker, calculated);
                    return calculated;
                });
    }

    private FullMetrics calculateMetricsFromApi(String ticker) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var incomeFuture = CompletableFuture.supplyAsync(
                    () -> marketDataPort.fetchIncomeStatements(ticker, 4), executor);

            var cashFlowFuture = CompletableFuture.supplyAsync(
                    () -> marketDataPort.fetchCashFlowStatements(ticker, 2), executor);

            var keyMetricsFuture = CompletableFuture.supplyAsync(
                    () -> marketDataPort.fetchKeyMetricsTtm(ticker), executor);

            var analystEstimatesFuture = CompletableFuture.supplyAsync(
                    () -> marketDataPort.fetchAnalystEstimates(ticker), executor);

            List<IncomeStatementRecord> incomeStatements = incomeFuture.join();
            List<CashFlowRecord> cashFlowStatements = cashFlowFuture.join();
            Optional<KeyMetricsRecord> keyMetricsOpt = keyMetricsFuture.join();
            List<AnalystEstimateRecord> analystEstimates = analystEstimatesFuture.join();

            if (incomeStatements.size() < 4 || cashFlowStatements.size() < 2) {
                throw new TickerNotFoundException(ticker + " (Insufficient data)");
            }

            IncomeStatementRecord ttmIncome = incomeStatements.getFirst();
            CashFlowRecord ttmCashFlow = cashFlowStatements.getFirst();

            // Growth
            BigDecimal revenueGrowth3Y = calculateCAGR(ttmIncome.revenue(), incomeStatements.get(3).revenue());
            BigDecimal ebitdaGrowth = calculateGrowth(ttmIncome.ebitda(), incomeStatements.get(1).ebitda());
            BigDecimal epsGrowth = calculateGrowth(ttmIncome.eps(), incomeStatements.get(1).eps());
            BigDecimal fcfGrowth = calculateGrowth(ttmCashFlow.freeCashFlow(), cashFlowStatements.get(1).freeCashFlow());
            GrowthMetrics growth = new GrowthMetrics(revenueGrowth3Y, ebitdaGrowth, epsGrowth, fcfGrowth);

            // Value & Quality base from KeyMetrics
            BigDecimal roic = BigDecimal.ZERO;
            BigDecimal netDebtToEbitda = BigDecimal.ZERO;
            BigDecimal evToEbit = BigDecimal.ZERO;
            BigDecimal peRatioTTM = BigDecimal.ZERO;
            BigDecimal evToSales = BigDecimal.ZERO;

            if (keyMetricsOpt.isPresent()) {
                KeyMetricsRecord km = keyMetricsOpt.get();
                roic = safeValue(km.returnOnInvestedCapitalTTM());
                netDebtToEbitda = safeValue(km.netDebtToEbitda());
                evToEbit = safeValue(km.evToEbit());
                peRatioTTM = safeValue(km.peRatioTTM());
                evToSales = safeValue(km.evToSalesTTM());
            }

            // Value
            BigDecimal pegRatioForward = calculatePegRatio(peRatioTTM, epsGrowth);
            ValueMetrics value = new ValueMetrics(evToEbit, peRatioTTM, pegRatioForward, evToSales);

            // Quality
            BigDecimal operatingMargin = calculateMargin(ttmIncome.operatingIncome(), ttmIncome.revenue());
            BigDecimal freeCashFlowMargin = calculateMargin(ttmCashFlow.freeCashFlow(), ttmIncome.revenue());
            BigDecimal sgaToRevenue = calculateMargin(ttmIncome.sellingGeneralAndAdministrativeExpenses(), ttmIncome.revenue());
            QualityMetrics quality = new QualityMetrics(roic, operatingMargin, netDebtToEbitda, freeCashFlowMargin, sgaToRevenue);

            return new FullMetrics(growth, value, quality);

        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof TickerNotFoundException tnf) throw tnf;
            throw new RuntimeException("Error computing metrics for " + ticker, e);
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

    private BigDecimal calculateMargin(BigDecimal numerator, BigDecimal denominator) {
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
