package com.cairedine.finance.app.webclient;

import com.cairedine.finance.app.webclient.record.CompanyProfileRecord;

public interface IMarketDataPort {
    CompanyProfileRecord fetchCompanyProfile(String symbol);
}
