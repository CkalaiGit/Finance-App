package com.cairedine.finance.app.webclient;

import java.util.List;

public interface IMarketDataPort {

    CompanyProfileRecord fetchCompanyProfile(String symbol);

    IncomeStatementRecord fetchIncomeStatement(String symbol);

    List<IncomeStatementRecord> fetchIncomeStatements(String symbol, int limit);
}
