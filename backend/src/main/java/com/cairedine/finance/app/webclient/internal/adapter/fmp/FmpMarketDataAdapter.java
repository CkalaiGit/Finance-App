package com.cairedine.finance.app.webclient.internal.adapter.fmp;

import com.cairedine.finance.app.webclient.IMarketDataPort;
import com.cairedine.finance.app.webclient.IncomeStatementRecord;
import com.cairedine.finance.app.webclient.internal.dto.FmpCompanyProfileDto;
import com.cairedine.finance.app.webclient.internal.dto.FmpIncomeStatementTtmDto;
import com.cairedine.finance.app.webclient.internal.mapper.CompanyProfileMapper;
import com.cairedine.finance.app.webclient.CompanyProfileRecord;
import com.cairedine.finance.app.webclient.internal.mapper.IncomeStatementMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
class FmpMarketDataAdapter implements IMarketDataPort {

    private final RestClient fmpRestClient;
    private final CompanyProfileMapper companyProfileMapper;
    private final IncomeStatementMapper incomeStatementMapper;

    @Override
    public CompanyProfileRecord fetchCompanyProfile(String symbol) {
        FmpCompanyProfileDto[] dtos = fmpRestClient.get()
                .uri("/stable/profile?symbol={symbol}", symbol)
                .retrieve()
                .body(FmpCompanyProfileDto[].class);
        assert dtos != null;
        return companyProfileMapper.toRecord(dtos[0]);
    }

    @Override
    public IncomeStatementRecord fetchIncomeStatement(String symbol) {
        FmpIncomeStatementTtmDto[] dtos = fmpRestClient.get()
                .uri("/stable/income-statement?symbol={symbol}", symbol)
                .retrieve()
                .body(FmpIncomeStatementTtmDto[].class);
        assert dtos != null;
        return incomeStatementMapper.toRecord(dtos[0]);
    }

    @Override
    public List<IncomeStatementRecord> fetchIncomeStatements(String symbol, int limit) {
        FmpIncomeStatementTtmDto[] dtos = fmpRestClient.get()
                .uri("/stable/income-statement?symbol={symbol}&period=annual&limit={limit}", symbol, limit)
                .retrieve()
                .body(FmpIncomeStatementTtmDto[].class);
        assert dtos != null;
        return Arrays.stream(dtos)
                .map(incomeStatementMapper::toRecord)
                .toList();
    }
}

