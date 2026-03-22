package com.cairedine.finance.app.webclient;

import java.util.List;

public interface IMarketDataPort {

        CompanyProfileRecord        fetchCompanyProfile(String symbol);
        CashFlowRecord              fetchCashFlowTtm(String symbol);
        KeyMetricsRecord            fetchKeyMetricsTtm(String symbol);
        List<AnalystEstimateRecord> fetchAnalystEstimates(String symbol);
        List<IncomeStatementRecord> fetchIncomeStatements(String symbol, int limit);

}
