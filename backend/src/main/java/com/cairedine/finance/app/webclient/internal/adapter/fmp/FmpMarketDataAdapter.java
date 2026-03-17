package com.cairedine.finance.app.webclient.internal.adapter.fmp;

import com.cairedine.finance.app.webclient.IMarketDataPort;
import com.cairedine.finance.app.webclient.internal.dto.FmpCompanyProfileDto;
import com.cairedine.finance.app.webclient.internal.mapper.CompanyProfileMapper;
import com.cairedine.finance.app.webclient.record.CompanyProfileRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
class FmpMarketDataAdapter implements IMarketDataPort {

    private final RestClient fmpRestClient;
    private final CompanyProfileMapper companyProfileMapper;

    @Override
    public CompanyProfileRecord fetchCompanyProfile(String symbol) {
        FmpCompanyProfileDto[] dtos = fmpRestClient.get()
                .uri("/stable/profile?symbol={symbol}", symbol)
                .retrieve()
                .body(FmpCompanyProfileDto[].class);
        return companyProfileMapper.toRecord(dtos[0]);
    }
}
