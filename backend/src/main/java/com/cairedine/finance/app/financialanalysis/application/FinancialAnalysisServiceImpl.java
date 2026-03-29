package com.cairedine.finance.app.financialanalysis.application;

import com.cairedine.finance.app.financialanalysis.domain.model.GrowthMetrics;
import com.cairedine.finance.app.financialanalysis.domain.port.IFinancialAnalysisService;
import com.cairedine.finance.app.shared.exceptions.TickerNotFoundException;
import com.cairedine.finance.app.webclient.CashFlowRecord;
import com.cairedine.finance.app.webclient.IMarketDataPort;
import com.cairedine.finance.app.webclient.IncomeStatementRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class FinancialAnalysisServiceImpl implements IFinancialAnalysisService {

    private final IMarketDataPort marketDataPort;
    private static final MathContext MC = new MathContext(8, RoundingMode.HALF_UP);

    @Override
    public GrowthMetrics computeMetrics(String ticker) {
        // On utilise un Executor de Virtual Threads (Stable en Java 21)
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            // Lancement des appels en parallèle
            var incomeFuture = CompletableFuture.supplyAsync(
                    () -> marketDataPort.fetchIncomeStatements(ticker, 4), executor);

            var cashFlowFuture = CompletableFuture.supplyAsync(
                    () -> marketDataPort.fetchCashFlowStatements(ticker, 2), executor);

            // Attente des résultats
            List<IncomeStatementRecord> incomeStatements = incomeFuture.join();
            List<CashFlowRecord> cashFlowStatements = cashFlowFuture.join();

            // Validation des données
            if (incomeStatements.size() < 4 || cashFlowStatements.size() < 2) {
                throw new TickerNotFoundException(ticker + " (Données insuffisantes)");
            }

            // Calculs
            BigDecimal revenueGrowth3Y = calculateCAGR(incomeStatements.get(0).revenue(), incomeStatements.get(3).revenue());
            BigDecimal ebitdaGrowth = calculateGrowth(incomeStatements.get(0).ebitda(), incomeStatements.get(1).ebitda());
            BigDecimal epsGrowth = calculateGrowth(incomeStatements.get(0).eps(), incomeStatements.get(1).eps());
            BigDecimal fcfGrowth = calculateGrowth(cashFlowStatements.get(0).freeCashFlow(), cashFlowStatements.get(1).freeCashFlow());

            return new GrowthMetrics(revenueGrowth3Y, ebitdaGrowth, epsGrowth, fcfGrowth);

        } catch (Exception e) {
            if (e.getCause() instanceof TickerNotFoundException) throw (TickerNotFoundException) e.getCause();
            throw new RuntimeException("Erreur lors du calcul des métriques pour " + ticker, e);
        }
    }

    private BigDecimal calculateCAGR(BigDecimal endValue, BigDecimal startValue) {
        if (startValue == null || endValue == null || startValue.signum() <= 0 || endValue.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        double ratio = endValue.divide(startValue, MC).doubleValue();
        double cagr = Math.pow(ratio, 1.0 / (double) 3) - 1;
        return BigDecimal.valueOf(cagr).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateGrowth(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null || previous.signum() == 0) {
            return BigDecimal.ZERO;
        }
        // Formule: (N - N-1) / |N-1|
        return current.subtract(previous)
                .divide(previous.abs(), MC)
                .setScale(4, RoundingMode.HALF_UP);
    }
}