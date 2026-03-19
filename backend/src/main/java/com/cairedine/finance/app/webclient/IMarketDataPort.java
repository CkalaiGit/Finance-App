package com.cairedine.finance.app.webclient;

public interface IMarketDataPort {

    CompanyProfileRecord fetchCompanyProfile(String symbol);

    IncomeStatementRecord fetchIncomeStatement(String symbol);
}
