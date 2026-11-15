package com.cairedine.finance.app.webclient;

import java.util.List;
import java.util.Optional;

public interface IMarketDataPort {

        Optional<CompanyProfileRecord> fetchCompanyProfile(String symbol);
        Optional<CashFlowRecord>       fetchCashFlowTtm(String symbol);
        Optional<KeyMetricsRecord>     fetchKeyMetricsTtm(String symbol);
        List<AnalystEstimateRecord>    fetchAnalystEstimates(String symbol);
        List<IncomeStatementRecord>    fetchIncomeStatements(String symbol, int limit);
        /** Comptes de résultat trimestriels, du plus récent au plus ancien. */
        List<IncomeStatementRecord>    fetchQuarterlyIncomeStatements(String symbol, int limit);
        List<CashFlowRecord>           fetchCashFlowStatements(String symbol, int limit);
}
